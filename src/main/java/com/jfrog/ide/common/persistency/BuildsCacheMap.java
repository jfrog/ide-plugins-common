package com.jfrog.ide.common.persistency;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.ci.BuildGeneralInfo;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The implementation of the scan cache. Contains a version and a map.
 * In case of incorrect version, it zaps the cache and starts over a new map.
 *
 * @author yahavi
 */
@Getter
@Setter
class BuildsCacheMap extends ScanCacheMap {

    @JsonProperty("buildStatus")
    private BuildGeneralInfo.Status buildStatus;
    @JsonProperty("vcs")
    private Vcs vcs;

    BuildsCacheMap(BuildGeneralInfo.Status buildStatus, Vcs vcs) {
        this();
        this.buildStatus = buildStatus;
        this.vcs = vcs;
    }

    BuildsCacheMap() {
        artifactsMap = new ConcurrentHashMap<>();
    }

    public BuildGeneralInfo.Status getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(BuildGeneralInfo.Status buildStatus) {
        this.buildStatus = buildStatus;
    }

    public Vcs getVcs() {
        return vcs;
    }

    public void setVcs(Vcs vcs) {
        this.vcs = vcs;
    }

    @Override
    void read(File file, Log logger) throws IOException {
        BuildsCacheMap buildsCacheMap = (BuildsCacheMap) readCommonCache(file, logger);
        if (buildsCacheMap == null) {
            return;
        }
        this.buildStatus = buildsCacheMap.buildStatus;
        this.vcs = buildsCacheMap.vcs;
    }
}
