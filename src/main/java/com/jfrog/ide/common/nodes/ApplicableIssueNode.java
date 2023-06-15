package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApplicableIssueNode extends FileIssueNode {
    @JsonProperty()
    private String scannerSearchTarget;
    @JsonProperty()
    private VulnerabilityNode issue;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    private ApplicableIssueNode() {
    }

    public ApplicableIssueNode(String name, int rowStart, int colStart, int rowEnd, int colEnd, String filePath, String reason, String lineSnippet, String scannerSearchTarget, VulnerabilityNode issue) {
        super(name, filePath, rowStart, colStart, rowEnd, colEnd, reason, lineSnippet, "DEPENDENCY");
        this.scannerSearchTarget = scannerSearchTarget;
        this.issue = issue;
    }

    public VulnerabilityNode getIssue() {
        return issue;
    }


    public String getScannerSearchTarget() {
        return scannerSearchTarget;
    }
}
