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

import static org.testng.Assert.*;

/**
 * Test correctness of DependencyTree for different npm projects.
 * The tests verify correctness before and after 'npm install' command.
 *
 * @author yahavi
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
        try {
            tempProject = Files.createTempDirectory("ide-plugins-common-yarn").toFile();
            tempProject.deleteOnExit();
            FileUtils.copyDirectory(((Project) testArgs[0]).path.toFile(), tempProject);
            Path projectDir = tempProject.toPath();
            descriptorFilePath = projectDir.resolve("package.json").toString();
            YarnTreeBuilder yarnTreeBuilder = new YarnTreeBuilder(projectDir, descriptorFilePath, null);
            depTree = yarnTreeBuilder.buildTree(new NullLog());
            assertNotNull(depTree);
        } catch (IOException e) {
            fail(e.getMessage(), e);
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
    public void yarnTreeBuilderTest(Project project, int expectedChildren) {
        if (!yarnDriver.isYarnInstalled()) {
            throw new SkipException("Skip test, yarn is not installed.");
        }
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
}