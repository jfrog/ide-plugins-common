package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.BuildsScanCache;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;

import java.io.IOException;
import java.text.ParseException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jfrog.ide.common.ci.Utils.BUILD_RET_ERR_FMT;
import static com.jfrog.ide.common.utils.Utils.createMapper;

/**
 * Produces build dependency trees and also save them in cache.
 *
 * @author yahavi
 */
public class BuildArtifactsDownloader extends ProducerRunnableBase {
    public static final String BUILD_INFO_REPO = "/artifactory-build-info/";

    private final ArtifactoryDependenciesClientBuilder clientBuilder;
    private final Queue<AqlSearchResult.SearchEntry> buildArtifacts;
    private final ProgressIndicator indicator;
    private final BuildsScanCache buildsCache;
    private final AtomicInteger count;
    private final double total;
    private final Log log;

    public BuildArtifactsDownloader(Queue<AqlSearchResult.SearchEntry> buildArtifacts,
                                    ArtifactoryDependenciesClientBuilder clientBuilder, BuildsScanCache buildsCache,
                                    ProgressIndicator indicator, AtomicInteger count, double total, Log log) {
        this.buildArtifacts = buildArtifacts;
        this.clientBuilder = clientBuilder;
        this.buildsCache = buildsCache;
        this.indicator = indicator;
        this.count = count;
        this.total = total;
        this.log = log;
    }

    @Override
    public void producerRun() throws InterruptedException {
        ObjectMapper mapper = createMapper();

        try (ArtifactoryDependenciesClient client = clientBuilder.build()) {
            String baseRepoUrl = client.getArtifactoryUrl() + BUILD_INFO_REPO;
            while (!buildArtifacts.isEmpty()) {
                if (Thread.interrupted()) {
                    break;
                }
                AqlSearchResult.SearchEntry searchEntry = buildArtifacts.remove();
                String buildName = searchEntry.getPath();
                String buildNumber = StringUtils.substringBefore(searchEntry.getName(), "-");
                try {
                    Build build = buildsCache.loadBuildInfo(mapper, buildName, buildNumber, log);
                    if (build == null) {
                        build = downloadBuildInfo(mapper, buildName, buildNumber, searchEntry, client, baseRepoUrl);
                    }
                    if (build == null) {
                        continue;
                    }
                    BuildDependencyTree buildDependencyTree = new BuildDependencyTree();
                    buildDependencyTree.createBuildDependencyTree(build);
                    executor.put(buildDependencyTree);
                } catch (ParseException | IllegalArgumentException e) {
                    log.error(String.format(BUILD_RET_ERR_FMT, buildName, buildNumber), e);
                } finally {
                    indicator.setFraction(count.incrementAndGet() / total);
                }
            }
        }
    }

    /**
     * Download build info from Artifactory and save it in the builds cache.
     *
     * @param mapper      - The object mapper
     * @param buildName   - Build name
     * @param buildNumber - Build number
     * @param searchEntry - The AQL search results entry
     * @param client      - Artifactory dependencies client
     * @param baseRepoUrl - Artifactory build info repository
     * @return the requested build or null if not found.
     */
    private Build downloadBuildInfo(ObjectMapper mapper, String buildName, String buildNumber,
                                    AqlSearchResult.SearchEntry searchEntry, ArtifactoryDependenciesClient client, String baseRepoUrl) {
        String downloadUrl = baseRepoUrl + searchEntry.getPath() + "/" + searchEntry.getName();
        HttpEntity entity = null;
        try (CloseableHttpResponse response = client.downloadArtifact(downloadUrl)) {
            entity = response.getEntity();
            byte[] content = IOUtils.toByteArray(entity.getContent());
            Build build = mapper.readValue(content, Build.class);
            buildsCache.save(content, buildName, buildNumber, BuildsScanCache.Type.BUILD_INFO);
            return build;
        } catch (IOException e) {
            log.error("Couldn't retrieve build information", e);
        } finally {
            EntityUtils.consumeQuietly(entity);
        }
        return null;
    }
}
