package org.jfrog.filter;

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
 * @author yahavi
 */
public class FilterManager {

    private static FilterManager instance;
    private Map<Severity, Boolean> selectedSeverities = Maps.newTreeMap(Collections.reverseOrder());
    private Map<License, Boolean> selectedLicenses = Maps.newHashMap();

    public static FilterManager getInstance() {
        if (instance == null) {
            instance = new FilterManager();
        }
        return instance;
    }

    private FilterManager() {
        for (Severity severity : Severity.NEW_SEVERITIES) {
            selectedSeverities.put(severity, true);
        }
    }

    @SuppressWarnings("unused")
    public Map<Severity, Boolean> getSelectedSeverities() {
        return selectedSeverities;
    }

    @SuppressWarnings("unused")
    public Map<License, Boolean> getSelectedLicenses() {
        return selectedLicenses;
    }

    public void setLicenses(Set<License> scanLicenses) {
        scanLicenses.forEach(license -> {
            if (!selectedLicenses.containsKey(license)) {
                selectedLicenses.put(license, true);
            }
        });
    }

    private boolean isSeveritySelected(Issue issue) {
        Severity severity = issue.getSeverity();
        return severity != null && selectedSeverities.get(severity);
    }

    private boolean isSeveritySelected(DependenciesTree node) {
        if (node.getIssues().size() == 0 && selectedSeverities.get(Severity.Normal)) {
            return true;
        }
        for (Issue issue : node.getIssues()) {
            if (isSeveritySelected(issue)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLicenseSelected(License license) {
        return selectedLicenses.containsKey(license) && selectedLicenses.get(license);
    }

    private boolean isLicenseSelected(DependenciesTree node) {
        for (License license : node.getLicenses()) {
            if (isLicenseSelected(license)) {
                return true;
            }
        }
        return false;
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
    @SuppressWarnings("unused")
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