package com.jfrog.ide.common.components.subentities;

public class License {
    private final String name;
    private final String moreInfoUrl;

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
