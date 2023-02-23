package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SeverityReason {
    @JsonProperty()
    private String name;
    @JsonProperty()
    private String description;
    @JsonProperty()
    private boolean isPositive;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    private SeverityReason() {
    }

    public SeverityReason(String name, String description, boolean isPositive) {
        this.name = name;
        this.description = description;
        this.isPositive = isPositive;
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unused")
    public boolean isPositive() {
        return isPositive;
    }
}
