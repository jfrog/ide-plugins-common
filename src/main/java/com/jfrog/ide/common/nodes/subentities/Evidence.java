package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Evidence {
    @JsonProperty()
    private String reason;
    @JsonProperty()
    private String filePathEvidences;
    @JsonProperty()
    private String codeEvidence;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    public Evidence() {

    }

    public Evidence(String reason, String filePathEvidences, String codeEvidence) {
        this.reason = reason;
        this.filePathEvidences = filePathEvidences;
        this.codeEvidence = codeEvidence;
    }

    @SuppressWarnings("unused")
    public String getReason() {
        return reason;
    }

    @SuppressWarnings("unused")
    public String getFilePathEvidence() {
        return filePathEvidences;
    }

    @SuppressWarnings("unused")
    public String getCodeEvidence() {
        return codeEvidence;
    }

    @SuppressWarnings("unused")
    public void setReason(String reason) {
        this.reason = reason;
    }

    @SuppressWarnings("unused")
    public void setFilePathEvidence(String filePathEvidences) {
        this.filePathEvidences = filePathEvidences;
    }

    @SuppressWarnings("unused")
    public void setCodeEvidence(String codeEvidence) {
        this.codeEvidence = codeEvidence;
    }
}
