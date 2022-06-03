package com.jfrog.ide.common.persistency;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.Artifact;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.jfrog.ide.common.utils.Utils.createMapper;

/**
 * The implementation of the scan cache. Contains a version and a map.
 * In case of incorrect version, it zaps the cache and starts over a new map.
 *
 * @author yahavi
 */
@Getter
@Setter
abstract class ScanCacheMap {

    static int CACHE_VERSION = 3;
    static ObjectMapper objectMapper = createMapper();

    @JsonProperty("version")
    int version = CACHE_VERSION;
    @JsonProperty("artifactsMap")
    Map<String, ScanCacheObject> artifactsMap;

    abstract void read(File file, Log logger) throws IOException;

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
     * Write the version and the map to disk.
     *
     * @param file - The cache file.
     * @throws IOException in case of I/O error during write.
     */
    void write(File file) throws IOException {
        objectMapper.writeValue(file, this);
    }

    void readCommonCache(File file, Log logger) throws IOException {
        try {
            ScanCacheMap scanCacheMap = objectMapper.readValue(file, getClass());
            if (scanCacheMap.getVersion() != version) {
                logger.warn("Incorrect cache version " + scanCacheMap.getVersion() + ". Zapping the old cache and starting a new one.");
                return;
            }
            this.artifactsMap = Collections.synchronizedMap(scanCacheMap.artifactsMap);
        } catch (JsonParseException | JsonMappingException e) {
            logger.warn("Failed reading cache file, zapping the old cache and starting a new one.");
        }
    }
}
