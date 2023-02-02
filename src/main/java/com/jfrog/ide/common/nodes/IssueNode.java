package com.jfrog.ide.common.nodes;

import com.jfrog.ide.common.nodes.subentities.Severity;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public abstract class IssueNode extends DefaultMutableTreeNode implements SubtitledTreeNode, Comparable<IssueNode> {
    public abstract Severity getSeverity();

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

    @Override
    public int compareTo(IssueNode o) {
        return o.getSeverity().ordinal() - this.getSeverity().ordinal();
    }
}
