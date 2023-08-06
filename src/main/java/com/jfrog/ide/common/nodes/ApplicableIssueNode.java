package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import lombok.Getter;

import java.util.Objects;

@Getter
public class ApplicableIssueNode extends FileIssueNode {
    @JsonProperty()
    private String scannerSearchTarget;
    @JsonProperty()
    @JsonIdentityReference(alwaysAsId = true)
    private VulnerabilityNode issue;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    private ApplicableIssueNode() {
    }

    public ApplicableIssueNode(String name, int rowStart, int colStart, int rowEnd, int colEnd, String filePath, String reason, String lineSnippet, String scannerSearchTarget, VulnerabilityNode issue, String ruleID) {
        super(name, filePath, rowStart, colStart, rowEnd, colEnd, reason, lineSnippet, SourceCodeScanType.CONTEXTUAL, issue.getSeverity(), ruleID);
        this.scannerSearchTarget = scannerSearchTarget;
        this.issue = issue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicableIssueNode that = (ApplicableIssueNode) o;
        return super.equals(o) && Objects.equals(scannerSearchTarget, that.scannerSearchTarget) && Objects.equals(issue, that.issue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scannerSearchTarget, issue);
    }
}
