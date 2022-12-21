package com.jfrog.ide.common.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yahavi
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class License extends IssueOrLicense {
    private static final String UNKNOWN_LICENCE_FULL_NAME = "Unknown license";
    @SuppressWarnings("FieldCanBeLocal")
    private static final String UNKNOWN_LICENCE_NAME = "Unknown";
    private String component = "";
    private final String fullName;
    private final String name;
    private List<String> moreInfoUrl = new ArrayList<>();
    private Severity severity;
    private String lastUpdated;

    public License() {
        this.fullName = UNKNOWN_LICENCE_FULL_NAME;
        this.name = UNKNOWN_LICENCE_NAME;
    }

    public License(String fullName, String name, List<String> moreInfoUrl, Severity severity, String lastUpdated) {
        this.fullName = StringUtils.trim(fullName);
        this.name = StringUtils.trim(name);
        this.moreInfoUrl = moreInfoUrl;
        this.severity = severity;
        this.lastUpdated = lastUpdated;
    }

    @SuppressWarnings("unused")
    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    @SuppressWarnings("unused")
    public String getFullName() {
        return fullName;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public List<String> getMoreInfoUrl() {
        return moreInfoUrl;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    @JsonIgnore
    @SuppressWarnings("unused")
    public boolean isFullNameEmpty() {
        return StringUtils.isBlank(fullName) || fullName.equals(License.UNKNOWN_LICENCE_FULL_NAME);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        License otherLicense = (License) other;
        return StringUtils.equals(fullName, otherLicense.fullName) && StringUtils.equals(name, otherLicense.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fullName);
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getTitle() {
        return "License violation";
    }

    @Override
    public String getSubtitle() {
        return name;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }
}