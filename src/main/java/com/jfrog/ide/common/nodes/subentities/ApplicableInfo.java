package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class ApplicableInfo {
    @JsonProperty()
    private boolean isApplicable;
    @JsonProperty()
    private String searchTarget;
    @JsonProperty()
    @Setter
    private List<Evidence> evidences = new ArrayList<>();

    public ApplicableInfo(boolean isApplicable) {
        this.isApplicable = isApplicable;
    }

    public ApplicableInfo(boolean isApplicable, String searchTarget, String reason, String filePathEvidence, String codeEvidence) {
        this(isApplicable);
        this.searchTarget = searchTarget;
        this.evidences.add(new Evidence(reason, filePathEvidence, codeEvidence));
    }

    public void addInfo(String reason, String filePath, String codeEvidence) {
        this.isApplicable = true;
        evidences.add(new Evidence(reason, filePath, codeEvidence));
    }

}