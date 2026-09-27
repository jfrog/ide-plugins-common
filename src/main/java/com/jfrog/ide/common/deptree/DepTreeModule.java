package com.jfrog.ide.common.deptree;

import java.util.Map;

/**
 * The dependency tree of a single module of a multi-module project, as resolved by that module.
 *
 * @param rootId the module's root node ID
 * @param nodes  the nodes reachable from the module's root, by their component IDs
 */
public record DepTreeModule(String rootId, Map<String, DepTreeNode> nodes) {
}
