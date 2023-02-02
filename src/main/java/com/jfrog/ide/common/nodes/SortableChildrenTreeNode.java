package com.jfrog.ide.common.nodes;

import org.apache.commons.collections.CollectionUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

public class SortableChildrenTreeNode extends DefaultMutableTreeNode {
    public void sortChildren() {
        if (CollectionUtils.isNotEmpty(children)) {
            children.sort(Comparator.comparing(treeNode -> ((Comparable) treeNode)));
            children.stream()
                    .filter(treeNode -> treeNode instanceof SortableChildrenTreeNode)
                    .forEach(treeNode -> ((SortableChildrenTreeNode) treeNode).sortChildren());
        }
    }
}
