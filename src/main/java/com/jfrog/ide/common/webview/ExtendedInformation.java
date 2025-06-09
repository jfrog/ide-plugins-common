package com.jfrog.ide.common.webview;

import lombok.Getter;

@Getter
public class ExtendedInformation {
    private final String shortDescription;
    private final String fullDescription;
    private final String jfrogResearchSeverity;
    private final String remediation;
    private final JfrogResearchSeverityReason[] jfrogResearchSeverityReason;

    public ExtendedInformation(String shortDescription, String fullDescription, String jfrogResearchSeverity, String remediation, JfrogResearchSeverityReason[] jfrogResearchSeverityReason) {
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.jfrogResearchSeverity = jfrogResearchSeverity;
        this.remediation = remediation;
        this.jfrogResearchSeverityReason = jfrogResearchSeverityReason;
    }
}
