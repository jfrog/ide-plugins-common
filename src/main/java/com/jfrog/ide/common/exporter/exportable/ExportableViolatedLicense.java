package com.jfrog.ide.common.exporter.exportable;

import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.License;

/**
 * Represents a single exportable violated license.
 *
 * @author yahavi
 **/
public abstract class ExportableViolatedLicense extends ExportableComponent {
    protected ExportableViolatedLicense(DependencyTree directDependency, License license) {
        super(directDependency, license.getComponent());
        setName(license.getName());
    }

    public abstract void setName(String name);
}