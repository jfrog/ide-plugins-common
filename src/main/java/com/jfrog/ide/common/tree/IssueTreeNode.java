package com.jfrog.ide.common.tree;

import javax.swing.tree.DefaultMutableTreeNode;

public class IssueTreeNode extends DefaultMutableTreeNode implements SubtitledTreeNode {

    protected String name;
    protected int row;
    protected int col;
    protected Issue issue;

    public IssueTreeNode(String name, int row, int col, Issue issue) {
        this.name = name;
        this.row = row;
        this.col = col;
        this.issue = issue;

    }

    public Severity getSeverity() {
        return issue.getSeverity();
    }

    @Override
    public String getTitle() {
        return "<html><b>" + name + "</b></html>";
    }

    @Override
    public String getSubtitle() {
        return "row: " + row + " col: " + col;
    }

    @Override
    public String getIcon() {
        return getSeverity().getIconName();
    }

}
