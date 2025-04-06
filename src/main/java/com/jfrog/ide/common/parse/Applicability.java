package com.jfrog.ide.common.parse;

import lombok.Getter;

@Getter
public enum Applicability {
    APPLICABLE("Applicable"),
    NOT_APPLICABLE("Not Applicable"),
    UNDETERMINED("Undetermined"),
    NOT_COVERED("Not Covered"),
    MISSING_CONTEXT("Missing Context");

    private final String value;

    Applicability(String value) {
        this.value = value;
    }

    public static Applicability fromSarif(String value) {
        return switch (value) {
            case "applicable" -> Applicability.APPLICABLE;
            case "not applicable" -> Applicability.NOT_APPLICABLE;
            case "undetermined" -> Applicability.UNDETERMINED;
            case "not covered" -> Applicability.NOT_COVERED;
            case "missing context" -> Applicability.MISSING_CONTEXT;
            default -> throw new IllegalArgumentException("'%s' applicability status not supported" + value);
        };
    }
}


