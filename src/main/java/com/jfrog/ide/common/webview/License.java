package com.jfrog.ide.common.webview;

import lombok.Getter;

@Getter
public class License {
    private final String name;
    private final String link;

    public License(String name, String link) {
        this.name = name;
        this.link = link;
    }
}