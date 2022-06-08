package com.jfrog.ide.common.yarn;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;
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
    private DependencyTree dependencyTree;
    private File tempProject;

    @BeforeMethod
    public void setUp(Object[] testArgs) {
        try {
            tempProject = Files.createTempDirectory("ide-plugins-common-yarn").toFile();
            tempProject.deleteOnExit();
            FileUtils.copyDirectory(((Project) testArgs[0]).path.toFile(), tempProject);
            YarnTreeBuilder yarnTreeBuilder = new YarnTreeBuilder(tempProject.toPath(), null);
            dependencyTree = yarnTreeBuilder.buildTree(new NullLog(), false);
            assertNotNull(dependencyTree);
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
        checkGeneralInfo(dependencyTree.getGeneralInfo(), expectedProjectName, tempProject);
        assertEquals(dependencyTree.getChildren().size(), expectedChildren);
        switch (expectedChildren) {
            case 0:
                noChildrenScenario(dependencyTree);
                break;
            case 4:
                fourChildrenScenario(dependencyTree, expectedProjectName);
        }
    }

    private void checkGeneralInfo(GeneralInfo actual, String name, File path) {
        assertNotNull(actual);
        assertEquals(actual.getComponentId(), name + ":" + "0.0.1");
        assertEquals(actual.getPath(), path.toString());
        assertEquals(actual.getPkgType(), "yarn");
        assertEquals(actual.getArtifactId(), name);
        assertEquals(actual.getVersion(), "0.0.1");
    }

    private void noChildrenScenario(DependencyTree dependencyTree) {
        assertTrue(dependencyTree.isLeaf());
    }

    private void fourChildrenScenario(DependencyTree dependencyTree, String expectedProjectName) {
        int count = 0;
        for (DependencyTree child : dependencyTree.getChildren()) {
            switch (child.toString()) {
                case "progress:2.0.3":
                    count++;
                    break;
                case "has-flag:3.0.0":
                    count++;
                    break;
                case "@ungap/promise-all-settled:1.1.2":
                    assertEquals(child.getScopes(), Sets.newHashSet(new Scope("Ungap")));
                    count++;
                    break;
                case "@types/node:14.14.10":
                    assertEquals(child.getScopes(), Sets.newHashSet(new Scope("types")));
                    count++;
                    break;
                default:
                    fail("Unexpected dependency " + child);
            }
            assertEquals(child.getParent().toString(), expectedProjectName);
        }
        assertEquals(count, 4);
    }
}