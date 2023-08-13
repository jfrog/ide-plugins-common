package com.jfrog.ide.common.persistency;

import com.jfrog.ide.common.nodes.*;
import com.jfrog.ide.common.nodes.subentities.Cve;
import com.jfrog.ide.common.nodes.subentities.ResearchInfo;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SeverityReason;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScanCacheTest {
    private Path tempCacheDirPath;

    @BeforeMethod
    public void setUp(Object[] testArgs) throws IOException {
        tempCacheDirPath = Files.createTempDirectory("ide-plugins-common-test-cache");
        FileUtils.forceDeleteOnExit(tempCacheDirPath.toFile());
    }

    @AfterMethod
    public void tearDown() throws IOException {
        FileUtils.forceDelete(tempCacheDirPath.toFile());
    }

    @Test
    public void cacheTest() throws IOException {
        final String TEST_PROJECT_ID = "test-project-id";
        List<FileTreeNode> fileTreeNodes = getFileTreeNode();

        // Create a new cache and validate it's empty
        ScanCache cache = new ScanCache(TEST_PROJECT_ID, tempCacheDirPath, new NullLog());
        Assert.assertNull(cache.getScanCacheObject());

        // Save FileTreeNodes in cache and read them for it
        cache.cacheNodes(fileTreeNodes);
        ScanCache newCache = new ScanCache(TEST_PROJECT_ID, tempCacheDirPath, new NullLog());
        List<FileTreeNode> actual = newCache.getScanCacheObject().getFileTreeNodes();
        Assert.assertEquals(actual, fileTreeNodes);
    }

    private static List<FileTreeNode> getFileTreeNode() {
        List<FileTreeNode> fileTreeNodes = new ArrayList<>();
        DescriptorFileTreeNode descriptorFileTreeNode = new DescriptorFileTreeNode("path/to/descriptor.xml");
        VulnerabilityNode vulnerabilityNode = new VulnerabilityNode(
                "issueId",
                Severity.LowNotApplic,
                "summary",
                new ArrayList<>(List.of("fixedVersions")),
                new ArrayList<>(List.of("infectedVersions")),
                new Cve("cveId",
                        "cvssV2Score",
                        "cvssV2Vector",
                        "cvssV3Score",
                        "cvssV3Vector"),
                "lastUpdated",
                new ArrayList<>(List.of("watchNames")),
                new ArrayList<>(List.of("references")),
                new ResearchInfo(Severity.Low,
                        "shortDescription",
                        "fullDescription",
                        "remediation",
                        new ArrayList<>(List.of(new SeverityReason("name", "description", true)))),
                "ignoreRuleUrl");
        DependencyNode dependencyNode = new DependencyNode().componentId("componentId");
        dependencyNode.addIssue(vulnerabilityNode);
        descriptorFileTreeNode.addDependency(dependencyNode);
        FileTreeNode fileTreeNode = new FileTreeNode("path/to/file.txt");
        ApplicableIssueNode applicableIssueNode = new ApplicableIssueNode(
                "name",
                1,
                2,
                3,
                4,
                "filePath",
                "reason",
                "lineSnippet",
                "scannerSearchTarget",
                vulnerabilityNode,
                "ruleID"
        );
        vulnerabilityNode.updateApplicableInfo(applicableIssueNode);
        fileTreeNode.addIssue(applicableIssueNode);

        // The order of the adding to the list is important! We add the ApplicableIssueNode first to make sure its
        // 'issue' field is saved in the cache file as a UUID only (a pointer), even though the VulnerabilityNode that
        // it refers to is written later in the file.
        fileTreeNodes.add(fileTreeNode);
        fileTreeNodes.add(descriptorFileTreeNode);
        return fileTreeNodes;
    }
}
