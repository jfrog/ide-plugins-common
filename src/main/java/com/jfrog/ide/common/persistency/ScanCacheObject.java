package com.jfrog.ide.common.persistency;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.FileTreeNode;
import lombok.Getter;

import java.util.List;

/**
 * The implementation of the scan cache. Contains a version and a map.
 * In case of incorrect version, it zaps the cache and starts over a new map.
 */
@Getter
public class ScanCacheObject {
    static int CACHE_VERSION = 5;

    @JsonProperty("version")
    int version = CACHE_VERSION;

    @JsonProperty("fileTreeNodes")
    List<FileTreeNode> fileTreeNodes;
    @JsonProperty("scanTimestamp")
    long scanTimestamp;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    public ScanCacheObject() {
    }

    public ScanCacheObject(List<FileTreeNode> fileTreeNodes, long scanTimestamp) {
        this.fileTreeNodes = fileTreeNodes;
        this.scanTimestamp = scanTimestamp;
    }
}
