package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.BuildsScanCache;
import com.jfrog.ide.common.utils.Constants;
import com.jfrog.xray.client.impl.XrayClientBuilder;
import com.jfrog.xray.client.services.details.DetailsResponse;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;
import org.jfrog.build.extractor.scan.DependencyTree;
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
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Sets.newHashSet;
import static com.jfrog.ide.common.utils.ArtifactoryConnectionUtils.createDependenciesClientBuilder;
import static com.jfrog.ide.common.utils.ArtifactoryConnectionUtils.isArtifactoryVersionSupported;
import static com.jfrog.ide.common.utils.Utils.createMapper;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;
import static org.jfrog.build.client.PreemptiveHttpClientBuilder.CONNECTION_POOL_SIZE;

/**
 * Managing the CI dependency tree creation process.
 *
 * @author yahavi
 */
public class CiManagerBase {
    protected DependencyTree root = new DependencyTree();
    private final ObjectMapper mapper = createMapper();
    private final BuildsScanCache buildsCache;
    private final ServerConfig serverConfig;
    private final Log log;

    public CiManagerBase(Path cachePath, String projectName, Log log, ServerConfig serverConfig) throws IOException {
        this.buildsCache = new BuildsScanCache(projectName, cachePath);
        this.serverConfig = serverConfig;
        this.log = log;
    }

    /**
     * Build the builds dependency tree.
     *
     * @param buildsPattern - The build pattern configured in the IDE configuration
     * @param indicator     - The progress indicator to show
     * @throws NoSuchAlgorithmException in case of error during creating the Artifactory dependencies client.
     * @throws KeyStoreException        in case of error during creating the Artifactory dependencies client.
     * @throws KeyManagementException   in case of error during creating the Artifactory dependencies client.
     */
    public void buildCiTree(String buildsPattern, ProgressIndicator indicator) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        root = new DependencyTree();
        XrayClientBuilder xrayClientBuilder = createXrayClientBuilder(serverConfig, log);
        ArtifactoryDependenciesClientBuilder dependenciesClientBuilder = createDependenciesClientBuilder(serverConfig, log);
        try (ArtifactoryDependenciesClient dependenciesClient = dependenciesClientBuilder.build()) {
            buildsCache.createDirectories();
            if (!isArtifactoryVersionSupported(dependenciesClient.getArtifactoryVersion())) {
                log.error("Unsupported JFrog Artifactory version: Required JFrog Artifactory version " + Constants.MINIMAL_ARTIFACTORY_VERSION_SUPPORTED + " and above.");
                return;
            }
            AqlSearchResult searchResult = dependenciesClient.searchArtifactsByAql(createAql(buildsPattern));
            if (searchResult.getResults().isEmpty()) {
                return;
            }

            Queue<AqlSearchResult.SearchEntry> buildArtifacts = new ArrayBlockingQueue<>(searchResult.getResults().size(), false);
            buildArtifacts.addAll(searchResult.getResults());

            AtomicInteger count = new AtomicInteger();
            double total = buildArtifacts.size() * 2;
            // Create producer Runnables.
            ProducerRunnableBase[] producerRunnable = new ProducerRunnableBase[]{
                    new BuildArtifactsDownloader(buildArtifacts, dependenciesClientBuilder, buildsCache, indicator, count, total, log)};
            // Create consumer Runnables.
            ConsumerRunnableBase[] consumerRunnables = new ConsumerRunnableBase[]{
                    new XrayBuildDetailsDownloader(root, buildsCache, xrayClientBuilder, indicator, count, total, log)
            };
            // Create the deployment executor.
            ProducerConsumerExecutor deploymentExecutor = new ProducerConsumerExecutor(log, producerRunnable, consumerRunnables, CONNECTION_POOL_SIZE);
            deploymentExecutor.start();
        } catch (Exception exception) {
            log.error("Failed to build CI tree", exception);
        }
    }

    public BuildDependencyTree loadBuildTree(String buildName, String buildNumber) throws IOException, ParseException {
        BuildDependencyTree buildDependencyTree = new BuildDependencyTree();

        // Load build info from cache
        Build build = buildsCache.loadBuildInfo(mapper, buildName, buildNumber, log);
        if (build == null) {
            throw new IOException(String.format("Couldn't find build info object in cache for '%s/%s'.", buildName, buildNumber));
        }
        buildDependencyTree.createBuildDependencyTree(build, log);

        // If the build scanned by Xray, load Xray 'details/build' response from cache
        DetailsResponse detailsResponse = buildsCache.loadDetailsResponse(mapper, buildName, buildNumber, log);
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

    private String createAql(String buildsPattern) {
        return String.format("items.find({" +
                "\"repo\":\"artifactory-build-info\"," +
                "\"path\":{\"$match\":\"%s\"}}" +
                ").include(\"name\",\"repo\",\"path\",\"created\").sort({\"$desc\":[\"created\"]}).limit(10)", buildsPattern);
    }
}
