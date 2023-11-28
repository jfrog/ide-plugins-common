package com.jfrog.ide.common.deptree;

import java.util.Map;

/**
 * Represents a dependency tree.
 *
 * @param rootId The root node ID
 * @param nodes  A map of the nodes in the tree by their component IDs
 */
public record DepTree(String rootId, Map<String, DepTreeNode> nodes) {

    public DepTreeNode getRootNode() {
        return nodes.get(rootId);
    }

    public String getRootNodeDescriptorFilePath() {
        return getRootNode().getDescriptorFilePath();
    }
}
