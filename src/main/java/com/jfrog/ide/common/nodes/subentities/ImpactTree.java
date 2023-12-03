package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public class ImpactTree {
    public static final int IMPACT_PATHS_LIMIT = 20;

    @Getter
    @JsonProperty
    private ImpactTreeNode root;
    @JsonProperty
    private int impactPathsCount = 0;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    public ImpactTree() {
    }

    public ImpactTree(ImpactTreeNode root) {
        this.root = root;
    }

    public boolean contains(String name) {
        return root.contains(name);
    }

    @SuppressWarnings("unused")
    public int getImpactPathsCount() {
        return impactPathsCount;
    }

    @SuppressWarnings("unused")
    public void incImpactPathsCount() {
        this.impactPathsCount++;
    }
}
