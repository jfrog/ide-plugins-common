package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.FindingInfo;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.common.nodes.subentities.Severity;
import lombok.Getter;

import java.util.Objects;

@Getter
public class FileIssueNode extends IssueNode implements SubtitledTreeNode {
    @JsonProperty()
    private String title;
    @JsonProperty()
    private String reason;
    @JsonProperty()
    private FindingInfo findingInfo;
    @JsonProperty()
    private Severity severity;
    @JsonProperty()
    private SourceCodeScanType reporterType;
    @JsonProperty()
    private String ruleID;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    protected FileIssueNode() {
    }

    public FileIssueNode(String title, String filePath, int rowStart, int colStart, int rowEnd, int colEnd, String reason, String lineSnippet, SourceCodeScanType reportType, Severity severity, String ruleID) {
        this.title = title;
        this.findingInfo = new FindingInfo(filePath, rowStart, colStart, rowEnd, colEnd, lineSnippet);
        this.reason = reason;
        this.reporterType = reportType;
        this.severity = severity;
        this.ruleID = ruleID;
    }
    public FileIssueNode(String title, String reason, SourceCodeScanType reportType, Severity severity, String ruleID){
        this.title = title;
        this.reason = reason;
        this.reporterType = reportType;
        this.severity = severity;
        this.ruleID = ruleID;
        this.findingInfo = new FindingInfo();
    }

    public String getFilePath() {
        return findingInfo.getFilePath();
    }

    public int getRowStart() {
        return findingInfo.getRowStart();
    }

    public int getColStart() {
        return findingInfo.getColStart();
    }

    @SuppressWarnings("unused")
    public int getRowEnd() {
        return findingInfo.getRowEnd();
    }

    @SuppressWarnings("unused")
    public int getColEnd() {
        return findingInfo.getColEnd();
    }

    public String getLineSnippet() {
        return findingInfo.getLineSnippet();
    }

    @Override
    public String getSubtitle() {
        // Indexes are zero-based. To enhance user readability, the range is converted to start from 1.
        return "row: " + (getRowStart() + 1) + " col: " + (getColStart() + 1);
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getIcon() {
        return getSeverity().getIconName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileIssueNode that = (FileIssueNode) o;
        return Objects.equals(findingInfo, that.findingInfo) && Objects.equals(title, that.title)
                && Objects.equals(reason, that.reason) && severity == that.severity && reporterType == that.reporterType
                && Objects.equals(ruleID, that.ruleID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, reason, findingInfo, severity, reporterType, ruleID);
    }
}
