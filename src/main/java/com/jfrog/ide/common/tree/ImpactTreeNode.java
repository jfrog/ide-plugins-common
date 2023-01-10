package com.jfrog.ide.common.tree;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ImpactTreeNode {
    String name;
    List<ImpactTreeNode> children = new ArrayList<>();

    public ImpactTreeNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getNameWithoutPrefix() {
        return removeComponentIdPrefix(name);
    }

    public List<ImpactTreeNode> getChildren() {
        return children;
    }

    public boolean contains(String name) {
        if (getNameWithoutPrefix().contains(name)) {
            return true;
        }
        for (var child : children) {
            if (child.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private static String removeComponentIdPrefix(String compId) {
        return StringUtils.substringAfter(compId, "://");
    }
}
