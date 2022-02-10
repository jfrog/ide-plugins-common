package com.jfrog.ide.common.exporter;

import com.jfrog.ide.common.exporter.exportable.ExportableViolatedLicense;
import com.jfrog.ide.common.exporter.exportable.ExportableVulnerability;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yahavi
 **/
public abstract class Exporter {
    private final DependencyTree root;

    public Exporter(DependencyTree root) {
        this.root = root;
    }

    /**
     * Generate vulnerabilities report that may be exported to a file.
     *
     * @return vulnerabilities report text.
     * @throws Exception in case of any unexpected error.
     */
    public abstract String generateVulnerabilitiesReport() throws Exception;

    /**
     * Generate violated licenses report that may be exported to a file.
     *
     * @return violated licenses report text.
     * @throws Exception in case of any unexpected error.
     */
    public abstract String generateViolatedLicensesReport() throws Exception;

    /**
     * Create a single exportable vulnerability.
     *
     * @param directDependency - The direct dependency ID in the dependency tree
     * @param issue            - The issue
     * @return a single exportable vulnerability.
     */
    protected abstract ExportableVulnerability createExportableVulnerability(DependencyTree directDependency, Issue issue);

    /**
     * Create a single exportable violated license.
     *
     * @param directDependency - The direct dependency ID in the dependency tree
     * @param violatedLicense  - The violated license
     * @return a single exportable violated license.
     */
    protected abstract ExportableViolatedLicense createExportableViolatedLicense(DependencyTree directDependency, License violatedLicense);

    /**
     * Collect the exportable vulnerabilities to export.
     *
     * @return the exportable vulnerabilities to export.
     */
    protected Collection<ExportableVulnerability> collectVulnerabilities() {
        Map<String, ExportableVulnerability> exportableVulnerabilities = new HashMap<>();
        populateExportableVulnerabilities(root, exportableVulnerabilities);
        return exportableVulnerabilities.values();
    }

    private void populateExportableVulnerabilities(DependencyTree node,
                                                   Map<String, ExportableVulnerability> exportableVulnerabilities) {
        if (node.isMetadata()) {
            // Node is an ancestor of a direct dependency
            for (DependencyTree child : node.getChildren()) {
                populateExportableVulnerabilities(child, exportableVulnerabilities);
            }
            return;
        }
        // Node is a direct dependency
        for (Issue issue : node.getIssues()) {
            ExportableVulnerability exportable = exportableVulnerabilities.get(issue.getIssueId());
            if (exportable == null) {
                // ExportableVulnerability is new. Add it to the map.
                exportableVulnerabilities.put(issue.getIssueId(), createExportableVulnerability(node, issue));
            } else {
                // ExportableVulnerability already exists in the map, Append the direct dependency to it.
                exportable.appendDirectDependency(node);
            }
        }
    }

    /**
     * Collect the exportable violated licenses to export.
     *
     * @return the exportable violated licenses to export.
     */
    protected Collection<ExportableViolatedLicense> collectViolatedLicenses() {
        Map<String, ExportableViolatedLicense> exportableViolatedLicenses = new HashMap<>();
        populateExportableViolatedLicenses(root, exportableViolatedLicenses);
        return exportableViolatedLicenses.values();
    }

    private void populateExportableViolatedLicenses(DependencyTree node,
                                                    Map<String, ExportableViolatedLicense> exportableViolatedLicenses) {
        if (node.isMetadata()) {
            // Node is an ancestor of a direct dependency
            for (DependencyTree child : node.getChildren()) {
                populateExportableViolatedLicenses(child, exportableViolatedLicenses);
            }
            return;
        }
        // Node is a direct dependency
        for (License violatedLicense : node.getViolatedLicenses()) {
            ExportableViolatedLicense exportable = exportableViolatedLicenses.get(violatedLicense.getName());
            if (exportable == null) {
                exportableViolatedLicenses.put(violatedLicense.getName(), createExportableViolatedLicense(node, violatedLicense));
            } else {
                exportable.appendDirectDependency(node);
            }
        }
    }
}
