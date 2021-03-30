package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.BuildsScanCache;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.impl.XrayClientBuilder;
import com.jfrog.xray.client.impl.services.details.DetailsResponseImpl;
import com.jfrog.xray.client.services.details.DetailsResponse;
import com.jfrog.xray.client.services.summary.Error;
import org.apache.commons.collections4.CollectionUtils;
import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jfrog.ide.common.utils.Utils.createMapper;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * @author yahavi
 **/
public class XrayBuildDetailsDownloader extends ConsumerRunnableBase {
    private final XrayClientBuilder xrayClientBuilder;
    private ProducerConsumerExecutor executor;
    private final ProgressIndicator indicator;
    private final BuildsScanCache buildsCache;
    private final DependencyTree root;
    private final AtomicInteger count;
    private final double total;
    private Log log;

    public XrayBuildDetailsDownloader(DependencyTree root, BuildsScanCache buildsCache, XrayClientBuilder xrayClientBuilder,
                                      ProgressIndicator indicator, AtomicInteger count, double total, Log log) {
        this.xrayClientBuilder = xrayClientBuilder;
        this.buildsCache = buildsCache;
        this.indicator = indicator;
        this.count = count;
        this.total = total;
        this.root = root;
        this.log = log;
    }

    @Override
    public void consumerRun() {
        ObjectMapper mapper = createMapper();
        try (XrayClient xrayClient = xrayClientBuilder.build()) {
            while (!Thread.interrupted()) {
                ProducerConsumerItem item = executor.take();
                if (item == executor.TERMINATE) {
                    // If reached the TERMINATE, return it to the queue and exit.
                    executor.put(item);
                    break;
                }
                CiDependencyTree buildDependencyTree = (CiDependencyTree) item;
                GeneralInfo generalInfo = buildDependencyTree.getGeneralInfo();
                String buildName = generalInfo.getArtifactId();
                String buildNumber = generalInfo.getVersion();
                try {
                    DetailsResponse response = tryLoadFromCache(mapper, buildName, buildNumber);
                    if (response == null) {
                        response = downloadBuildDetails(mapper, xrayClient, buildName, buildNumber);
                    }
                    if (response == null) {
                        continue;
                    }
                    buildDependencyTree.populateBuildDependencyTree(response);
                    synchronized (root) {
                        root.add(buildDependencyTree);
                    }
                } catch (IOException e) {
                    String msg = String.format("Couldn't scan build '%s/%s'", buildName, buildNumber);
                    log.error(msg, e);
                } finally {
                    indicator.setFraction(count.incrementAndGet() / total);
                }
            }
        } catch (InterruptedException ignored) {
        }
    }

    private DetailsResponse tryLoadFromCache(ObjectMapper mapper, String buildName, String buildNumber) {
        try {
            byte[] buffer = buildsCache.load(buildName, buildNumber, BuildsScanCache.Type.XRAY_BUILD_SCAN);
            if (buffer != null) {
                return mapper.readValue(buffer, DetailsResponseImpl.class);
            }
        } catch (IOException e) {
            String msg = String.format("Failed reading cache file for '%s%s', zapping the old cache and starting a new one.", buildName, buildNumber);
            log.error(msg, e);
        }
        return null;
    }

    private DetailsResponse downloadBuildDetails(ObjectMapper mapper, XrayClient xrayClient, String buildName, String buildNumber) throws IOException {
        DetailsResponse response = xrayClient.details().build(buildName, buildNumber);
        if (!response.getScanCompleted() || isNotEmpty(response.getErrors()) || isEmpty(response.getComponents())) {
            if (CollectionUtils.isNotEmpty(response.getErrors())) {
                printError(response);
            }
            return null;
        }
        byte[] buffer = mapper.writeValueAsBytes(response);
        buildsCache.save(buffer, buildName, buildNumber, BuildsScanCache.Type.XRAY_BUILD_SCAN);
        return response;
    }

    private void printError(DetailsResponse response) {
        response.getErrors().stream()
                .map(err -> (Error) err)
                .forEach(err -> log.error(err.getError() + "/n" + err.getIdentifier()));
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
