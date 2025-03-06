package com.jfrog.ide.common.parse;

import lombok.Getter;

@Getter
public enum Applicability {
    APPLICABLE("Applicable"),
    NOT_APPLICABLE("Not Applicable"),
    NOT_DETERMINED("Not Determined");


    private final String applicability;

    Applicability(String applicability) {
        this.applicability = applicability;
    }

    public static Applicability fromSarif(String applicability) {
        return switch (applicability) {
            case "Applicable" -> Applicability.APPLICABLE;
            case "Not Applicable" -> Applicability.NOT_APPLICABLE;
            default -> Applicability.NOT_DETERMINED;
        };
    }

}


