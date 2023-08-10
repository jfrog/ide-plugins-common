package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * @author yahavi
 */
@Getter
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

    public boolean isHigherThan(Severity other) {
        return this.ordinal() > other.ordinal();
    }

    @JsonCreator
    public static Severity fromString(String inputSeverity) {
        return Severity.valueOf(inputSeverity);
    }

    public static Severity fromSarif(String level) {
        return switch (level) {
            case "error" -> Severity.High;
            case "note" -> Severity.Low;
            case "none" -> Severity.Unknown;
            default -> Severity.Medium;
        };
    }

    public static Severity getNotApplicableSeverity(Severity severity) {
        return switch (severity) {
            case Low -> LowNotApplic;
            case Medium -> MediumNotApplic;
            case High -> HighNotApplic;
            case Critical -> CriticalNotApplic;
            default -> UnknownNotApplic;
        };
    }
}