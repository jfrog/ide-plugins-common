package com.jfrog.ide.common.ci;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.jfrog.ide.common.log.ProgressIndicator;
import org.jfrog.build.api.Build;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jfrog.build.client.PreemptiveHttpClientBuilder.CONNECTION_POOL_SIZE;

/**
 * @author yahavi
 **/
public class CiManagerBase {
    protected DependencyTree root = new DependencyTree();

    public void buildCiTree(String buildsPattern, ArtifactoryDependenciesClientBuilder dependenciesClientBuilder,
                            String projectPath, Log logger, ProgressIndicator indicator) {
        root.setGeneralInfo(new GeneralInfo().path(projectPath));
        try (ArtifactoryDependenciesClient dependenciesClient = dependenciesClientBuilder.build()) {
            AqlSearchResult searchResult = dependenciesClient.searchArtifactsByAql(createAql(buildsPattern));
            Queue<AqlSearchResult.SearchEntry> buildArtifacts = new ArrayBlockingQueue<>(searchResult.getResults().size(), false);
            buildArtifacts.addAll(searchResult.getResults());

            AtomicInteger count = new AtomicInteger();
            double total = buildArtifacts.size();
            // Create producer Runnables.
            ProducerRunnableBase[] producerRunnable = new ProducerRunnableBase[]{
                    new BuildArtifactsDownloader(buildArtifacts, dependenciesClient, indicator, count, total)};
            // Create consumer Runnables.
            Multimap<String, DependencyTree> branchDependencyTreeItems = Multimaps.synchronizedSetMultimap(HashMultimap.create());
            ConsumerRunnableBase[] consumerRunnables = new ConsumerRunnableBase[]{
                    new XrayScanBuildResultsDownloader(root)
            };
            // Create the deployment executor.
            ProducerConsumerExecutor deploymentExecutor = new ProducerConsumerExecutor(logger, producerRunnable, consumerRunnables, CONNECTION_POOL_SIZE);
            deploymentExecutor.start();

        } catch (Exception exception) {
            logger.error("Failed to build CI tree", exception);
        }
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

    private long getLastModified(String date) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        Date parse = simpleDateFormat.parse(date);
        return parse.getTime();
    }

}
