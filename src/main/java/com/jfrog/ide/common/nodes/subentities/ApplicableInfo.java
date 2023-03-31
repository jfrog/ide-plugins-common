package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class ApplicableInfo {
    @JsonProperty()
    private boolean isApplicable;
    @JsonProperty()
    private String searchTarget;
    @JsonProperty()
    private List<String> reasons = new ArrayList<>();
    @JsonProperty()
    private List<String> filePathEvidences = new ArrayList<>();
    @JsonProperty()
    private List<String> codeEvidences = new ArrayList<>();

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    public ApplicableInfo() {

    }

    public ApplicableInfo(boolean isApplicable) {
        this.isApplicable = isApplicable;
    }

    public ApplicableInfo(boolean isApplicable, String searchTarget, String reason, String filePathEvidence, String codeEvidence) {
        this(isApplicable);
        this.searchTarget = searchTarget;
        this.reasons.add(reason);
        this.filePathEvidences.add(filePathEvidence);
        this.codeEvidences.add(codeEvidence);
    }

    public String getSearchTarget() {
        return searchTarget;
    }

    public boolean isApplicable() {
        return isApplicable;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public List<String> getFilePathEvidences() {
        return filePathEvidences;
    }

    public List<String> getCodeEvidences() {
        return codeEvidences;
    }

    public void addInfo(String reason, String filePath, String codeEvidence) {
        this.isApplicable = true;
        this.reasons.add(reason);
        this.filePathEvidences.add(filePath);
        this.codeEvidences.add(codeEvidence);

    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public void setFilePathEvidences(List<String> filePathEvidences) {
        this.filePathEvidences = filePathEvidences;
    }

    public void setCodeEvidences(List<String> codeEvidences) {
        this.codeEvidences = codeEvidences;
    }
}