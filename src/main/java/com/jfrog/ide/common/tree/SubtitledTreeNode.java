package com.jfrog.ide.common.tree;

import javax.swing.tree.TreeNode;

public interface SubtitledTreeNode extends TreeNode {
    String getTitle();
    String getSubtitle();
    String getIcon();
}
