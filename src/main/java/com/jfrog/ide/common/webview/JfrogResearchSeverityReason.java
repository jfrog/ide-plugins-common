package com.jfrog.ide.common.webview;

import lombok.Getter;

@Getter
public class JfrogResearchSeverityReason {
    private final String name;
    private final String description;
    private final boolean isPositive;

    public JfrogResearchSeverityReason(String name, String description, boolean isPositive) {
        this.name = name;
        this.description = description;
        this.isPositive = isPositive;
    }
}
