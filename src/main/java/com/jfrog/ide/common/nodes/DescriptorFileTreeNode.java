package com.jfrog.ide.common.nodes;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class DescriptorFileTreeNode extends FileTreeNode {
    public DescriptorFileTreeNode(String filePath) {
        super(filePath);
    }

    /**
     * Adds a dependency as a child of the descriptor file.
     * Each dependency can have only one parent.
     *
     * @param dependency dependency to add to the descriptor file
     */
    public void addDependency(DependencyNode dependency) {
        add(dependency);
        if (dependency.getSeverity().isHigherThan(topSeverity)) {
            topSeverity = dependency.getSeverity();
        }
    }

    public Collection<DependencyNode> getDependencies() {
        if (children == null) {
            return List.of();
        }
        return children.stream().filter(child -> child instanceof DependencyNode).map(child -> (DependencyNode) child).collect(Collectors.toList());
    }
}
