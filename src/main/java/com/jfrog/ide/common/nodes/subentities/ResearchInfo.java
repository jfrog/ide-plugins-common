package com.jfrog.ide.common.nodes.subentities;

public class ResearchInfo {
    private final Severity severity;
    private final String shortDescription;
    private final String fullDescription;
    private final String remediation;
    private final SeverityReason[] severityReasons;

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
