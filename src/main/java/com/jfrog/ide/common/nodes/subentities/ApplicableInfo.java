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
    private List<Evidence> evidences = new ArrayList<>();

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
        this.evidences.add(new Evidence(reason, filePathEvidence, codeEvidence));
    }

    public String getSearchTarget() {
        return searchTarget;
    }

    public boolean isApplicable() {
        return isApplicable;
    }

    public void addInfo(String reason, String filePath, String codeEvidence) {
        this.isApplicable = true;
        evidences.add(new Evidence(reason, filePath, codeEvidence));
    }

    public List<Evidence> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<Evidence> evidences) {
        this.evidences = evidences;
    }

}