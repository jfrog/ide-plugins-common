package org.jfrog.persistency;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
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
    private transient static ObjectMapper objectMapper = new ObjectMapper();

    @JsonProperty("version")
    private int version = 1;
    @JsonProperty("artifactsMap")
    private Map<String, ScanCacheObject> artifactsMap;

    ScanCacheMap() {
        artifactsMap = new HashMap<>();
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

    void read(File file) throws IOException {
        ScanCacheMap scanCacheMap = objectMapper.readValue(file, ScanCacheMap.class);
        if (scanCacheMap.version != version) {
            System.out.println("Incorrect version");
            return;
        }
        this.artifactsMap = scanCacheMap.artifactsMap;
    }
}
