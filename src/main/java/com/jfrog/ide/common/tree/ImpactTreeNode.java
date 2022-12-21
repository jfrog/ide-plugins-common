package com.jfrog.ide.common.tree;

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

    public List<ImpactTreeNode> getChildren() {
        return children;
    }
}
