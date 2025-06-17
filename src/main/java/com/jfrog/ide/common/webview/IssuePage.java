package com.jfrog.ide.common.webview;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
public class IssuePage {
    private String pageType;
    private String header;
    private String severity;
    private String abbreviation;
    private Location location;
    private String description;
    private Finding finding;

    public IssuePage() {
    }

    public IssuePage(IssuePage other) {
        if (other == null) {
            return;
        }
        this.pageType = other.pageType;
        this.header = other.header;
        this.severity = other.severity;
        this.abbreviation = other.abbreviation;
        this.location = other.location != null ? new Location(other.location) : null;
        this.description = other.description;
        this.finding = other.finding != null ? new Finding(other.finding) : null;
    }

    public IssuePage header(String header) {
        this.header = header;
        return this;
    }

    public IssuePage severity(String severity) {
        this.severity = severity;
        return this;
    }

    public IssuePage abbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
        return this;
    }

    public IssuePage location(Location location) {
        this.location = location;
        return this;
    }

    public IssuePage description(String description) {
        this.description = description;
        return this;
    }

    public IssuePage finding(Finding finding) {
        this.finding = finding;
        return this;
    }

    public IssuePage type(String type) {
        this.pageType = type;
        return this;
    }
}

