package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.FindingInfo;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.common.nodes.subentities.Severity;

import java.util.Objects;

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

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    protected FileIssueNode() {
    }

    public FileIssueNode(String title, String filePath, int rowStart, int colStart, int rowEnd, int colEnd, String reason, String lineSnippet, SourceCodeScanType reportType, Severity severity) {
        this.title = title;
        this.findingInfo = new FindingInfo(filePath, rowStart, colStart, rowEnd, colEnd, lineSnippet);
        this.reason = reason;
        this.reporterType = reportType;
        this.severity = severity;
    }

    @SuppressWarnings("unused")
    public String getFilePath() {
        return findingInfo.getFilePath();
    }

    @SuppressWarnings("unused")
    public int getRowStart() {
        return findingInfo.getRowStart();
    }

    @SuppressWarnings("unused")
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

    public String getReason() {
        return reason;
    }

    @SuppressWarnings("unused")
    public SourceCodeScanType getReporterType() {
        return reporterType;
    }

    @Override
    public String getSubtitle() {
        // The indexes ranges start form 0, for user readability convert the range to start from 1.
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
        return rowStart == that.rowStart && colStart == that.colStart && rowEnd == that.rowEnd && colEnd == that.colEnd && Objects.equals(title, that.title) && Objects.equals(reason, that.reason) && Objects.equals(lineSnippet, that.lineSnippet) && Objects.equals(filePath, that.filePath) && severity == that.severity && reporterType == that.reporterType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, reason, lineSnippet, rowStart, colStart, rowEnd, colEnd, filePath, severity, reporterType);
    }
}
