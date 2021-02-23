package com.jfrog.ide.common.filter;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.jfrog.build.extractor.scan.*;

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

    private final Map<Severity, Boolean> selectedSeverities = Maps.newTreeMap(Collections.reverseOrder());
    private final Map<License, Boolean> selectedLicenses = Maps.newHashMap();
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

    /**
     * Add missing scopes.
     *
     * @param scanScopes - Scopes from the Xray scan.
     */
    public void addScopes(Set<Scope> scanScopes) {
        scanScopes.forEach(scope -> selectedScopes.putIfAbsent(scope, true));
    }

    /**
     * Recursively, add all dependencies list licenses to the licenses set.
     *
     * @param node        - In - The root DependenciesTree node.
     * @param allLicenses - Out - All licenses in the tree.
     */
    protected void collectAllLicenses(DependenciesTree node, Set<License> allLicenses) {
        allLicenses.addAll(node.getLicenses());
        node.getChildren().forEach(child -> collectAllLicenses(child, allLicenses));
        addLicenses(allLicenses);
    }

    /**
     * Recursively, add all dependencies list scopes to the scopes set.
     *
     * @param node      - In - The root DependenciesTree node.
     * @param allScopes - Out - All licenses in the tree.
     */
    protected void collectAllScopes(DependenciesTree node, Set<Scope> allScopes) {
        allScopes.addAll(node.getScopes());
        node.getChildren().forEach(child -> collectAllScopes(child, allScopes));
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

    /**
     * Return true if this node contains a selected license.
     *
     * @param node - The dependencies tree node
     * @return true if this node contains a selected license
     */
    private boolean isLicenseSelected(DependenciesTree node) {
        return node.getLicenses().stream().anyMatch(this::isLicenseSelected);
    }

    private boolean isScopeSelected(Scope scope) {
        return selectedScopes.getOrDefault(scope, false);
    }

    /**
     * Return true if this node or its parents contains a selected scope.
     *
     * @param node - The dependencies tree node
     * @return true if this node or its parents contains a selected scope
     */
    private boolean isScopeSelected(DependenciesTree node) {
        while (node != null) {
            if (node.getScopes().stream().anyMatch(this::isScopeSelected)) {
                return true;
            }
            node = (DependenciesTree) node.getParent();
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
    public DependenciesTree applyFilters(DependenciesTree unfilteredRoot) {
        DependenciesTree filteredRoot = (DependenciesTree) unfilteredRoot.clone();
        filteredRoot.getIssues().clear();
        applyFilters(unfilteredRoot, filteredRoot, new MutableBoolean());
        return filteredRoot;
    }

    private void applyFilters(DependenciesTree unfilteredRoot, DependenciesTree filteredRoot, MutableBoolean selected) {
        selected.setValue(isSeveritySelected(unfilteredRoot) && isLicenseSelected(unfilteredRoot) && isScopeSelected(unfilteredRoot));
        for (int i = 0; i < unfilteredRoot.getChildCount(); i++) {
            DependenciesTree unfilteredChild = (DependenciesTree) unfilteredRoot.getChildAt(i);
            DependenciesTree filteredChild = cloneNode(unfilteredChild);
            MutableBoolean childSelected = new MutableBoolean();
            applyFilters(unfilteredChild, filteredChild, childSelected);
            if (childSelected.booleanValue()) {
                selected.setValue(true);
                filteredRoot.add(filteredChild);
            }
        }
    }

    private DependenciesTree cloneNode(DependenciesTree node) {
        DependenciesTree filteredChild = (DependenciesTree) node.clone();
        filteredChild.setIssues(filterIssues(node.getIssues()));
        return filteredChild;
    }
}