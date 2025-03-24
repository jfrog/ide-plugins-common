package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SourceCodeScanType {
    CONTEXTUAL("analyze-applicability"),
    SECRETS("JFrog Secrets scanner"),
    IAC("JFrog Terraform scanner"),
    SAST("JFrog SAST"),
    SCA("JFrog Xray Scanner");

    private final String scannerName;

    @JsonCreator
    SourceCodeScanType(String scannerName) {
        this.scannerName = scannerName;
    }

    @JsonValue
    public String getScannerName() {
        return scannerName;
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
