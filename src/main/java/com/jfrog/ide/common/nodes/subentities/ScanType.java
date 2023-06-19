package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ScanType {
    CONTEXTUAL("analyze-applicability"),
    SECRETS("secrets-scan"),
    IAC("iac-scan-modules");
    private final String name;

    @JsonCreator
    ScanType(String name) {
        this.name = name;
    }

    @JsonValue
    public String toString() {
        return this.name;
    }
}
