package com.jfrog.ide.common.webview;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
public class DependencyPage {
    private String id;
    private String component;
    private String componentType;
    private final String pageType = "DEPENDENCY";
    private String version;
    private String severity;
    private License[] license;
    private String summary;
    private String[] fixedVersion;
    private String[] infectedVersion;
    private Reference[] references;
    private Cve cve;
    private ImpactGraph impactGraph;
    private String[] watchName;
    private String edited;
    private ExtendedInformation extendedInformation;

    public DependencyPage() {
    }

    public DependencyPage component(String component) {
        this.component = component;
        return this;
    }

    public DependencyPage id(String id) {
        this.id = id;
        return this;
    }

    public DependencyPage componentType(String componentType) {
        this.componentType = componentType;
        return this;
    }

    public DependencyPage version(String version) {
        this.version = version;
        return this;
    }

    public DependencyPage severity(String severity) {
        this.severity = severity;
        return this;
    }

    public DependencyPage license(License[] license) {
        this.license = license;
        return this;
    }

    public DependencyPage summary(String summary) {
        this.summary = summary;
        return this;
    }

    public DependencyPage fixedVersion(String[] fixedVersion) {
        this.fixedVersion = fixedVersion;
        return this;
    }

    public DependencyPage infectedVersion(String[] infectedVersion) {
        this.infectedVersion = infectedVersion;
        return this;
    }

    public DependencyPage references(Reference[] references) {
        this.references = references;
        return this;
    }

    public DependencyPage cve(Cve cve) {
        this.cve = cve;
        return this;
    }

    public DependencyPage impactGraph(ImpactGraph impactGraph) {
        this.impactGraph = impactGraph;
        return this;
    }
}

