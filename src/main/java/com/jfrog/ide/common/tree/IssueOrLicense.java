package com.jfrog.ide.common.tree;

import javax.swing.tree.DefaultMutableTreeNode;

// TODO: consider removing this class
public abstract class IssueOrLicense extends DefaultMutableTreeNode implements SubtitledTreeNode {
    public abstract Severity getSeverity();

    public String getIcon() {
        return getSeverity().getIconName();
    }
}
