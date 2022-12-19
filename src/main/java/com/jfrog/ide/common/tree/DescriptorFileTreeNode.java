package com.jfrog.ide.common.tree;

import java.util.Collection;

// TODO: used?
public class DescriptorFileTreeNode extends FileTreeNode {
    // TODO: consider if neeeded. if not, consider if this class is needed, and also if FileTreeNode is needed.

    public DescriptorFileTreeNode(String filePath) {
        super(filePath);
    }

    public void addDependency(Artifact dependency) {
        Artifact clonedDep = (Artifact) dependency.clone();
        add(clonedDep);
        if (clonedDep.getTopSeverity().isHigherThan(topSeverity)) {
            topSeverity = clonedDep.getTopSeverity();
        }
        sortChildren();
    }

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
        children.sort((treeNode1, treeNode2) -> ((Artifact) treeNode2).getTopSeverity().ordinal() - ((Artifact) treeNode1).getTopSeverity().ordinal());
    }

    @Override
    public String getIcon() {
        return topSeverity.getIconName();
    }
}
