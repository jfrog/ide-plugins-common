package com.jfrog.ide.common.tree;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class ComparableSeverityTreeNode extends DefaultMutableTreeNode implements Comparable<ComparableSeverityTreeNode> {
    abstract Severity getSeverity();

    public int compareTo(ComparableSeverityTreeNode other) {
        return other.getSeverity().ordinal() - this.getSeverity().ordinal();
    }

}
