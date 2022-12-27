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
public class LicenseViolation extends VulnerabilityOrViolation {
    private static final String UNKNOWN_LICENCE_FULL_NAME = "Unknown license";
    @SuppressWarnings("FieldCanBeLocal")
    private static final String UNKNOWN_LICENCE_NAME = "Unknown";
    private final String fullName;
    private final String name;
    private List<String> references = new ArrayList<>();
    private Severity severity;
    private String lastUpdated;
    private List<String> watchNames;

    public LicenseViolation() {
        this.fullName = UNKNOWN_LICENCE_FULL_NAME;
        this.name = UNKNOWN_LICENCE_NAME;
    }

    public LicenseViolation(String fullName, String name, List<String> references, Severity severity, String lastUpdated, List<String> watchNames) {
        this.fullName = StringUtils.trim(fullName);
        this.name = StringUtils.trim(name);
        this.references = references;
        this.severity = severity;
        this.lastUpdated = lastUpdated;
        this.watchNames = watchNames;
    }

    @SuppressWarnings("unused")
    public String getFullName() {
        return fullName;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public List<String> getReferences() {
        return references;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public List<String> getWatchNames() {
        return watchNames;
    }

    @JsonIgnore
    @SuppressWarnings("unused")
    public boolean isFullNameEmpty() {
        return StringUtils.isBlank(fullName) || fullName.equals(LicenseViolation.UNKNOWN_LICENCE_FULL_NAME);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        LicenseViolation otherLicense = (LicenseViolation) other;
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