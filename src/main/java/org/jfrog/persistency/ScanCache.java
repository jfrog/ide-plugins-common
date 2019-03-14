package org.jfrog.persistency;

import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author yahavi
 */
public class ScanCache {

    private ScanCacheMap scanCacheMap;
    private File file;

    public ScanCache(String projectName) throws IOException {
        this(projectName, Paths.get(".jfrog"));
    }

    ScanCache(String projectName, Path basePath) throws IOException {
        scanCacheMap = new ScanCacheMap();
        file = basePath.resolve(projectName + "_XrayScanCache.json").toFile();
        if (!file.exists()) {
            Files.createDirectories(basePath);
            return;
        }
        scanCacheMap.read(file);
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

}
