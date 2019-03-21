package org.jfrog.persistency;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
 * @author yahavi
 */
@Getter
@Setter
class ScanCacheMap {

    private static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @JsonProperty("version")
    private int version = 0;
    @JsonProperty("artifactsMap")
    private Map<String, ScanCacheObject> artifactsMap;

    ScanCacheMap() {
        artifactsMap = new HashMap<>();
    }

    int getVersion() {
        return version;
    }

    void setVersion(int version) {
        this.version = version;
    }

    Map<String, ScanCacheObject> getArtifactsMap() {
        return artifactsMap;
    }

    void setArtifactsMap(Map<String, ScanCacheObject> artifactsMap) {
        this.artifactsMap = artifactsMap;
    }

    void put(Artifact artifact) {
        artifactsMap.put(artifact.getGeneralInfo().getComponentId(), new ScanCacheObject(artifact));
    }

    Artifact get(String id) {
        ScanCacheObject scanCacheObject = artifactsMap.get(id);
        if (scanCacheObject == null) {
            return null;
        }
        return scanCacheObject.getArtifact();
    }

    boolean contains(String id) {
        return artifactsMap.containsKey(id);
    }

    void removeInvalidated() {
        artifactsMap.entrySet().removeIf(entry -> entry.getValue().isInvalidated());
    }

    void write(File file) throws IOException {
        objectMapper.writeValue(file, this);
    }

    void read(File file, Log logger) throws IOException {
        ScanCacheMap scanCacheMap = objectMapper.readValue(file, ScanCacheMap.class);
        if (scanCacheMap.getVersion() != version) {
            logger.warn("Incorrect cache version. Zapping the old cache and starting a new one.");
            return;
        }
        this.artifactsMap = scanCacheMap.artifactsMap;
    }
}
