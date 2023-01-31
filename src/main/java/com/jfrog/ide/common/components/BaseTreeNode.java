package com.jfrog.ide.common.components;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

/**
 * The root node that is passed to the tree.
 * This node does not appear in the tree.
 * Its children appear as the upper level of the tree.
 */
public class BaseTreeNode extends DefaultMutableTreeNode {
    public void sortChildren() {
        children.sort(Comparator.comparing(treeNode -> ((ComparableSeverityTreeNode) treeNode)));
    }
}
