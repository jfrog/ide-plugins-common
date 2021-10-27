package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.BuildsScanCache;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.DownloadResponse;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jfrog.ide.common.ci.Utils.BUILD_RET_ERR_FMT;
import static com.jfrog.ide.common.ci.Utils.createBuildGeneralInfo;
import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.common.utils.Utils.createMapper;

/**
 * Download build info artifacts from Artifactory, save them in cache and produce the build general info to the consumer -
 * {@link XrayBuildDetailsDownloader}.
 *
 * @author yahavi
 */
public class BuildArtifactsDownloader extends ProducerRunnableBase {
    private final ArtifactoryManagerBuilder artifactoryManagerBuilder;
    private final Queue<AqlSearchResult.SearchEntry> buildArtifacts;
    private final ProgressIndicator indicator;
    private final BuildsScanCache buildsCache;
    private final Runnable checkCancel;
    private final String buildInfoRepo;
    private final AtomicInteger count;
    private final boolean shouldToast;
    private final double total;
    private final Log log;

    public BuildArtifactsDownloader(Queue<AqlSearchResult.SearchEntry> buildArtifacts, boolean shouldToast,
                                    ArtifactoryManagerBuilder artifactoryManagerBuilder, BuildsScanCache buildsCache,
                                    ProgressIndicator indicator, AtomicInteger count, double total, Log log, Runnable checkCancel,
                                    String buildInfoRepo) {
        this.artifactoryManagerBuilder = artifactoryManagerBuilder;
        this.buildArtifacts = buildArtifacts;
        this.buildInfoRepo = buildInfoRepo;
        this.shouldToast = shouldToast;
        this.buildsCache = buildsCache;
        this.checkCancel = checkCancel;
        this.indicator = indicator;
        this.count = count;
        this.total = total;
        this.log = log;
    }

    @Override
    public void producerRun() throws InterruptedException {
        ObjectMapper mapper = createMapper();

        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            while (!buildArtifacts.isEmpty()) {
                if (Thread.interrupted()) {
                    // Stop the producer if the thread received an interruption event
                    break;
                }
                AqlSearchResult.SearchEntry searchEntry = buildArtifacts.remove();
                String buildName = searchEntry.getPath();
                String buildNumber = StringUtils.substringBefore(searchEntry.getName(), "-");
                try {
                    checkCancel.run();
                    String encodedBuildName = new URLCodec().decode(buildName);
                    Build build = buildsCache.loadBuildInfo(mapper, encodedBuildName, buildNumber);
                    if (build == null) {
                        build = downloadBuildInfo(mapper, searchEntry, artifactoryManager);
                    }

                    // Create and produce the build general info to the consumer
                    executor.put(createBuildGeneralInfo(build, log));
                } catch (CancellationException e) {
                    break;
                } catch (ParseException | IllegalArgumentException | DecoderException e) {
                    logError(log, String.format(BUILD_RET_ERR_FMT, buildName, buildNumber), e, shouldToast);
                } catch (IOException e) {
                    logError(log, "", e, shouldToast);
                } finally {
                    indicator.setFraction(count.incrementAndGet() / total);
                }
            }
        }
    }

    /**
     * Download build info from Artifactory and save it in the builds cache.
     *
     * @param mapper             - The object mapper
     * @param searchEntry        - The AQL search results entry
     * @param artifactoryManager - Artifactory manager
     * @return the requested build or null if not found.
     */
    private Build downloadBuildInfo(ObjectMapper mapper, AqlSearchResult.SearchEntry searchEntry, ArtifactoryManager artifactoryManager) throws IOException {
        String downloadUrl = String.format("/%s/%s/%s", buildInfoRepo, searchEntry.getPath(), searchEntry.getName());
        try {
            DownloadResponse downloadResponse = artifactoryManager.download(downloadUrl);
            if (downloadResponse == null) {
                throw new IOException("An empty response received from Artifactory.");
            }
            String buildInfoContent = downloadResponse.getContent();
            Build build = mapper.readValue(buildInfoContent, Build.class);
            buildsCache.save(buildInfoContent.getBytes(StandardCharsets.UTF_8), build.getName(), build.getNumber(), BuildsScanCache.Type.BUILD_INFO);
            return build;
        } catch (IOException e) {
            throw new IOException("Couldn't retrieve build information from Artifactory, using this path: " + downloadUrl, e);
        }
    }
}
