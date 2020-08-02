package com.jfrog.ide.common.filter;

import com.google.common.collect.Sets;
import org.jfrog.build.extractor.scan.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static com.jfrog.ide.common.filter.Utils.createIssue;
import static com.jfrog.ide.common.filter.Utils.createLicense;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * @author yahavi
 */
public class FilterManagerTest {

    private static final Scope MAIN_SCOPE = new Scope("main");

    private DependenciesTree root, one, two, three, four, five;
    private Map<Severity, Boolean> severitiesFilters;
    private Map<License, Boolean> licensesFilters;
    private FilterManager filterManager;

    /**
     * Initialize the FilterManager to accept all severities and MIT license.
     */
    @BeforeMethod
    public void setUp() {
        // Create filter manager
        filterManager = new FilterManager();

        // Create severities filter with all severities enabled
        severitiesFilters = filterManager.getSelectedSeverities();
        for (Severity severity : Severity.values()) {
            severitiesFilters.put(severity, true);
        }

        // Create licenses filter with 'MIT' license enabled
        licensesFilters = filterManager.getSelectedLicenses();
        licensesFilters.put(createLicense("MIT"), true);

        // Create scope filter with 'main' scope enabled
        filterManager.getSelectedScopes().put(MAIN_SCOPE, true);

        // Create the dependencies tree
        root = new DependenciesTree("0");
        one = new DependenciesTree("1");
        two = new DependenciesTree("2");
        three = new DependenciesTree("3");
        four = new DependenciesTree("4");
        five = new DependenciesTree("5");
        root.add(one); // 0 -> 1
        root.add(two); // 0 -> 2
        two.add(three); // 2 -> 3
        two.add(four); // 2 -> 4
        four.add(five); // 4 -> 5
        // Add scope 'main' to the root
        root.setScopes(Sets.newHashSet(MAIN_SCOPE));
    }

    @Test
    public void testNoFilter() {
        // Sanity test - Empty tree
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(0, root.getIssueCount());
        assertEquals(0, rootIssues.size());

        // Insert 'Low' issue and 'MIT' license to node 1.
        one.setIssues(Sets.newHashSet(createIssue(Severity.Low)));
        one.setLicenses(Sets.newHashSet(createLicense("MIT")));

        // Apply filter
        DependenciesTree filteredRoot = filterAndAssert();

        // Assert that the issues filtered tree have 1 issue, 1 license and 1 node except the root
        rootIssues = filteredRoot.processTreeIssues();
        assertEquals(1, filteredRoot.getIssueCount());
        assertEquals(1, rootIssues.size());
        assertEquals(1, filteredRoot.getChildren().get(0).getIssueCount());
        assertEquals(1, filteredRoot.getChildren().get(0).getIssues().size());
        assertEquals(1, filteredRoot.getChildren().get(0).getLicenses().size());
        assertEquals(0, filteredRoot.getChildren().get(0).getChildren().size());
    }

    @Test
    public void testOneIssueFilter() {
        // Insert 'Low' issue and 'MIT' license to node 1.
        one.setIssues(Sets.newHashSet(createIssue(Severity.Low)));
        one.setLicenses(Sets.newHashSet(createLicense("MIT")));

        // Filter 'Low' issues
        severitiesFilters.replace(Severity.Low, false);

        // Apply filter
        DependenciesTree filteredRoot = filterAndAssert();

        // Assert that the 'Low' issue from 'testNoFilter' had been filtered
        Set<Issue> rootIssues = filteredRoot.processTreeIssues();
        assertEquals(0, filteredRoot.getIssueCount());
        assertEquals(0, rootIssues.size());
    }

