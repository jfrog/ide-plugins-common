package com.jfrog.ide.common.exporter.exportable;

import org.jfrog.build.extractor.scan.DependencyTree;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

/**
 * Represents the shared code between ExportableVulnerability and ExportableViolatedLicense.
 *
 * @author yahavi
 **/
abstract class ExportableComponent {
    protected ExportableComponent(DependencyTree directDependency, String component) {
        setImpactedDependency(component);
        DependencyTree parent = (DependencyTree) directDependency.getParent();
        setType(parent.getGeneralInfo().getPkgType());
        appendDirectDependency(directDependency);
    }

    public abstract void setImpactedDependencyName(String impactedDependencyName);

    public abstract void setImpactedDependencyVersion(String impactedDependencyVersion);

    public abstract void setType(String type);

    public abstract void addDirectDependency(String directDependency);

    public void setImpactedDependency(String impactedDependency) {
        setImpactedDependencyName(substringBeforeLast(impactedDependency, ":"));
        setImpactedDependencyVersion(substringAfterLast(impactedDependency, ":"));
    }

    public void appendDirectDependency(DependencyTree directDependency) {
        addDirectDependency(directDependency.getGeneralInfo().getComponentId());
    }
}
