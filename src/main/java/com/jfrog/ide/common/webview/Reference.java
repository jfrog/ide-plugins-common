package com.jfrog.ide.common.webview;

import lombok.Getter;

@Getter
public class Reference {
    private final String url;
    private final String text;

    public Reference(String url, String text) {
        this.url = url;
        this.text = text;
    }
}
