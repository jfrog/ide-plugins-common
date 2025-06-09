package com.jfrog.ide.common.webview;

import lombok.Getter;

@Getter
public class Cve {
    private final String id;
    private final String cvssV2Score;
    private final String cvssV2Vector;
    private final String cvssV3Score;
    private final String cvssV3Vector;
    private final ApplicableDetails applicableData;

    public Cve(String id, String cvssV2Score, String cvssV2Vector, String cvssV3Score, String cvssV3Vector, ApplicableDetails applicableData) {
        this.id = id;
        this.cvssV2Score = cvssV2Score;
        this.cvssV2Vector = cvssV2Vector;
        this.cvssV3Score = cvssV3Score;
        this.cvssV3Vector = cvssV3Vector;
        this.applicableData = applicableData;
    }
}
