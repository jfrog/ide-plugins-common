package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import lombok.Getter;

@Getter
public class ApplicableIssueNode extends FileIssueNode {
    @JsonProperty()
    private String scannerSearchTarget;
    @JsonIdentityReference(alwaysAsId = true)
    private VulnerabilityNode issue;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    private ApplicableIssueNode() {
    }

    public ApplicableIssueNode(String name, int rowStart, int colStart, int rowEnd, int colEnd, String filePath, String reason, String lineSnippet, String scannerSearchTarget, VulnerabilityNode issue) {
        super(name, filePath, rowStart, colStart, rowEnd, colEnd, reason, lineSnippet, SourceCodeScanType.CONTEXTUAL, issue.getSeverity());
        this.scannerSearchTarget = scannerSearchTarget;
        this.issue = issue;
    }
}
