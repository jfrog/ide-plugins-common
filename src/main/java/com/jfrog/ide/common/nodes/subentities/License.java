package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class License {
    @JsonProperty()
    private String name;
    @JsonProperty()
    private String moreInfoUrl;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    private License() {
    }

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
