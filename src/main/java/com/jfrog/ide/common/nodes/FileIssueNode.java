package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.Severity;

public class FileIssueNode extends IssueNode implements SubtitledTreeNode {
    @JsonProperty()
    private String name;
    @JsonProperty()
    private String reason;
    @JsonProperty()
    private String lineSnippet;
    @JsonProperty()
    private int rowStart;
    @JsonProperty()
    private int colStart;
    @JsonProperty()
    private int rowEnd;
    @JsonProperty()
    private int colEnd;
    @JsonProperty()
    private String filePath;
    @JsonProperty()
    private Severity severity;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    protected FileIssueNode() {
    }

    public FileIssueNode(String name, String filePath, int rowStart, int colStart, int rowEnd, int colEnd, String reason, String lineSnippet, String reportType) {
        this.name = name;
        this.filePath = filePath;
        this.rowStart = rowStart;
        this.colStart = colStart;
        this.rowEnd = rowEnd;
        this.colEnd = colEnd;
        this.reason = reason;
        this.lineSnippet = lineSnippet;
        this.reporterType = reportType;
    }

    @SuppressWarnings("unused")
    public String getFilePath() {
        return filePath;
    }

    @SuppressWarnings("unused")
    public int getRowStart() {
        return rowStart;
    }

    @SuppressWarnings("unused")
    public int getColStart() {
        return colStart;
    }

    @SuppressWarnings("unused")
    public int getRowEnd() {
        return rowEnd;
    }

    @SuppressWarnings("unused")
    public int getColEnd() {
        return colEnd;
    }

    public String getReason() {
        return reason;
    }

    public String getLineSnippet() {
        return lineSnippet;
    }

    public String getReporterType() {
        return reporterType;
    }

    @Override
    // The indexes ranges start form 0, for user readability convert the range to start from 1.
    public String getSubtitle() {
        return "row: " + (rowStart + 1) + " col: " + (colStart + 1);
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public String getIcon() {
        return getSeverity().getIconName();
    }
}
