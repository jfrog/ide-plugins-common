package com.jfrog.ide.common.persistency;

import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * @author yahavi
 **/
public class XrayScanCache extends ScanCache {

    /**
     * Construct an Xray scan cache.
     *
     * @param projectName - The IDE project name. If this is an npm project, it is a full path to the directory containing the package.json.
     * @param basePath    - The directory for the cache.
     * @param logger      - The logger.
     * @throws IOException in case of I/O problem in the paths.
     */
    public XrayScanCache(String projectName, Path basePath, Log logger) throws IOException {
        scanCacheMap = new XrayScanCacheMap();
        file = basePath.resolve(Base64.getEncoder().encodeToString(projectName.getBytes(StandardCharsets.UTF_8)) + "XrayScanCache.json").toFile();
        logger.debug("Project cache path: " + file.getAbsolutePath());
        if (!file.exists()) {
            Files.createDirectories(basePath);
            return;
        }
        scanCacheMap.read(file, logger);
    }
}
