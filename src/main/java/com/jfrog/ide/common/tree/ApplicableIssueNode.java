package com.jfrog.ide.common.tree;

public class ApplicableIssueNode extends VulnerabilityOrViolationNode implements SubtitledTreeNode {

    protected String name;
    protected String reason;
    protected String lineSnippet;

    protected int row;
    protected int col;
    protected String filePath;
    protected IssueNode issue;

    public ApplicableIssueNode(String name, int row, int col, String filePath, String reason, String lineSnippet, IssueNode issue) {
        this.name = name;
        this.row = row;
        this.col = col;
        this.issue = issue;
        this.filePath = filePath;
        this.reason = reason;
        this.lineSnippet = lineSnippet;
    }

    @Override
    public Severity getSeverity() {
        return issue.getSeverity();
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public String getSubtitle() {
        return "row: " + (row + 1) + " col: " + col;
    }

    @Override
    public String getIcon() {
        return getSeverity().getIconName();
    }

    public IssueNode getIssue() {
        return issue;
    }

    @SuppressWarnings("unused")
    public String getFilePath() {
        return filePath;
    }

    @SuppressWarnings("unused")
    public int getRow() {
        return row;
    }

    @SuppressWarnings("unused")
    public int getCol() {
        return col;
    }

    public String getReason() {
        return reason;
    }

    public String getLineSnippet() {
        return lineSnippet;
    }

}
