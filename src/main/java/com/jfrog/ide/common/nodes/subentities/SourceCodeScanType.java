package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum SourceCodeScanType {
    CONTEXTUAL("analyze-applicability"),
    SECRETS("secrets-scan"),
    IAC("iac-scan-modules"),
    SAST("sast"),
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
        if (param == null) {
            throw new IllegalArgumentException("No enum constant with param null");
        }
        for (SourceCodeScanType type : SourceCodeScanType.values()) {
            if (type.getScannerName().equals(param)) {
                return type;
            }
        }
        String normalized = param.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "jfrog sast" -> SAST;
            case "jfrog secrets scanner" -> SECRETS;
            case "jfrog terraform scanner" -> IAC;
            default -> throw new IllegalArgumentException("No enum constant with param " + param);
        };
    }
}
