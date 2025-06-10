package com.jfrog.ide.common.webview;

import lombok.Getter;

@Getter
public class ImpactGraph {
    private final ImpactGraphNode root;
    private final int pathsLimit;

    public ImpactGraph(ImpactGraphNode root, int pathsLimit) {
        this.root = root;
        this.pathsLimit = pathsLimit;
    }
}
