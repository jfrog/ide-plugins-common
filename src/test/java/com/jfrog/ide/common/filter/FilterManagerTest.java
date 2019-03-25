package com.jfrog.ide.common.filter;

import com.google.common.collect.Sets;
import com.jfrog.ide.common.ide.DependenciesTreeTestsBase;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Severity;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * @author yahavi
 */
public class FilterManagerTest extends DependenciesTreeTestsBase {

    private FilterManager filterManager = FilterManager.getInstance();
    private Map<Severity, Boolean> severitiesFilters;
    private Map<License, Boolean> licensesFilters;

    /**
     * Initialize the FilterManager to accept all severities and MIT license.
     */
    @BeforeTest
    public void setUp() {
        super.setUp();
        severitiesFilters = filterManager.getSelectedSeverities();
        for (Severity severity : Severity.values()) {
            severitiesFilters.put(severity, true);
        }
        licensesFilters = filterManager.getSelectedLicenses();
        licensesFilters.put(createLicense("MIT"), true);
    }

    @Test
    public void testNoFilter() {
        DependenciesTree issuesFilteredRoot = new DependenciesTree("0");
        DependenciesTree licenseFilteredRoot = new DependenciesTree("0");

        // Sanity test - Empty tree
        filterManager.applyFilters(root, issuesFilteredRoot, new DependenciesTree("0"));
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(0, root.getIssueCount());
        assertEquals(0, rootIssues.size());

        // Insert 'Low' issue and 'MIT' license to node 1.
        one.setIssues(Sets.newHashSet(createIssue(Severity.Low)));
        one.setLicenses(Sets.newHashSet(createLicense("MIT")));

        filterManager.applyFilters(root, issuesFilteredRoot, licenseFilteredRoot);
        // Assert that the issues filtered tree have 1 issue and one node except the root
        rootIssues = issuesFilteredRoot.processTreeIssues();
        assertEquals(1, issuesFilteredRoot.getIssueCount());
        assertEquals(1, rootIssues.size());
        assertEquals(1, issuesFilteredRoot.getChildren().get(0).getIssueCount());
        assertEquals(1, issuesFilteredRoot.getChildren().get(0).getIssues().size());
        assertEquals(0, issuesFilteredRoot.getChildren().get(0).getChildren().size());

        // Assert that the license filtered tree have 1 license and one node except the root
        rootIssues = licenseFilteredRoot.processTreeIssues();
        assertEquals(1, licenseFilteredRoot.getIssueCount());
        assertEquals(1, rootIssues.size());
        assertEquals(1, licenseFilteredRoot.getChildren().get(0).getLicenses().size());
    }

    @Test(dependsOnMethods = {"testNoFilter"})
    public void testOneIssueFilter() {
        // Filter 'Low' issues
        severitiesFilters.replace(Severity.Low, false);
        DependenciesTree issuesFilteredRoot = new DependenciesTree("0");
        DependenciesTree licenseFilteredRoot = new DependenciesTree("0");
        filterManager.applyFilters(root, issuesFilteredRoot, licenseFilteredRoot);

        // Assert that the 'Low' issue from 'testNoFilter' had been filtered
        Set<Issue> rootIssues = issuesFilteredRoot.processTreeIssues();
        assertEquals(0, issuesFilteredRoot.getIssueCount());
        assertEquals(0, rootIssues.size());

        // Assert that the license filtered tree have 1 license and one node except the root
        rootIssues = licenseFilteredRoot.processTreeIssues();
        assertEquals(1, licenseFilteredRoot.getIssueCount());
        assertEquals(1, rootIssues.size());
        assertEquals(1, licenseFilteredRoot.getChildren().get(0).getLicenses().size());
    }

    @Test(dependsOnMethods = {"testOneIssueFilter"})
    public void testManyIssueFilters() {
        // Filter 'Low' and 'Medium' issues
        severitiesFilters.replace(Severity.Medium, false);
        DependenciesTree issuesFilteredRoot = new DependenciesTree("0");
        DependenciesTree licenseFilteredRoot = new DependenciesTree("0");

        // Insert some issues
        two.setIssues(Sets.newHashSet(createIssue(Severity.Medium), createIssue(Severity.High)));
        four.setIssues(Sets.newHashSet(createIssue(Severity.Unknown)));
        five.setIssues(Sets.newHashSet(createIssue(Severity.Low)));
        filterManager.applyFilters(root, issuesFilteredRoot, licenseFilteredRoot);

        // Assert that the issues filtered tree have 2 issues (1 'High' and 1 'Unknown')
        Set<Issue> rootIssues = issuesFilteredRoot.processTreeIssues();
        assertEquals(2, issuesFilteredRoot.getIssueCount());
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

    @Test(dependsOnMethods = {"testNoFilter"})
    public void testOneLicenseFilter() {
        // Filter out all licenses
        licensesFilters.clear();
        DependenciesTree issuesFilteredRoot = new DependenciesTree("0");
        DependenciesTree licenseFilteredRoot = new DependenciesTree("0");
        filterManager.applyFilters(root, issuesFilteredRoot, licenseFilteredRoot);

        // Assert that the license in "1" have been filtered
        assertEquals(1, one.getLicenses().size());
        assertEquals(0, licenseFilteredRoot.getLicenses().size());
        assertEquals(0, licenseFilteredRoot.getChildren().size());
    }

    @Test(dependsOnMethods = {"testOneLicenseFilter"})
    public void testManyLicenseFilter() {
        // Accept 'MIT' and 'GPL' licenses
        licensesFilters.put(createLicense("MIT"), true);
        licensesFilters.put(createLicense("GPL"), true);

        // Insert some licenses
        two.setLicenses(Sets.newHashSet(createLicense("MIT"), createLicense("GNU")));
        three.setLicenses(Sets.newHashSet(createLicense("GNU")));
        five.setLicenses(Sets.newHashSet(createLicense("MIT")));
        DependenciesTree licensesFilteredRoot = new DependenciesTree("0");
        filterManager.applyFilters(root, new DependenciesTree("0"), licensesFilteredRoot);

        // Assert that the license filtered tree root has 2 children ("2" and "3")
        assertEquals(2, licensesFilteredRoot.getChildren().size());
        licensesFilteredRoot.processTreeIssues();

        // Check each one of the license filtered tree nodes
        assertEquals(0, licensesFilteredRoot.getLicenses().size());
        assertEquals(2, licensesFilteredRoot.getChildren().size());
        licensesFilteredRoot.getChildren().forEach(child -> {
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

}