package com.jfrog.ide.common.tree;

public class SeverityReason {
    String name;
    String description;
    boolean isPositive;

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
