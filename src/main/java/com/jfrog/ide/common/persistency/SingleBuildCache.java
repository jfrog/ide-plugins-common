package com.jfrog.ide.common.persistency;

import com.jfrog.ide.common.ci.BuildGeneralInfo;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * @author yahavi
 **/
public class SingleBuildCache extends ScanCache {

    /**
     * Construct a build scan cache.
     *
     * @param buildName   - Build name.
     * @param buildNumber - Build number.
     * @param timestamp   - Build timestamp.
     * @param basePath    - The directory for the cache.
     * @param logger      - The logger.
     * @throws IOException in case of I/O problem in the paths.
     */
    SingleBuildCache(String buildName, String buildNumber, String timestamp, Path basePath, Log logger, BuildGeneralInfo.Status buildStatus, Vcs vcs) throws IOException {
        scanCacheMap = new BuildsCacheMap(buildStatus, vcs);
        file = basePath.resolve(getBuildFileName(buildName, buildNumber, timestamp)).toFile();
        logger.debug("Build cache path: " + file.getAbsolutePath());
        if (!file.exists()) {
            Files.createDirectories(basePath);
        }
    }

    private SingleBuildCache(ScanCacheMap scanCacheMap, File file) {
        this.scanCacheMap = scanCacheMap;
        this.file = file;
    }

    public Vcs getVcs() {
        return ((BuildsCacheMap) scanCacheMap).getVcs();
    }

    public BuildGeneralInfo.Status getBuildStatus() {
        return ((BuildsCacheMap) scanCacheMap).getBuildStatus();
    }

    static SingleBuildCache getBuildCache(String buildName, String buildNumber, String timestamp, Path basePath, Log logger) throws IOException {
        File file = basePath.resolve(getBuildFileName(buildName, buildNumber, timestamp)).toFile();
        if (!file.exists()) {
            return null;
        }
        ScanCacheMap scanCacheMap = new BuildsCacheMap();
        scanCacheMap.read(file, logger);
        return new SingleBuildCache(scanCacheMap, file);
    }

    static String getBuildFileName(String buildName, String buildNumber, String timestamp) {
        String buildIdentifier = String.format("%s_%s", buildName, buildNumber);
        return timestamp + "-" + Base64.getEncoder().encodeToString(buildIdentifier.getBytes(StandardCharsets.UTF_8));
    }
}
