package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SourceCodeScanType {
    CONTEXTUAL("analyze-applicability", "JFrog Contextual Scan"),
    SECRETS("JFrog Secrets scanner", "Potential Secret"),
    IAC("JFrog Terraform scanner", "Infrastructure as Code Vulnerability"),
    SAST("JFrog SAST", "SAST Vulnerability"),
    SCA("JFrog Xray Scanner", "SCA Vulnerability");

    private final String scannerName;
    private final String scannerIssueTitle;

    @JsonCreator
    SourceCodeScanType(String scannerName, String scannerIssueTitle) {
        this.scannerName = scannerName;
        this.scannerIssueTitle = scannerIssueTitle;
    }

    @JsonValue
    public String getScannerName() {
        return scannerName;
    }

    @JsonValue
    public String getScannerIssueTitle() {
        return scannerIssueTitle;
    }

    public static SourceCodeScanType fromParam(String param) {
        for (SourceCodeScanType type : SourceCodeScanType.values()) {
            if (type.getScannerName().equals(param)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant with param " + param);
    }
}
