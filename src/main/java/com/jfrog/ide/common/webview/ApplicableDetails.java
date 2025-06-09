package com.jfrog.ide.common.webview;

import lombok.Getter;

@Getter
public class ApplicableDetails {
    private final boolean isApplicable;
    private final Evidence[] evidence;
    private final String searchTarget;

    public ApplicableDetails(boolean isApplicable, Evidence[] evidence, String searchTarget) {
        this.isApplicable = isApplicable;
        this.evidence = evidence;
        this.searchTarget = searchTarget;
    }
}
