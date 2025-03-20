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

    // empty constructor for deserialization
    public ImpactPath() {
    }

    public ImpactPath(String dependencyName, String dependencyVersion) {
        this.name = dependencyName;
        this.version = dependencyVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImpactPath that = (ImpactPath) o;
        return Objects.equals(name, that.name) && Objects.equals(version, that.version);
    }
}
