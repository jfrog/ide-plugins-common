package com.jfrog.ide.common.parse;

import lombok.Getter;

@Getter
public enum Applicability {
    APPLICABLE("applicable"),
    NOT_APPLICABLE("not_applicable"),
    UNDETERMINED("undetermined"),
    NOT_COVERED("not_covered"),
    MISSING_CONTEXT("missing_context");

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


