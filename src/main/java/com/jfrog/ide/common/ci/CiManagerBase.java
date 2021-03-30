package com.jfrog.ide.common.ci;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.BuildsScanCache;
import com.jfrog.xray.client.impl.XrayClientBuilder;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
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
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Sets.newHashSet;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.createDependenciesClientBuilder;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;
import static org.jfrog.build.client.PreemptiveHttpClientBuilder.CONNECTION_POOL_SIZE;

/**
 * @author yahavi
 **/
public class CiManagerBase {
    protected DependencyTree root = new DependencyTree();
    private final BuildsScanCache buildsCache;
    private final ServerConfig serverConfig;
    private final Log log;

    public CiManagerBase(Path cachePath, String projectName, Log log, ServerConfig serverConfig) throws IOException {
        this.buildsCache = new BuildsScanCache(projectName, cachePath, log);
        this.serverConfig = serverConfig;
        this.log = log;
    }

    public void buildCiTree(String buildsPattern, String projectPath, ProgressIndicator indicator) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        root = new DependencyTree();
        root.setGeneralInfo(new GeneralInfo().path(projectPath));
        ArtifactoryDependenciesClientBuilder dependenciesClientBuilder = createDependenciesClientBuilder(serverConfig, log);
        XrayClientBuilder xrayClientBuilder = createXrayClientBuilder(serverConfig, log);
        try (ArtifactoryDependenciesClient dependenciesClient = dependenciesClientBuilder.build()) {
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
