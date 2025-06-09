package com.jfrog.ide.common.webview;

import lombok.Getter;

@Getter
public class Finding {
    private final String does;
    private final String happen;
    private final String meaning;
    private final String snippet;

    public Finding(String happen, String meaning, String snippet, String does) {
        this.happen = happen;
        this.meaning = meaning;
        this.snippet = snippet;
        this.does = does;
    }

    public Finding(Finding other) {
        this(other.happen, other.meaning, other.snippet, other.does);
    }
}

