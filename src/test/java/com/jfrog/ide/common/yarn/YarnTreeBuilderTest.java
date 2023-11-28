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

    public enum Project {
        EMPTY("package-name1", "empty"),
        DEPENDENCY("package-name2", "dependency"),
        EXAMPLE("example-yarn-package", "exampleYarnPackage");

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

    private YarnTreeBuilder createYarnTreeBuilder(Project project) throws IOException {
        tempProject = Files.createTempDirectory("ide-plugins-common-yarn").toFile();
        tempProject.deleteOnExit();
        FileUtils.copyDirectory((project).path.toFile(), tempProject);
        Path projectDir = tempProject.toPath();
        descriptorFilePath = projectDir.resolve("package.json").toString();
        return new YarnTreeBuilder(projectDir, descriptorFilePath, null, new NullLog());
    }

    @Test(dataProvider = "yarnTreeBuilderProvider")
    public void yarnTreeBuilderTest(Project project, int expectedChildren) throws IOException {
        YarnTreeBuilder yarnTreeBuilder = createYarnTreeBuilder(project);
        depTree = yarnTreeBuilder.buildTree();
        assertNotNull(depTree);
        String expectedProjectName = project.name;
        checkDependencyTree(expectedProjectName, expectedChildren);
    }

    private void checkDependencyTree(String expectedProjectName, int expectedChildren) {
        assertEquals(depTree.rootId(), expectedProjectName + ":0.0.1");
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
            DepTreeNode childNode = depTree.nodes().get(childId);
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
    public void extractMultiplePathsTest() {
        String projectRootId = "root";
        String packageFullName = "pkg:1.0.0";
        List<String> rawDependencyPaths = List.of(
                "Specified in \"dependencies\"",
                "Hoisted from \"jest-cli#node-notifier#minimist\"",
                "Hoisted from \"jest-cli#sane#minimist\"",
                "Hoisted from \"@test0#\\test1#test2#pkg\"",
                "This module exists because \"jest-cli#istanbul-api#mkdirp\" depends on it.",
                "This module exists because it's specified in \"devDependencies\"."
        );

        List<List<String>> expectedPaths = List.of(
                List.of(projectRootId, packageFullName),
                List.of(projectRootId, "jest-cli", "node-notifier", "minimist", packageFullName),
                List.of(projectRootId, "jest-cli", "sane", "minimist", packageFullName),
                List.of(projectRootId, "@test0", "\\test1", "test2", packageFullName),
                List.of(projectRootId, "jest-cli", "istanbul-api", "mkdirp", packageFullName),
                List.of(projectRootId, packageFullName)
        );
        YarnTreeBuilder yarnTreeBuilder = new YarnTreeBuilder(Paths.get(""), "", null, new NullLog());
        List<List<String>> actualPaths = yarnTreeBuilder.extractMultiplePaths(projectRootId, packageFullName, rawDependencyPaths);

        assertNotNull(actualPaths);
        assertEquals(actualPaths.size(), expectedPaths.size());
        for (List<String> path : actualPaths) {
            assertTrue(expectedPaths.contains(path));
        }
    }

    @DataProvider
    private Object[][] findDependencyImpactPathsProvider() {
        return new Object[][]{
                {Project.DEPENDENCY, "@types/node", Set.of("14.14.10"), List.of(List.of("package-name2", "@types/node:14.14.10"))},
                {Project.EXAMPLE, "lodash", Set.of("4.16.2"), List.of(List.of("example-yarn-package", "lodash:4.16.2"),
                        List.of("example-yarn-package", "jest-cli", "jest-runtime", "babel-core", "lodash:4.16.2"),
                        List.of("example-yarn-package", "jest-cli", "jest-runtime", "babel-core", "babel-register", "lodash:4.16.2"),
                        List.of("example-yarn-package", "jest-cli", "istanbul-lib-instrument", "babel-generator", "lodash:4.16.2"),
                        List.of("example-yarn-package", "jest-cli", "istanbul-lib-instrument", "babel-template", "lodash:4.16.2"),
                        List.of("example-yarn-package", "jest-cli", "istanbul-lib-instrument", "babel-traverse", "lodash:4.16.2"),
                        List.of("example-yarn-package", "jest-cli", "istanbul-lib-instrument", "babel-types", "lodash:4.16.2")
                )},
        };
    }

    @Test(dataProvider = "findDependencyImpactPathsProvider")
    public void findDependencyImpactPathsTest(Project project, String packageName, Set<String> packageVersions, List<List<String>> expectedPaths) throws IOException {
        String projectRootId = project.name;

        YarnTreeBuilder yarnTreeBuilder = createYarnTreeBuilder(project);
        Map<String, List<List<String>>> pathsMap = yarnTreeBuilder.findDependencyImpactPaths(projectRootId, packageName, packageVersions);

        assertNotNull(pathsMap);
        for (String packageVersion : packageVersions) {
            String packageFullName = packageName + ":" + packageVersion;
            assertTrue(pathsMap.containsKey(packageFullName));
            List<List<String>> paths = pathsMap.get(packageFullName);
            assertEquals(paths.size(), expectedPaths.size());
            for (List<String> path : paths) {
                assertTrue(expectedPaths.contains(path));
            }
        }
    }
}