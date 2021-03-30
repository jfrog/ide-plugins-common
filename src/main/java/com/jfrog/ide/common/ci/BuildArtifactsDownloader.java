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

import static com.jfrog.ide.common.utils.Utils.createMapper;

/**
 * @author yahavi
 **/
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
                    Build build = tryLoadFromCache(mapper, buildName, buildNumber);
                    if (build == null) {
                        build = downloadBuildInfo(mapper, buildName, buildNumber, searchEntry, client, baseRepoUrl);
                    }
                    if (build == null) {
                        continue;
                    }
                    CiDependencyTree buildDependencyTree = new CiDependencyTree(build);
                    buildDependencyTree.createBuildDependencyTree();
                    executor.put(buildDependencyTree);
                } catch (ParseException | IOException e) {
                    String msg = String.format("Couldn't retrieve build information for build '%s/%s'.", buildName, buildNumber);
                    log.error(msg, e);
                } finally {
                    indicator.setFraction(count.incrementAndGet() / total);
                }
            }
        }
    }

    private Build tryLoadFromCache(ObjectMapper mapper, String buildName, String buildNumber) {
        try {
            byte[] buffer = buildsCache.load(buildName, buildNumber, BuildsScanCache.Type.BUILD_INFO);
            if (buffer != null) {
                return mapper.readValue(buffer, Build.class);
            }
        } catch (IOException e) {
            String msg = String.format("Failed reading cache file for '%s%s', zapping the old cache and starting a new one.", buildName, buildNumber);
            log.error(msg, e);
        }
        return null;
    }

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
