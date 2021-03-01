package com.jfrog.ide.common.persistency.local;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.Artifact;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The implementation of the scan cache. Contains a version and a map.
 * In case of incorrect version, it zaps the cache and starts over a new map.
 *
 * @author yahavi
 */
@Getter
@Setter
class ScanCacheMap {

    private static int CACHE_VERSION = 0;
    private static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @JsonProperty("version")
    private int version = CACHE_VERSION;
    @JsonProperty("artifactsMap")
    private Map<String, ScanCacheObject> artifactsMap;

    ScanCacheMap() {
        artifactsMap = new HashMap<>();
    }

    /**
     * Put an artifact in the map.
     *
     * @param artifact - The artifact to put.
     */
    void put(Artifact artifact) {
        artifactsMap.put(artifact.getGeneralInfo().getComponentId(), new ScanCacheObject(artifact));
    }

    /**
     * Get an artifact from the map.
     *
     * @param id - The artifact id.
     * @return artifact from the map or null if absent.
     */
    Artifact get(String id) {
        ScanCacheObject scanCacheObject = artifactsMap.get(id);
        if (scanCacheObject == null) {
            return null;
        }
        return scanCacheObject.getArtifact();
    }

    /**
     * return true iff the map contains the artifact id.
     *
     * @param id - The id to search.
     * @return true iff the map contains the artifact id.
     */
    boolean contains(String id) {
        return artifactsMap.containsKey(id);
    }

    /**
     * Remove artifacts older than 1 week.
     */
    void removeInvalidated() {
        artifactsMap.entrySet().removeIf(entry -> entry.getValue().isInvalidated());
    }

    /**
     * Write the version and the map to disk.
     *
     * @param file - The cache file.
     * @throws IOException in case of I/O error during write.
     */
    void write(File file) throws IOException {
        objectMapper.writeValue(file, this);
    }

    /**
     * Load the cache map from disk. If version incorrect, it does nothing.
     *
     * @param file   - The cache file.
     * @param logger - The logger.
     * @throws IOException in case of I/O error during read.
     */
    void read(File file, Log logger) throws IOException {
        try {
            ScanCacheMap scanCacheMap = objectMapper.readValue(file, ScanCacheMap.class);
            if (scanCacheMap.getVersion() != version) {
                logger.warn("Incorrect cache version " + scanCacheMap.getVersion() + ". Zapping the old cache and starting a new one.");
                return;
            }
            this.artifactsMap = scanCacheMap.artifactsMap;
        } catch (JsonParseException | JsonMappingException e) {
            logger.error("Failed reading cache file, zapping the old cache and starting a new one.");
        }
    }
}
