package com.jfrog.ide.common.yarn;

import com.google.common.collect.Sets;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Test correctness of DependencyTree for different yarn projects.
 * The tests verify correctness before and after 'yarn install' command.
 */
public class YarnTreeBuilderTest {
    private static final Path YARN_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "yarn"));

    enum Project {
        EMPTY("package-name1", "empty"),
        DEPENDENCY("package-name2", "dependency");

        private final String name;
        private final Path path;

        Project(String name, String path) {
            this.name = name;
            this.path = YARN_ROOT.resolve(path);
        }
    }

    private final YarnDriver yarnDriver = new YarnDriver(null);
    private DepTree depTree;
    private File tempProject;
    private String descriptorFilePath;

    @BeforeMethod
    public void setUp(Object[] testArgs) {
        if (!yarnDriver.isYarnInstalled()) {
            throw new SkipException("Skip test, yarn is not installed.");
        }
    }

    @AfterMethod
    public void tearDown() {
        FileUtils.deleteQuietly(tempProject);
    }

    @DataProvider
    private Object[][] yarnTreeBuilderProvider() {
        return new Object[][]{
                {Project.EMPTY, 0},
                {Project.DEPENDENCY, 4},
        };
    }

    @Test(dataProvider = "yarnTreeBuilderProvider")
    public void yarnTreeBuilderTest(Project project, int expectedChildren) throws IOException {
        tempProject = Files.createTempDirectory("ide-plugins-common-yarn").toFile();
        tempProject.deleteOnExit();
        FileUtils.copyDirectory((project).path.toFile(), tempProject);
        Path projectDir = tempProject.toPath();
        descriptorFilePath = projectDir.resolve("package.json").toString();
        YarnTreeBuilder yarnTreeBuilder = new YarnTreeBuilder(projectDir, descriptorFilePath, null);
        depTree = yarnTreeBuilder.buildTree(new NullLog());
        assertNotNull(depTree);
        String expectedProjectName = project.name;
        checkDependencyTree(expectedProjectName, expectedChildren);
    }

    private void checkDependencyTree(String expectedProjectName, int expectedChildren) {
        assertEquals(depTree.getRootId(), expectedProjectName + ":0.0.1");
        DepTreeNode rootNode = depTree.getRootNode();
        assertNotNull(rootNode);
        assertEquals(rootNode.getDescriptorFilePath(), descriptorFilePath);
        assertEquals(rootNode.getChildren().size(), expectedChildren);
        if (expectedChildren == 4) {
            fourChildrenScenario();
        }
    }

    private void fourChildrenScenario() {
        DepTreeNode rootNode = depTree.getRootNode();
        int count = 0;
        for (String childId : rootNode.getChildren()) {
            DepTreeNode childNode = depTree.getNodes().get(childId);
            switch (childId) {
                case "progress:2.0.3":
                case "has-flag:3.0.0":
                    count++;
                    break;
                case "@ungap/promise-all-settled:1.1.2":
                    assertEquals(childNode.getScopes(), Sets.newHashSet("ungap"));
                    count++;
                    break;
                case "@types/node:14.14.10":
                    assertEquals(childNode.getScopes(), Sets.newHashSet("types"));
                    count++;
                    break;
                default:
                    fail("Unexpected dependency " + childId);
            }
        }
        assertEquals(count, 4);
    }

    @Test
    public void extractSinglePathTest() {
        String projectRootId = "root";
        String packageFullName = "pkg:1.0.0";
        String rawDependency = "{\"type\":\"info\",\"data\":\"This module exists because \\\"pkg#subpkg#dep\\\" depends on it.\"}";

        YarnTreeBuilder yarnTreeBuilder = new YarnTreeBuilder(Paths.get(""), "", null);
        List<String> pathResult = yarnTreeBuilder.extractSinglePath(projectRootId, packageFullName, rawDependency);

        assertNotNull(pathResult);
        assertEquals(pathResult.size(), 2);
        assertEquals(pathResult.get(0), projectRootId);
        assertEquals(pathResult.get(1), packageFullName);
    }

    @Test
    public void extractMultiplePathsTest() {
        String projectRootId = "root";
        String packageFullName = "pkg:1.0.0";
        List<String> rawDependencyPaths = List.of(
                "{\"type\":\"reasons\",\"items\":[\"Specified in \\\"dependencies\\\"\",\"Hoisted from \\\"pkg#dep1\\\"\",\"Hoisted from \\\"pkg#dep2\\\"\"]}",
                "{\"type\":\"reasons\",\"items\":[\"Specified in \\\"devDependencies\\\"\",\"Hoisted from \\\"pkg#dep3\\\"\"]}"
        );

        YarnTreeBuilder yarnTreeBuilder = new YarnTreeBuilder(Paths.get(""), "", null);
        List<List<String>> paths = yarnTreeBuilder.extractMultiplePaths(projectRootId, packageFullName, rawDependencyPaths);

        assertNotNull(paths);
        assertEquals(paths.size(), 2);
        assertEquals(paths.get(0).size(), 4);
        assertEquals(paths.get(0).get(0), projectRootId);
        assertEquals(paths.get(0).get(1), packageFullName);
        assertEquals(paths.get(0).get(2), "pkg#dep1");
        assertEquals(paths.get(0).get(3), "pkg#dep2");

        assertEquals(paths.get(1).size(), 3);
        assertEquals(paths.get(1).get(0), projectRootId);
        assertEquals(paths.get(1).get(1), packageFullName);
        assertEquals(paths.get(1).get(2), "pkg#dep3");
    }

    @Test
    public void findDependencyImpactPathsTest() throws IOException {
        String projectRootId = "root";
        String packageName = "pkg";
        Set<String> packageVersions = Set.of("1.0.0", "2.0.0");
        List<String> yarnWhyOutput = List.of(
                "{\"type\":\"info\",\"data\":\"Found \\\"pkg@1.0.0\\\"\"}",
                "{\"type\":\"list\",\"data\":{\"type\":\"reasons\",\"items\":[\"Specified in \\\"dependencies\\\"\",\"Hoisted from \\\"pkg#dep1\\\"\",\"Hoisted from \\\"pkg#dep2\\\"\"]}}"
        );

        YarnTreeBuilder yarnTreeBuilder = new YarnTreeBuilder(Paths.get(""), "", null);
        Map<String, List<List<String>>> paths = yarnTreeBuilder.findDependencyImpactPaths(new NullLog(), projectRootId, packageName, packageVersions);

        assertNotNull(paths);
        assertEquals(paths.size(), 1);

        List<List<String>> pkgPaths = paths.get("pkg:1.0.0");
        assertNotNull(pkgPaths);
        assertEquals(pkgPaths.size(), 1);

        List<String> singlePath = pkgPaths.get(0);
        assertEquals(singlePath.size(), 4);
        assertEquals(singlePath.get(0), projectRootId);
        assertEquals(singlePath.get(1), "pkg:1.0.0");
        assertEquals(singlePath.get(2), "pkg#dep1");
        assertEquals(singlePath.get(3), "pkg#dep2");
    }
}