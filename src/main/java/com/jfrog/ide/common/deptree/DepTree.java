package com.jfrog.ide.common.deptree;

import java.util.List;
import java.util.Map;

/**
 * Represents a dependency tree.
 *
 * @param rootId  The root node ID
 * @param nodes   A map of the nodes in the tree by their component IDs, merged across all modules
 * @param modules The per-module trees, for projects whose modules may resolve a component differently.
 *                Empty for ecosystems that don't report per-module dependency trees.
 */
public record DepTree(String rootId, Map<String, DepTreeNode> nodes, List<DepTreeModule> modules) {

    public DepTree(String rootId, Map<String, DepTreeNode> nodes) {
        this(rootId, nodes, List.of());
    }

    public DepTreeNode getRootNode() {
        return nodes.get(rootId);
    }

    public String getRootNodeDescriptorFilePath() {
        return getRootNode().getDescriptorFilePath();
    }
}
