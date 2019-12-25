package com.jfrog.ide.common.persistency;

import com.jfrog.ide.common.utils.Utils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.Artifact;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Cache for Xray scan.
 *
 * @author yahavi
 */
public class ScanCache {

    private ScanCacheMap scanCacheMap;
    private File file;

    /**
     * Construct an Xray scan cache.
     *
     * @param projectName - The IDE project name. If this is an npm project, it is a full path to the directory containing the package.json.
     * @param basePath    - The directory for the cache.
     * @param logger      - The logger.
     * @throws IOException in case of I/O problem in the paths.
     */
    public ScanCache(String projectName, Path basePath, Log logger) throws IOException {
        scanCacheMap = new ScanCacheMap();
        file = basePath.resolve(Base64.getEncoder().encodeToString(projectName.getBytes(StandardCharsets.UTF_8)) + "XrayScanCache.json").toFile();
        if (!file.exists()) {
            Files.createDirectories(basePath);
            return;
        }
        scanCacheMap.read(file, logger);
        scanCacheMap.removeInvalidated();
    }

    public void write() throws IOException {
        scanCacheMap.write(file);
    }

    public void add(com.jfrog.xray.client.services.summary.Artifact artifact) {
        scanCacheMap.put(Utils.getArtifact(artifact));
    }

    public Artifact get(String id) {
        return scanCacheMap.get(id);
    }

    public boolean contains(String id) {
        return scanCacheMap.contains(id);
    }

    ScanCacheMap getScanCacheMap() {
        return scanCacheMap;
    }

    void setScanCacheMap(ScanCacheMap scanCacheMap) {
        this.scanCacheMap = scanCacheMap;
    }
}
