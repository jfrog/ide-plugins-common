package com.jfrog.ide.common.ci;

import com.google.common.collect.Sets;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.BuildsScanCache;
import com.jfrog.ide.common.persistency.SingleBuildCache;
import com.jfrog.xray.client.impl.XrayClientBuilder;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;
import org.jfrog.build.extractor.scan.*;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jfrog.ide.common.utils.XrayConnectionUtils.createDependenciesClientBuilder;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;
import static org.jfrog.build.client.PreemptiveHttpClientBuilder.CONNECTION_POOL_SIZE;

/**
 * @author yahavi
 **/
public class CiManagerBase {
    public static final String DEPENDENCIES_NODE = "dependencies";
    public static final String ARTIFACTS_NODE = "artifacts";
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

            List<DependencyTree> cachedDependencyTrees = Lists.newArrayList();
            List<AqlSearchResult.SearchEntry> newBuilds = createBuildsTreeFromCache(cachedDependencyTrees, searchResult.getResults());
            if (!newBuilds.isEmpty()) {
                Queue<AqlSearchResult.SearchEntry> buildArtifacts = new ArrayBlockingQueue<>(newBuilds.size(), false);
                buildArtifacts.addAll(newBuilds);

                AtomicInteger count = new AtomicInteger();
                double total = buildArtifacts.size() * 2;
                // Create producer Runnables.
                ProducerRunnableBase[] producerRunnable = new ProducerRunnableBase[]{
                        new BuildArtifactsDownloader(buildArtifacts, dependenciesClientBuilder, indicator, count, total, log)};
                // Create consumer Runnables.
                ConsumerRunnableBase[] consumerRunnables = new ConsumerRunnableBase[]{
                        new XrayBuildDetailsDownloader(root, xrayClientBuilder, indicator, count, total, log)
                };
                // Create the deployment executor.
                ProducerConsumerExecutor deploymentExecutor = new ProducerConsumerExecutor(log, producerRunnable, consumerRunnables, CONNECTION_POOL_SIZE);
                deploymentExecutor.start();

                // Cache dependency trees
                for (DependencyTree child : root.getChildren()) {
                    buildsCache.cacheDependencyTree(child);
                }
            }

            // Cache dependency trees
            for (DependencyTree cachedDependencyTree : cachedDependencyTrees) {
                root.add(cachedDependencyTree);
            }

        } catch (Exception exception) {
            log.error("Failed to build CI tree", exception);
        }
    }

    private List<AqlSearchResult.SearchEntry> createBuildsTreeFromCache(List<DependencyTree> cachedDependenciesTrees, List<AqlSearchResult.SearchEntry> searchResults) {
        List<AqlSearchResult.SearchEntry> newBuilds = Lists.newArrayList();
        for (AqlSearchResult.SearchEntry searchResult : searchResults) {
            String buildName = searchResult.getPath();
            String buildNumber = StringUtils.substringBefore(searchResult.getName(), "-");
            String timestamp = StringUtils.substringBetween(searchResult.getName(), "-", ".json");
            SingleBuildCache singleBuildCache = buildsCache.getBuildCache(buildName, buildNumber, timestamp);
            if (singleBuildCache == null) {
                newBuilds.add(searchResult);
                continue;
            }
            Artifact artifact = singleBuildCache.get(buildName + ":" + buildNumber);
            cachedDependenciesTrees.add(buildDependencyTree(artifact, singleBuildCache));
        }

        return newBuilds;
    }

    private DependencyTree buildDependencyTree(Artifact artifact, SingleBuildCache singleBuildCache) {
        DependencyTree node = new DependencyTree(artifact.getGeneralInfo().getComponentId());
        node.setGeneralInfo(artifact.getGeneralInfo());
        node.setIssues(artifact.getIssues());
        node.setLicenses(artifact.getLicenses());
        node.setScopes(artifact.getScopes());
        for (String child : artifact.getChildren()) {
            node.add(buildDependencyTree(singleBuildCache.get(child), singleBuildCache));
        }
        return node;
    }

    /**
     * Recursively, add all dependencies list licenses to the licenses set.
     */
    public Set<License> getAllLicenses() {
        Set<License> allLicences = Sets.newHashSet();
        root.collectAllScopesAndLicenses(Sets.newHashSet(), allLicences);
        return allLicences;
    }

    /**
     * Recursively, add all dependencies list scopes to the scopes set.
     */
    public Set<Scope> getAllScopes() {
        Set<Scope> allScopes = Sets.newHashSet();
        root.collectAllScopesAndLicenses(allScopes, Sets.newHashSet());
        return allScopes;
    }

    private String createAql(String buildsPattern) {
        return String.format("items.find({" +
                "\"repo\":\"artifactory-build-info\"," +
                "\"path\":{\"$match\":\"%s\"}}" +
                ").include(\"name\",\"repo\",\"path\",\"created\").sort({\"$asc\":[\"created\"]}).limit(10)", buildsPattern);
    }

    private long getBuildTimestamp(String date) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        Date parse = simpleDateFormat.parse(date);
        return parse.getTime();
    }

}
