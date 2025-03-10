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
            case "Applicable" -> Applicability.APPLICABLE;
            case "Not Applicable" -> Applicability.NOT_APPLICABLE;
            case "Undetermined" -> Applicability.UNDETERMINED;
            case "Not Covered" -> Applicability.NOT_COVERED;
            case "Missing Context" -> Applicability.MISSING_CONTEXT;
            default -> throw new IllegalArgumentException("No applicability constant with value " + value);
        };
    }

}


