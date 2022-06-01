package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.BuildsScanCache;
import com.jfrog.xray.client.impl.XrayClientBuilder;
import com.jfrog.xray.client.services.details.DetailsResponse;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Sets.newHashSet;
import static com.jfrog.ide.common.ci.Utils.createAqlForBuildArtifacts;
import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.common.utils.ArtifactoryConnectionUtils.createArtifactoryManagerBuilder;
import static com.jfrog.ide.common.utils.Utils.createMapper;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.jfrog.build.client.PreemptiveHttpClientBuilder.CONNECTION_POOL_SIZE;

/**
 * Managing the CI dependency tree creation process.
 *
 * @author yahavi
 */
@SuppressWarnings("unused")
public class CiManagerBase {
    private static final String DEFAULT_PROJECT = "artifactory";
    protected DependencyTree root = new DependencyTree();
    private final ObjectMapper mapper = createMapper();
    private final ServerConfig serverConfig;
    final BuildsScanCache buildsCache;
    private final Log log;

    public CiManagerBase(Path cachePath, String projectName, Log log, ServerConfig serverConfig) throws IOException {
        this.buildsCache = new BuildsScanCache(projectName, cachePath, log);
        this.serverConfig = serverConfig;
        this.log = log;
    }

    /**
     * Build the CI tree in a producer-consumer method.
     * The producer download build-info artifacts from artifactory, save them in cache and produce the build
     * general info to the consumer.
     * The consumer download build scan results from Xray, save them in cache and populate the CI tree with nodes
     * containing only the build general info of the builds.
     * <p>
     * When the produce-consumer job is done, the CI tree contains only general information on the builds.
     * The build dependencies, artifacts and Xray scan results is stored in cache to save RAM.
     *
     * @param buildsPattern - The build pattern configured in the IDE configuration
     * @param project       - The JFrog project to scan
     * @param indicator     - The progress indicator to show
     * @param checkCanceled - Callback that throws an exception if scan was cancelled by user
     * @param shouldToast   - True if scan was triggered by the "refresh" button
     * @throws NoSuchAlgorithmException in case of error during creating the Artifactory dependencies client.
     * @throws KeyStoreException        in case of error during creating the Artifactory dependencies client.
     * @throws KeyManagementException   in case of error during creating the Artifactory dependencies client.
     */
    public void buildCiTree(String buildsPattern, String project, ProgressIndicator indicator, Runnable checkCanceled, boolean shouldToast) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        root = new DependencyTree();
        XrayClientBuilder xrayClientBuilder = createXrayClientBuilder(serverConfig, log);
        ArtifactoryManagerBuilder artifactoryManagerBuilder = createArtifactoryManagerBuilder(serverConfig, new NullLog());
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            buildsCache.createDirectories();
            String buildInfoRepo = StringUtils.defaultIfBlank(serverConfig.getProject(), DEFAULT_PROJECT) + "-build-info";
            AqlSearchResult searchResult = artifactoryManager.searchArtifactsByAql(createAqlForBuildArtifacts(buildsPattern, buildInfoRepo));
            if (searchResult.getResults().isEmpty()) {
                return;
            }

            Queue<AqlSearchResult.SearchEntry> buildArtifacts = new ArrayBlockingQueue<>(searchResult.getResults().size(), false);
            buildArtifacts.addAll(searchResult.getResults());

            AtomicInteger count = new AtomicInteger();
            double total = buildArtifacts.size() * 2;
            // Create producer Runnables.
            ProducerRunnableBase[] producerRunnable = new ProducerRunnableBase[]{new BuildArtifactsDownloader(
                    buildArtifacts, shouldToast, artifactoryManagerBuilder,
                    buildsCache, indicator, count, total, log, checkCanceled, buildInfoRepo)};
            // Create consumer Runnables.
            ConsumerRunnableBase[] consumerRunnables = new ConsumerRunnableBase[]{new XrayBuildDetailsDownloader(
                    root, buildsCache, xrayClientBuilder, indicator, count, total, log, checkCanceled, serverConfig.getProject())
            };

            new ProducerConsumerExecutor(log, producerRunnable, consumerRunnables, CONNECTION_POOL_SIZE).start();
            checkCanceled.run();
        } catch (CancellationException cancellationException) {
            log.info("Builds scan was canceled.");
        } catch (Exception e) {
            logError(log, "Failed to build CI tree", e, shouldToast);
        }
    }

    public BuildDependencyTree loadBuildTree(BuildGeneralInfo buildGeneralInfo) throws IOException, ParseException {
        BuildDependencyTree buildDependencyTree = new BuildDependencyTree();
        // Load build info from cache
        Build build = buildsCache.loadBuildInfo(mapper, buildGeneralInfo.getBuildName(), buildGeneralInfo.getBuildNumber());
        if (build == null) {
            throw new IOException(String.format("Couldn't find build info object in cache for '%s/%s'.", buildGeneralInfo.getBuildName(), buildGeneralInfo.getBuildNumber()));
        }
        buildDependencyTree.createBuildDependencyTree(build, log);

        // If the build was scanned by Xray, load Xray 'details/build' response from cache
        DetailsResponse detailsResponse = buildsCache.loadScanResults(mapper, buildGeneralInfo.getBuildName(), buildGeneralInfo.getBuildNumber());
        buildDependencyTree.populateBuildDependencyTree(detailsResponse);

        return buildDependencyTree;
    }

    /**
     * Recursively, add all dependencies list licenses to the licenses set.
     */
    public Set<License> getAllLicenses() {
        Set<License> allLicences = newHashSet();
        root.collectAllScopesAndLicenses(newHashSet(), allLicences);
        return allLicences;
    }

    /**
     * Recursively, add all dependencies list scopes to the scopes set.
     */
    public Set<Scope> getAllScopes() {
        Set<Scope> allScopes = newHashSet();
        root.collectAllScopesAndLicenses(allScopes, newHashSet());
        return allScopes;
    }
}
