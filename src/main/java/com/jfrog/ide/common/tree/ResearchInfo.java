package com.jfrog.ide.common.tree;

public class ResearchInfo {
    private Severity severity;
    private String shortDescription;
    private String fullDescription;
    private String remediation;
    private SeverityReason[] severityReasons;

    public ResearchInfo(Severity severity, String shortDescription, String fullDescription, String remediation, SeverityReason[] severityReasons) {
        this.severity = severity;
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.remediation = remediation;
        this.severityReasons = severityReasons;
    }

    @SuppressWarnings("unused")
    public Severity getSeverity() {
        return severity;
    }

    @SuppressWarnings("unused")
    public String getShortDescription() {
        return shortDescription;
    }

    @SuppressWarnings("unused")
    public String getFullDescription() {
        return fullDescription;
    }

    @SuppressWarnings("unused")
    public String getRemediation() {
        return remediation;
    }

    @SuppressWarnings("unused")
    public SeverityReason[] getSeverityReasons() {
        return severityReasons;
    }
}
