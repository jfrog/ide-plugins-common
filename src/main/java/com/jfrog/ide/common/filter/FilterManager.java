package com.jfrog.ide.common.filter;

import com.google.common.collect.Maps;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Severity;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Severities and licenses filtering.
 *
 * @author yahavi
 */
public class FilterManager {

    private Map<Severity, Boolean> selectedSeverities = Maps.newTreeMap(Collections.reverseOrder());
    private Map<License, Boolean> selectedLicenses = Maps.newHashMap();

    protected FilterManager() {
        for (Severity severity : Severity.NEW_SEVERITIES) {
            selectedSeverities.put(severity, true);
        }
    }

    @SuppressWarnings({"unused"})
    protected void setSelectedSeverities(Map<Severity, Boolean> selectedSeverities) {
        for (Severity severity : Severity.NEW_SEVERITIES) {
            if (selectedSeverities.containsKey(severity)) {
                this.selectedSeverities.put(severity, selectedSeverities.get(severity));
            }
        }
    }

    @SuppressWarnings({"unused"})
    public Map<Severity, Boolean> getSelectedSeverities() {
        return selectedSeverities;
    }

    @SuppressWarnings({"unused"})
    public Map<License, Boolean> getSelectedLicenses() {
        return selectedLicenses;
    }

    /**
     * Add missing licenses.
     *
     * @param scanLicenses - Licenses from the Xray scan.
     */
    public void addLicenses(Set<License> scanLicenses) {
        scanLicenses.forEach(license -> selectedLicenses.putIfAbsent(license, true));
    }

    /**
     * Return true iff severity selected.
     *
     * @param issue - The issue contained the severity.
     * @return true iff severity selected.
     */
    private boolean isSeveritySelected(Issue issue) {
        return selectedSeverities.getOrDefault(issue.getSeverity(), false);
    }

    private boolean isSeveritySelected(DependenciesTree node) {
        // If there are no issues in the node and we accept normal severity, return true
        if (node.getIssues().isEmpty() && selectedSeverities.get(Severity.Normal)) {
            return true;
        }
        // Return true if any issue in this node contains a selected severity
        return node.getIssues().stream().anyMatch(this::isSeveritySelected);
    }

    private boolean isLicenseSelected(License license) {
        return selectedLicenses.getOrDefault(license, false);
    }

    private boolean isLicenseSelected(DependenciesTree node) {
        // Return true if any issue in this node contains a selected license
        return node.getLicenses().stream().anyMatch(this::isLicenseSelected);
    }

    public Set<Issue> filterIssues(Set<Issue> allIssues) {
        return allIssues
                .stream()
                .filter(this::isSeveritySelected)
                .collect(Collectors.toSet());
    }

    /**
     * Filter scan results
     *
     * @param unfilteredRoot      In - The scan results
     * @param issuesFilteredRoot  Out - Filtered issues tree
     * @param LicenseFilteredRoot Out - Filtered licenses tree
     */
    @SuppressWarnings({"unused"})
    public void applyFilters(DependenciesTree unfilteredRoot, DependenciesTree issuesFilteredRoot, DependenciesTree LicenseFilteredRoot) {
        applyFilters(unfilteredRoot, issuesFilteredRoot, LicenseFilteredRoot, new MutableBoolean(), new MutableBoolean());
    }

    private void applyFilters(DependenciesTree unfilteredRoot, DependenciesTree issuesFilteredRoot, DependenciesTree licenseFilteredRoot, MutableBoolean severitySelected, MutableBoolean licenseSelected) {
        severitySelected.setValue(isSeveritySelected(unfilteredRoot));
        licenseSelected.setValue(isLicenseSelected(unfilteredRoot));
        for (int i = 0; i < unfilteredRoot.getChildCount(); i++) {
            DependenciesTree unfilteredChild = (DependenciesTree) unfilteredRoot.getChildAt(i);
            DependenciesTree filteredSeverityChild = getFilteredTreeNode(unfilteredChild);
            DependenciesTree filteredLicenseChild = (DependenciesTree) unfilteredChild.clone();
            MutableBoolean childSeveritySelected = new MutableBoolean();
            MutableBoolean childLicenseSelected = new MutableBoolean();
            applyFilters(unfilteredChild, filteredSeverityChild, filteredLicenseChild, childSeveritySelected, childLicenseSelected);
            if (childSeveritySelected.booleanValue()) {
                severitySelected.setValue(true);
                issuesFilteredRoot.add(filteredSeverityChild);
            }
            if (childLicenseSelected.booleanValue()) {
                licenseSelected.setValue(true);
                licenseFilteredRoot.add(filteredLicenseChild);
            }
        }
    }

    private DependenciesTree getFilteredTreeNode(DependenciesTree unfilteredChild) {
        DependenciesTree filteredSeverityChild = (DependenciesTree) unfilteredChild.clone();
        filteredSeverityChild.setIssues(filterIssues(unfilteredChild.getIssues()));
        return filteredSeverityChild;
    }
}