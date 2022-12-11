package com.jfrog.ide.common.tree;

import javax.swing.tree.DefaultMutableTreeNode;

public class BaseTreeNode extends DefaultMutableTreeNode {
    public void sortChildren() {
        children.sort((treeNode1, treeNode2) -> ((FileTreeNode) treeNode2).getTopSeverity().ordinal() - ((FileTreeNode) treeNode1).getTopSeverity().ordinal());
    }
}
