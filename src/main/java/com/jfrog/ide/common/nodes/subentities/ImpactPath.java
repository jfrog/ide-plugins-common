package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.webview.ImpactGraph;
import com.jfrog.ide.common.webview.ImpactGraphNode;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Getter
public class ImpactPath {
    @JsonProperty()
    private String name;
    @JsonProperty()
    private String version;

    // empty constructor for deserialization
    @SuppressWarnings("unused")
    public ImpactPath() {
    }

    @SuppressWarnings("unused")
    public ImpactPath(String dependencyName, String dependencyVersion) {
        this.name = dependencyName;
        this.version = dependencyVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImpactPath that = (ImpactPath) o;
        return Objects.equals(this.name, that.name) && Objects.equals(this.version, that.version);
    }

    /**
     * Converts a list of impact paths to an ImpactGraph.
     * Each path is a list of ImpactPath objects, representing a path from root to leaf.
     * Node names are "name:version" (or just "name" if version is empty).
     */
    public static ImpactGraph toImpactGraph(List<List<ImpactPath>> impactPaths) {
        // Use a dummy root node
        Node root = new Node("root");
        int maxDepth = 0;
        for (List<ImpactPath> path : impactPaths) {
            Node current = root;
            int depth = 0;
            for (ImpactPath ip : path) {
                String nodeName = ip.getName() + (ip.getVersion() != null && !ip.getVersion().isEmpty() ? ":" + ip.getVersion() : "");
                current = current.children.computeIfAbsent(nodeName, Node::new);
                depth++;
            }
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }
        ImpactGraphNode rootGraphNode = toGraphNode(root);
        // Set pathsLimit to the maximum depth found
        return new ImpactGraph(rootGraphNode, maxDepth + 1); // +1 to include root
    }

    // Internal tree node for building
    private static class Node {
        String name;
        Map<String, Node> children = new LinkedHashMap<>();
        Node(String name) { this.name = name; }
    }

    // Convert internal Node tree to ImpactGraphNode tree
    private static ImpactGraphNode toGraphNode(Node node) {
        ImpactGraphNode[] children = node.children.values().stream()
            .map(ImpactPath::toGraphNode)
            .toArray(ImpactGraphNode[]::new);
        return new ImpactGraphNode(node.name, children);
    }
}