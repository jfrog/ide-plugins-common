package com.jfrog.ide.common.nodes;

import java.util.ArrayList;
import java.util.List;

import static com.jfrog.ide.common.utils.Utils.removeComponentIdPrefix;

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
}
