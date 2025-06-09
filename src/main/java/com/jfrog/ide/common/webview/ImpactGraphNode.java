package com.jfrog.ide.common.webview;

import lombok.Getter;

@Getter
public class ImpactGraphNode {
    private final String name;
    private final ImpactGraphNode[] children;

    public ImpactGraphNode(String name, ImpactGraphNode[] children) {
        this.name = name;
        this.children = children;
    }
}
