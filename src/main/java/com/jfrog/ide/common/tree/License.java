package com.jfrog.ide.common.tree;

public class License {
    private String name;
    private String moreInfoUrl;

    public License(String name, String moreInfoUrl) {
        this.name = name;
        this.moreInfoUrl = moreInfoUrl;
    }

    public String getName() {
        return name;
    }

    public String getMoreInfoUrl() {
        return moreInfoUrl;
    }
}
