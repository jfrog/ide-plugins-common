package com.jfrog.ide.common.tree;

public class ApplicableIssue extends VulnerabilityOrViolation implements SubtitledTreeNode {

    protected String name;
    protected int row;
    protected int col;
    protected String filePath;
    protected Issue issue;

    public ApplicableIssue(String name, int row, int col, String filePath, Issue issue) {
        this.name = name;
        this.row = row;
        this.col = col;
        this.issue = issue;
        this.filePath = filePath;

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

    public Issue getIssue() {
        return issue;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}
