package org.jfrog.persistency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.extractor.scan.Artifact;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
@Setter @Getter
class ScanCacheObject implements Serializable {
    private static final long serialVersionUID = 0L;
    private static final long MAX_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(7);

    @JsonProperty("artifact")
    private Artifact artifact;
    @JsonProperty("lastUpdated")
    private long lastUpdated;

    public ScanCacheObject() {
        this.lastUpdated = System.currentTimeMillis();
    }

    @SuppressWarnings("WeakerAccess")
    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    ScanCacheObject(Artifact artifact) {
        this.lastUpdated = System.currentTimeMillis();
        this.artifact = artifact;
    }

    @JsonIgnore
    boolean isInvalidated() {
        return System.currentTimeMillis() - lastUpdated > MAX_EXPIRATION_TIME;
    }
}