    @Test
    public void testManyIssueFilters() {
        // Filter 'Low' and 'Medium' issues
        severitiesFilters.replace(Severity.Low, false);
        severitiesFilters.replace(Severity.Medium, false);

        // Insert some issues
        one.setIssues(Sets.newHashSet(createIssue(Severity.Low)));
        two.setIssues(Sets.newHashSet(createIssue(Severity.Medium), createIssue(Severity.High)));
        four.setIssues(Sets.newHashSet(createIssue(Severity.Unknown)));
        five.setIssues(Sets.newHashSet(createIssue(Severity.Low)));

        // Fill all nodes with licenses to avoid license filtering
        one.setLicenses(Sets.newHashSet(createLicense("MIT")));
        two.setLicenses(Sets.newHashSet(createLicense("MIT")));
        three.setLicenses(Sets.newHashSet(createLicense("MIT")));
        four.setLicenses(Sets.newHashSet(createLicense("MIT")));
        five.setLicenses(Sets.newHashSet(createLicense("MIT")));

        // Apply filter
        DependenciesTree filteredRoot = filterAndAssert();

        // Assert that the issues filtered tree have 2 issues (1 'High' and 1 'Unknown')
        Set<Issue> rootIssues = filteredRoot.processTreeIssues();
        assertEquals(2, filteredRoot.getIssueCount());
        assertEquals(2, rootIssues.size());
        rootIssues.forEach(issue -> {
            switch (issue.getComponent()) {
                case "2":
                    assertEquals(Severity.High, issue.getSeverity());
                    break;
                case "4":
                    assertEquals(Severity.Unknown, issue.getSeverity());
                    break;
                default:
                    fail("issues filtered tree should have only 1 High issue and 1 Unknown issue");
                    break;
            }
        });
    }

    @Test
    public void testOneLicenseFilter() {
        // Insert 'Low' issue and 'MIT' license to node 1.
        one.setLicenses(Sets.newHashSet(createLicense("MIT")));

        // Filter 'MIT' license
        licensesFilters.put(createLicense("MIT"), false);

        // Apply filter
        DependenciesTree filteredRoot = filterAndAssert();

        // Assert that the license in "1" has been filtered
        assertEquals(1, one.getLicenses().size());
        assertEquals(0, filteredRoot.getLicenses().size());
        assertEquals(0, filteredRoot.getChildren().size());
    }

    @Test
    public void testManyLicenseFilter() {
        // Accept 'MIT' and 'GPL' licenses
        licensesFilters.put(createLicense("GPL"), true);

        // Insert some licenses
        one.setLicenses(Sets.newHashSet(createLicense("MIT")));
        two.setLicenses(Sets.newHashSet(createLicense("MIT"), createLicense("GNU")));
        three.setLicenses(Sets.newHashSet(createLicense("GNU")));
        five.setLicenses(Sets.newHashSet(createLicense("MIT")));

        // Apply filter
        DependenciesTree filteredRoot = filterAndAssert();

        // Assert that the license filtered tree root has 2 children ("2" and "3")
        assertEquals(2, filteredRoot.getChildren().size());
        filteredRoot.processTreeIssues();

        // Check each one of the license filtered tree nodes
        assertEquals(0, filteredRoot.getLicenses().size());
        assertEquals(2, filteredRoot.getChildren().size());
        filteredRoot.getChildren().forEach(child -> {
            if (child.getUserObject().equals("1")) {
                assertEquals(1, child.getLicenses().size());
            }
            if (child.getUserObject().equals("2")) {
                assertEquals(2, child.getLicenses().size());
                assertEquals(1, child.getChildren().size()); // "3" filtered out
                child.getChildren().forEach(twoChild -> {
                    // "4" have no licenses and should appear because of "5"
                    assertEquals(0, twoChild.getLicenses().size());
                    assertEquals(1, twoChild.getChildren().size());
                    twoChild.getChildren().forEach(fourChild -> {
                        // 5
                        assertEquals(1, fourChild.getLicenses().size());
                    });
                });
            }
        });
    }

    private DependenciesTree filterAndAssert() {
        DependenciesTree filteredRoot = filterManager.applyFilters(root);
        assertTrue(filteredRoot.getScopes().contains(MAIN_SCOPE));
        return filteredRoot;
    }

}