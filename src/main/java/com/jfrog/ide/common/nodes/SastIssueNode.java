package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.FindingInfo;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import lombok.Getter;

import java.util.Arrays;

@Getter
public class SastIssueNode extends FileIssueNode {
    @JsonProperty()
    private FindingInfo[][] codeFlows;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    private SastIssueNode() {
    }

    @SuppressWarnings("unused")
    public SastIssueNode(String name, String filePath, int rowStart, int colStart, int rowEnd, int colEnd, String reason, String lineSnippet, FindingInfo[][] codeFlows, Severity severity, String ruleID) {
        super(name, filePath, rowStart, colStart, rowEnd, colEnd, reason, lineSnippet, SourceCodeScanType.SAST, severity, ruleID);
        this.codeFlows = codeFlows;
    }

    // Constructor for building SastIssueNode with fullDescription param
    public SastIssueNode(String name, String filePath, int rowStart, int colStart, int rowEnd, int colEnd, String reason, String lineSnippet, FindingInfo[][] codeFlows, Severity severity, String ruleID, String fullDescription) {
        super(name, filePath, rowStart, colStart, rowEnd, colEnd, reason, lineSnippet, SourceCodeScanType.SAST, severity, ruleID, fullDescription);
        this.codeFlows = codeFlows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SastIssueNode that = (SastIssueNode) o;
        return Arrays.deepEquals(codeFlows, that.codeFlows);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.deepHashCode(codeFlows);
        return result;
    }
}
