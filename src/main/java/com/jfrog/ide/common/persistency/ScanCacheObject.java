package com.jfrog.ide.common.persistency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.extractor.scan.Artifact;

import java.util.concurrent.TimeUnit;

/**
 * Cache object for the Xray scan. Contains an artifact and the last update time.
 *
 * @author yahavi
 */
@SuppressWarnings("unused")
@Setter
@Getter
class ScanCacheObject implements Comparable<ScanCacheObject> {
    private static final long EXPIRATION = TimeUnit.DAYS.toMillis(7);

    @JsonProperty("artifact")
    private Artifact artifact;
    @JsonProperty("lastUpdated")
    private long lastUpdated;

    @SuppressWarnings("WeakerAccess")
    public ScanCacheObject() {
        this.lastUpdated = System.currentTimeMillis();
    }

    ScanCacheObject(Artifact artifact) {
        this.lastUpdated = System.currentTimeMillis();
        this.artifact = artifact;
    }

    /**
     * Return true iff this artifact is older than 1 week. Relevant only for the time based cache.
     *
     * @return true iff this artifact is older than 1 week.
     */
    @JsonIgnore
    boolean isExpired() {
        return System.currentTimeMillis() - lastUpdated > EXPIRATION;
    }

    @Override
    public int compareTo(ScanCacheObject o) {
        return Long.compare(getLastUpdated(), o.getLastUpdated());
    }
}
