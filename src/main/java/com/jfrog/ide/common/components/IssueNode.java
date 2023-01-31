package com.jfrog.ide.common.components;

import javax.swing.tree.TreeNode;

public abstract class IssueNode extends ComparableSeverityTreeNode implements SubtitledTreeNode {

    public String getIcon() {
        return getSeverity().getIconName();
    }

    public DependencyNode getParentArtifact() {
        TreeNode parent = getParent();
        if (!(parent instanceof DependencyNode)) {
            return null;
        }
        return (DependencyNode) parent;
    }
}
