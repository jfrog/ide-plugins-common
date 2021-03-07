package com.jfrog.ide.common.filter;

import com.google.common.collect.Maps;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.jfrog.build.extractor.scan.*;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Severities and licenses filtering.
 *
 * @author yahavi
 */
public class FilterManager {

    private final Map<Severity, Boolean> selectedSeverities = Maps.newTreeMap(Collections.reverseOrder());
    private final Map<License, Boolean> selectedLicenses = Maps.newHashMap();
    private final Map<String, Boolean> selectedBranches = Maps.newHashMap();
    private final Map<Scope, Boolean> selectedScopes = Maps.newHashMap();

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
        return this.selectedSeverities;
    }

    @SuppressWarnings({"unused"})
    public Map<License, Boolean> getSelectedLicenses() {
        return this.selectedLicenses;
    }


    @SuppressWarnings("unused")
    public Map<String, Boolean> getSelectedBranches() {
        return this.selectedBranches;
    }

    @SuppressWarnings("unused")
    public Map<Scope, Boolean> getSelectedScopes() {
        return this.selectedScopes;
    }

    /**
     * Add missing licenses.
     *
     * @param scanLicenses - Licenses from the Xray scan.
     */
    public void addLicenses(Set<License> scanLicenses) {
        scanLicenses.forEach(license -> selectedLicenses.putIfAbsent(license, true));
    }

    public void addBranches(Set<String> branches) {
        branches.forEach(branch -> selectedBranches.putIfAbsent(branch, false));
    }

    /**
     * Add missing scopes.
     *
     * @param scanScopes - Scopes from the Xray scan.
     */
    public void addScopes(Set<Scope> scanScopes) {
        scanScopes.forEach(scope -> selectedScopes.putIfAbsent(scope, true));
    }

    /**
     * Recursively, add all dependency tree's licenses and scopes.
     *
     * @param root - The root dependency tree node.
     */
    public void collectAllLicensesAndScopes(DependencyTree root) {
        Enumeration<?> enumeration = root.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DependencyTree child = (DependencyTree) enumeration.nextElement();
            addLicenses(child.getLicenses());
            addScopes(child.getScopes());
        }
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

    private boolean isSeveritySelected(DependencyTree node) {
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

    /**
     * Return true if this node contains a selected license.
     *
     * @param node - The dependency tree node
     * @return true if this node contains a selected license
     */
    private boolean isLicenseSelected(DependencyTree node) {
        return node.getLicenses().stream().anyMatch(this::isLicenseSelected);
    }

    private boolean isScopeSelected(Scope scope) {
        return selectedScopes.getOrDefault(scope, false);
    }

    /**
     * Return true if this node or its parents contains a selected scope.
     *
     * @param node - The dependency tree node
     * @return true if this node or its parents contains a selected scope
     */
    private boolean isScopeSelected(DependencyTree node) {
        while (node != null) {
            if (node.getScopes().stream().anyMatch(this::isScopeSelected)) {
                return true;
            }
            node = (DependencyTree) node.getParent();
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
     * @param unfilteredRoot - The scan results before filtering
     */
    @SuppressWarnings({"unused"})
    public DependencyTree applyFilters(DependencyTree unfilteredRoot) {
        DependencyTree filteredRoot = (DependencyTree) unfilteredRoot.clone();
        filteredRoot.getIssues().clear();
        applyFilters(unfilteredRoot, filteredRoot, new MutableBoolean());
        return filteredRoot;
    }

    private void applyFilters(DependencyTree unfilteredRoot, DependencyTree filteredRoot, MutableBoolean selected) {
        selected.setValue(isSeveritySelected(unfilteredRoot) && isLicenseSelected(unfilteredRoot) && isScopeSelected(unfilteredRoot));
        for (int i = 0; i < unfilteredRoot.getChildCount(); i++) {
            DependencyTree unfilteredChild = (DependencyTree) unfilteredRoot.getChildAt(i);
            DependencyTree filteredChild = cloneNode(unfilteredChild);
            MutableBoolean childSelected = new MutableBoolean();
            applyFilters(unfilteredChild, filteredChild, childSelected);
            if (childSelected.booleanValue()) {
                selected.setValue(true);
                filteredRoot.add(filteredChild);
            }
        }
    }

    private DependencyTree cloneNode(DependencyTree node) {
        DependencyTree filteredChild = (DependencyTree) node.clone();
        filteredChild.setIssues(filterIssues(node.getIssues()));
        return filteredChild;
    }
}