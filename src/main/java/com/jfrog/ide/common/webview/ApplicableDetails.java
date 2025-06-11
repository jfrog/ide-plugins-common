package com.jfrog.ide.common.webview;

import lombok.Getter;

@Getter
public class ApplicableDetails {
    private final String applicability;
    private final Evidence[] evidence;
    private final String searchTarget;

    public ApplicableDetails(String applicability, Evidence[] evidence, String searchTarget) {
        this.applicability = applicability;
        this.evidence = evidence;
        this.searchTarget = searchTarget;
    }
}
