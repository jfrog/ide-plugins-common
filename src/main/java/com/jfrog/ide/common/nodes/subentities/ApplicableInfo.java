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
    private final List<String> reasons = new ArrayList<>();
    @JsonProperty()
    private final List<String> filePathEvidences = new ArrayList<>();
    @JsonProperty()
    private final List<String> codeEvidences = new ArrayList<>();

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

    public String[] getReasons() {
        return reasons.toArray(new String[]{});
    }

    public String[] getFilePathEvidences() {
        return filePathEvidences.toArray(new String[]{});
    }

    public String[] getCodeEvidences() {
        return codeEvidences.toArray(new String[]{});
    }

    public void addInfo(String reason, String filePath, String codeEvidence) {
        this.isApplicable = true;
        this.reasons.add(reason);
        this.filePathEvidences.add(filePath);
        this.codeEvidences.add(codeEvidence);

    }
}