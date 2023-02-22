package com.jfrog.ide.common.nodes;

import com.jfrog.ide.common.nodes.subentities.Severity;

public class ApplicableIssueNode extends IssueNode implements SubtitledTreeNode {
    private String name;
    private String reason;
    private String lineSnippet;
    private String scannerSearchTarget;
    private int rowStart;
    private int colStart;
    private int rowEnd;
    private int colEnd;
    private String filePath;
    private VulnerabilityNode issue;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    private ApplicableIssueNode() {
    }

    public ApplicableIssueNode(String name, int rowStart, int colStart, int rowEnd, int colEnd, String filePath, String reason, String lineSnippet, String scannerSearchTarget, VulnerabilityNode issue) {
        this.name = name;
        this.rowStart = rowStart;
        this.colStart = colStart;
        this.rowEnd = rowEnd;
        this.colEnd = colEnd;
        this.filePath = filePath;
        this.reason = reason;
        this.lineSnippet = lineSnippet;
        this.scannerSearchTarget = scannerSearchTarget;
        this.issue = issue;
    }

    public Severity getSeverity() {
        return issue.getSeverity();
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public String getSubtitle() {
        return "row: " + (rowStart + 1) + " col: " + colStart;
    }

    @Override
    public String getIcon() {
        return getSeverity().getIconName();
    }

    public VulnerabilityNode getIssue() {
        return issue;
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

    public String getScannerSearchTarget() {
        return scannerSearchTarget;
    }
}
