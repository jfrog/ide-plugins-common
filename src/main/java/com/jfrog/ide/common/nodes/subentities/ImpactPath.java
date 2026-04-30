package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Objects;


@Getter
public class ImpactPath {
    @JsonProperty()
    private String name;
    @JsonProperty()
    private String version;
    @JsonProperty()
    private String id;

    // empty constructor for deserialization
    @SuppressWarnings("unused")
    public ImpactPath() {
    }

    @SuppressWarnings("unused")
    public ImpactPath(String dependencyName, String dependencyVersion) {
        this.name = dependencyName;
        this.version = dependencyVersion;
    }

    @SuppressWarnings("unused")
    public ImpactPath(String dependencyId, String dependencyName, String dependencyVersion) {
        this.id = dependencyId;
        this.name = dependencyName;
        this.version = dependencyVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImpactPath that = (ImpactPath) o;
        return Objects.equals(this.name, that.name) && Objects.equals(this.version, that.version);
    }
}