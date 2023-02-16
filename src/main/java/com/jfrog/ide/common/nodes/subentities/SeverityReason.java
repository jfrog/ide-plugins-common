package com.jfrog.ide.common.nodes.subentities;

public class SeverityReason {
    private String name;
    private String description;
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
