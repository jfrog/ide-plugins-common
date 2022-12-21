package com.jfrog.ide.common.tree;

import javax.swing.tree.DefaultMutableTreeNode;

// TODO: consider removing this class
public abstract class IssueOrLicense extends DefaultMutableTreeNode implements SubtitledTreeNode {
    public abstract Severity getSeverity();

    public String getIcon() {
        return getSeverity().getIconName();
    }

    public Artifact getParentArtifact() {
        Object parent = getParent();
        if (parent == null || !(parent instanceof Artifact)) {
            return null;
        }
        return (Artifact) parent;
    }
}
