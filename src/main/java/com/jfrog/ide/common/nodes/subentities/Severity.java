package com.jfrog.ide.common.nodes.subentities;

/**
 * @author yahavi
 */
public enum Severity {
    Normal("Scanned - No Issues", "normal"),
    Pending("Pending Scan", "pending"),
    UnknownNotApplic("Unknown Not Applicable", "unknownnotapplic"),
    LowNotApplic("Low Not Applicable", "lownotapplic"),
    MediumNotApplic("Medium Not Applicable", "mediumnotapplic"),
    HighNotApplic("High Not Applicable", "highnotapplic"),
    CriticalNotApplic("Critical Not Applicable", "criticalnotapplic"),
    Unknown("Unknown", "unknown"),
    Information("Information", "information"),
    Low("Low", "low"),
    Medium("Medium", "medium"),
    High("High", "high"),
    Critical("Critical", "critical");

    private final String severityName;
    private final String iconName;

    Severity(String severityName, String iconName) {
        this.severityName = severityName;
        this.iconName = iconName;
    }

    public String getSeverityName() {
        return this.severityName;
    }

    public String getIconName() {
        return iconName;
    }

    public boolean isHigherThan(Severity other) {
        return this.ordinal() > other.ordinal();
    }

    public static Severity fromString(String inputSeverity) {
        for (Severity severity : Severity.values()) {
            if (severity.getSeverityName().equals(inputSeverity)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Severity " + inputSeverity + " doesn't exist");
    }

    public static Severity getNotApplicableSeverity(Severity severity) {
        switch (severity) {
            case Low:
                return LowNotApplic;
            case Medium:
                return MediumNotApplic;
            case High:
                return HighNotApplic;
            case Critical:
                return CriticalNotApplic;
            case Unknown:
            default:
                return UnknownNotApplic;
        }
    }
}