package com.jfrog.ide.common.deptree;

import java.util.HashSet;
import java.util.Set;

public class DepTreeNode {
    // Used in nodes that their descriptor files are part of the project only.
    private String descriptorFilePath;
    private Set<String> scopes = new HashSet<>();
    private Set<String> children = new HashSet<>();

    public DepTreeNode descriptorFilePath(String path) {
        this.descriptorFilePath = path;
        return this;
    }

    public DepTreeNode scopes(Set<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    public DepTreeNode children(Set<String> children) {
        this.children = children;
        return this;
    }

    public String getDescriptorFilePath() {
        return descriptorFilePath;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public Set<String> getChildren() {
        return children;
    }
}
