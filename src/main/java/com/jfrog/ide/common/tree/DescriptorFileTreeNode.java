package com.jfrog.ide.common.tree;

import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.Collections;

@SuppressWarnings("unused")
public class DescriptorFileTreeNode extends FileTreeNode {
    public DescriptorFileTreeNode(String filePath) {
        super(filePath);
    }

    /**
     * Adds a dependency as a child of the descriptor file.
     * Each dependency can have only one parent.
     *
     * @param dependency
     */
    public void addDependency(Artifact dependency) {
        addDependencies(Collections.singletonList(dependency));
    }

    /**
     * Adds dependencies as children of the descriptor file.
     * Each dependency can have only one parent.
     *
     * @param dependencies
     */
    public void addDependencies(Collection<Artifact> dependencies) {
        for (Artifact dependency : dependencies) {
            add(dependency);
            if (dependency.getTopSeverity().isHigherThan(topSeverity)) {
                topSeverity = dependency.getTopSeverity();
            }
        }
        sortChildren();
    }

    private void sortChildren() {
        if (CollectionUtils.isNotEmpty(children)) {
            children.sort((treeNode1, treeNode2) -> ((Artifact) treeNode2).getTopSeverity().ordinal() - ((Artifact) treeNode1).getTopSeverity().ordinal());
        }
    }

    @Override
    public String getIcon() {
        return topSeverity.getIconName();
    }
}
