package com.jfrog.ide.common.parse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;



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
    }
}
