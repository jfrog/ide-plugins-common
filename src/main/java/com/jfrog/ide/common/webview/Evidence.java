package com.jfrog.ide.common.webview;

import lombok.Getter;

@Getter
public class Evidence {
    private final String reason;
    private final String filePathEvidence;
    private final String codeEvidence;

    public Evidence(String reason, String filePathEvidence, String codeEvidence) {
        this.reason = reason;
        this.filePathEvidence = filePathEvidence;
        this.codeEvidence = codeEvidence;
    }
}
