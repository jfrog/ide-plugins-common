package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.BuildsScanCache;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.impl.XrayClientBuilder;
import com.jfrog.xray.client.services.details.DetailsResponse;
import com.jfrog.xray.client.services.details.Error;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jfrog.ide.common.ci.Utils.BUILD_RET_ERR_FMT;
import static com.jfrog.ide.common.utils.Constants.MINIMAL_XRAY_VERSION_SUPPORTED_FOR_CI;
import static com.jfrog.ide.common.utils.Utils.createMapper;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * Consumes {@link BuildDependencyTree} that produced by {@link BuildArtifactsDownloader}.
 * The consumer downloads Xray build scan results and save them in the local cache.
 *
 * @author yahavi
 **/
public class XrayBuildDetailsDownloader extends ConsumerRunnableBase {
    private final XrayClientBuilder xrayClientBuilder;
    private ProducerConsumerExecutor executor;
    private final ProgressIndicator indicator;
    private final BuildsScanCache buildsCache;
    private final Runnable checkCancel;
    private final DependencyTree root;
    private final AtomicInteger count;
    private final double total;
    private String project;
    private Log log;

    public XrayBuildDetailsDownloader(DependencyTree root, BuildsScanCache buildsCache, XrayClientBuilder xrayClientBuilder,
                                      ProgressIndicator indicator, AtomicInteger count, double total, Log log, Runnable checkCancel,
                                      String project) {
        this.xrayClientBuilder = xrayClientBuilder;
        this.buildsCache = buildsCache;
        this.checkCancel = checkCancel;
        this.indicator = indicator;
        this.project = project;
        this.count = count;
        this.total = total;
        this.root = root;
        this.log = log;
    }

    @Override
    public void consumerRun() {
        ObjectMapper mapper = createMapper();
        try (XrayClient xrayClient = xrayClientBuilder.build()) {
            boolean xraySupported = isXraySupported(xrayClient);
            while (!Thread.interrupted()) {
                ProducerConsumerItem item = executor.take();
                if (item == executor.TERMINATE) {
                    // If reached the TERMINATE, return it to the queue and exit.
                    executor.put(item);
                    break;
                }
                BuildGeneralInfo generalInfo = (BuildGeneralInfo) item;
                String buildName = generalInfo.getArtifactId();
                String buildNumber = generalInfo.getVersion();
                try {
                    checkCancel.run();
                    if (!xraySupported) {
                        continue;
                    }
                    if (buildsCache.loadScanResults(mapper, buildName, buildNumber) == null) {
                        downloadBuildDetails(mapper, xrayClient, buildName, buildNumber);
                    }
                } catch (CancellationException ignored) {
                } catch (IOException e) {
                    log.debug(String.format(BUILD_RET_ERR_FMT, buildName, buildNumber) + ". " + ExceptionUtils.getRootCauseMessage(e));
                } finally {
                    addResults(generalInfo);
                    indicator.setFraction(count.incrementAndGet() / total);
                }
            }
        } catch (InterruptedException ignored) {
        }
    }

    private boolean isXraySupported(XrayClient xrayClient) {
        try {
            return xrayClient.system().version().isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED_FOR_CI);
        } catch (IOException ignored) {
            return false;
        }
    }

    private void downloadBuildDetails(ObjectMapper mapper, XrayClient xrayClient, String buildName, String buildNumber) throws IOException {
        DetailsResponse response = xrayClient.details().build(buildName, buildNumber, project);
        if (!response.getScanCompleted() || response.getError() != null || isEmpty(response.getComponents())) {
            if (response.getError() != null) {
                Error error = response.getError();
                log.debug(String.format(BUILD_RET_ERR_FMT, buildName, buildNumber) + " " +
                        error.getErrorCode() + ": " + error.getMessage());
            }
            return;
        }
        buildsCache.save(mapper.writeValueAsBytes(response), buildName, buildNumber, BuildsScanCache.Type.BUILD_SCAN_RESULTS);
    }

    private void addResults(BuildGeneralInfo generalInfo) {
        BuildDependencyTree dependencyTree = new BuildDependencyTree(generalInfo.getBuildName() + "/" + generalInfo.getBuildNumber());
        dependencyTree.setGeneralInfo(generalInfo);
        synchronized (root) {
            root.add(dependencyTree);
        }
    }

    @Override
    public void setExecutor(ProducerConsumerExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void setLog(Log log) {
        this.log = log;
    }
}
