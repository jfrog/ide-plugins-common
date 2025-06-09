package com.jfrog.ide.common.webview;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
public class SastIssuePage extends IssuePage {
    @JsonProperty("analysisStep")
    private Location[] analysisSteps;
    private String ruleId;

    public SastIssuePage() {
    }

    public SastIssuePage(IssuePage issuePage) {
        super(issuePage);
    }

    public SastIssuePage setAnalysisSteps(Location[] analysisSteps) {
        this.analysisSteps = analysisSteps;
        return this;
    }

    public SastIssuePage setRuleID(String ruleID) {
        this.ruleId = ruleID;
        return this;
    }
}
