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
    public void addDependency(DependencyNode dependency) {
        addDependencies(Collections.singletonList(dependency));
    }

    public void addDependency(ApplicableIssue issue) {
        add(issue);
        if (issue.getSeverity().isHigherThan(topSeverity)) {
            topSeverity = issue.getSeverity();
        }
    }


    /**
     * Adds dependencies as children of the descriptor file.
     * Each dependency can have only one parent.
     *
     * @param dependencies
     */
    public void addDependencies(Collection<DependencyNode> dependencies) {
        for (DependencyNode dependency : dependencies) {
            add(dependency);
            if (dependency.getTopSeverity().isHigherThan(topSeverity)) {
                topSeverity = dependency.getTopSeverity();
            }
        }
        sortChildren();
    }

    private void sortChildren() {
        if (CollectionUtils.isNotEmpty(children)) {
            children.sort((treeNode1, treeNode2) -> ((DependencyNode) treeNode2).getTopSeverity().ordinal() - ((DependencyNode) treeNode1).getTopSeverity().ordinal());
        }
    }

}
