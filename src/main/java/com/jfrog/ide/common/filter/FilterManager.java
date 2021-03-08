package com.jfrog.ide.common.filter;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jfrog.build.extractor.scan.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Severities and licenses filtering.
 *
 * @author yahavi
 */
public class FilterManager {

    private final Map<Severity, Boolean> selectedSeverities = Maps.newTreeMap(Collections.reverseOrder());
    private List<Pair<String, Boolean>> selectableBuilds = Lists.newArrayList();
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

    @SuppressWarnings("unused")
    public List<Pair<String, Boolean>> getSelectableBuilds() {
        return this.selectableBuilds;
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

    public void addBuild(String build) {
        selectableBuilds.add(MutablePair.of(build, false));
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
    public void collectsFiltersInformation(DependencyTree root) {
        Enumeration<?> enumeration = root.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DependencyTree child = (DependencyTree) enumeration.nextElement();
            addLicenses(child.getLicenses());
            addScopes(child.getScopes());
        }
    }

    protected void clearBuilds() {
        this.selectableBuilds = Lists.newArrayList();
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
     * Return filtered issues according to the selected component and user filters.
     *
     * @param selectedNodes - Selected tree nodes that the user chose from the ui.
     * @return filtered issues according to the selected component and user filters.
     */
    public Set<Issue> getFilteredScanIssues(List<DependencyTree> selectedNodes) {
        Set<Issue> filteredIssues = Sets.newHashSet();
        selectedNodes.forEach(node -> filteredIssues.addAll(filterIssues(node.getIssues())));
        return filteredIssues;
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