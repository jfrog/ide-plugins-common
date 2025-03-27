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
    private String ruleId;
    @JsonProperty()
    private String fullDescription;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    protected FileIssueNode() {
    }

    public FileIssueNode(String title, String filePath, int rowStart, int colStart, int rowEnd, int colEnd, String reason, String lineSnippet, SourceCodeScanType reportType, Severity severity, String ruleId) {
        this.title = title;
        this.findingInfo = new FindingInfo(filePath, rowStart, colStart, rowEnd, colEnd, lineSnippet);
        this.reason = reason;
        this.reporterType = reportType;
        this.severity = severity;
        this.ruleId = ruleId;
    }

    // Constructor for building FileIssueNode with fullDescription param
    public FileIssueNode(String title, String filePath, int rowStart, int colStart, int rowEnd, int colEnd, String reason, String lineSnippet, SourceCodeScanType reportType, Severity severity, String ruleId, String fullDescription) {
        this(title, filePath, rowStart, colStart, rowEnd, colEnd, reason, lineSnippet, reportType, severity, ruleId);
        this.fullDescription = fullDescription;
    }

    // TODO: Temporary constructor for ScaIssueNode that currently not passing location info. When corresponding functionality is implemented, this constructor should be removed.
    public FileIssueNode(String title, String reason, SourceCodeScanType reportType, Severity severity, String ruleId, String fullDescription){
        this.title = title;
        this.reason = reason;
        this.reporterType = reportType;
        this.severity = severity;
        this.ruleId = ruleId;
        this.fullDescription = fullDescription;
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
                && Objects.equals(ruleId, that.ruleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, reason, findingInfo, severity, reporterType, ruleId);
    }

    @Override
    public String toString() {
        return title;
    }
}
