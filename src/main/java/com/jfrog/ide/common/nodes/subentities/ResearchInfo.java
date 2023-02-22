package com.jfrog.ide.common.nodes.subentities;

import java.util.List;

public class ResearchInfo {
    private Severity severity;
    private String shortDescription;
    private String fullDescription;
    private String remediation;
    private List<SeverityReason> severityReasons;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    private ResearchInfo() {
    }

    public ResearchInfo(Severity severity, String shortDescription, String fullDescription, String remediation, List<SeverityReason> severityReasons) {
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
    public List<SeverityReason> getSeverityReasons() {
        return severityReasons;
    }
}
