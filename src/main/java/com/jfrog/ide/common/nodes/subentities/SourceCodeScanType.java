package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SourceCodeScanType {
    CONTEXTUAL("analyze-applicability"),
    SECRETS("secrets-scan"),
    IAC("iac-scan-modules");

    private final String param;

    @JsonCreator
    SourceCodeScanType(String param) {
        this.param = param;
    }

    @JsonValue
    public String getParam() {
        return param;
    }
}
