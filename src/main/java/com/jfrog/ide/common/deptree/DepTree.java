package com.jfrog.ide.common.deptree;

import java.util.Map;

public class DepTree {
    private final String rootId;
    // A map of the nodes in the tree by their component IDs
    private final Map<String, DepTreeNode> nodes;

    public DepTree(String rootId, Map<String, DepTreeNode> nodes) {
        this.rootId = rootId;
        this.nodes = nodes;
    }

    public String getRootId() {
        return rootId;
    }

    public Map<String, DepTreeNode> getNodes() {
        return nodes;
    }

    public DepTreeNode getRootNode() {
        return nodes.get(rootId);
    }
}
