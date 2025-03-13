package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SourceCodeScanType {
    CONTEXTUAL("analyze-applicability"),
    SECRETS("JFrog Secrets scanner"),
    IAC("JFrog Terraform scanner"),
    SAST("JFrog SAST"),
    SCA("JFrog Xray Scanner");

    private final String param;

    @JsonCreator
    SourceCodeScanType(String param) {
        this.param = param;
    }

    @JsonValue
    public String getParam() {
        return param;
    }

    public static SourceCodeScanType fromParam(String param) {
        for (SourceCodeScanType type : SourceCodeScanType.values()) {
            if (type.getParam().equals(param)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant with param " + param);
    }
}
