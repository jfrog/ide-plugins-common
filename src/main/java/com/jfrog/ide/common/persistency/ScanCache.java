package com.jfrog.ide.common.persistency;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.jfrog.ide.common.log.Utils;
import com.jfrog.ide.common.nodes.FileTreeNode;
import lombok.Getter;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static com.jfrog.ide.common.utils.Utils.createMapper;

/**
 * Cache for Xray scan results.
 *
 * @author yahavi
 */
public class ScanCache {
    private final File file;
    private final ObjectMapper objectMapper;
    @Getter
    private ScanCacheObject scanCacheObject;

    /**
     * Construct a scan cache.
     *
     * @param projectId - A unique string of a project. This is used to locate and read the cache later.
     * @param basePath  - The directory for the cache.
     * @param logger    - The logger.
     * @throws IOException in case of I/O problem in the paths.
     */
    public ScanCache(String projectId, Path basePath, Log logger) throws IOException {
        // We do that to allow inheritance on deserialization
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.jfrog.ide.common.nodes")
                .allowIfSubType("com.jfrog.ide.common.nodes.subentities")
                .allowIfSubType("com.jfrog.ide.common.persistency")
                .allowIfSubType("java.util.ArrayList")
                .allowIfSubType("java.util.Vector")
                .build();
        objectMapper = createMapper();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        file = basePath.resolve(Base64.getEncoder().encodeToString(projectId.getBytes(StandardCharsets.UTF_8)) + "XrayScanCache.json").toFile();
        logger.debug("Project cache path: " + file.getAbsolutePath());
        if (!file.exists()) {
            Files.createDirectories(basePath);
            return;
        }
        readCachedNodes(logger);
    }

    /**
     * Write the given {@link FileTreeNode}s to cache.
     *
     * @param nodes a list of {@link FileTreeNode}s to cache.
     * @throws IOException in case of I/O error during write.
     */
    public void cacheNodes(List<FileTreeNode> nodes) throws IOException {
        scanCacheObject = new ScanCacheObject(nodes, System.currentTimeMillis());
        objectMapper.writeValue(file, scanCacheObject);
    }

    public void deleteScanCacheObject() throws IOException {
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Wasn't able to delete the cache file: " + file.getAbsolutePath());
            }
        }
    }

    private void readCachedNodes(Log logger) throws IOException {
        try {
            scanCacheObject = objectMapper.readValue(file, ScanCacheObject.class);
            if (scanCacheObject.getVersion() != ScanCacheObject.CACHE_VERSION) {
                logger.warn("Invalid cache version " + scanCacheObject.getVersion() + ". Ignoring the old cache and starting a new one.");
            }
        } catch (JsonParseException | JsonMappingException e) {
            Utils.logError(logger, "Failed reading cache file. Ignoring the old cache and starting a new one.", e, false);
        }
    }
}
