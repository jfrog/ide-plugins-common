package com.jfrog.ide.common.npm;

import com.google.common.collect.Sets;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.Version;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Test correctness of DependencyTree for different npm projects.
 * The tests verify correctness before and after 'npm install' command.
 *
 * @author yahavi
 */
public class NpmTreeBuilderTest {
    private static final Path NPM_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "npm"));

    private DepTree depTree;

    private final NpmDriver npmDriver = new NpmDriver(null);
    private final boolean isNpm7 = isNpm7();
    private String descriptorFilePath;
    private File tempProject;

    @BeforeMethod
    public void setUp(Object[] testArgs) {
        try {
            tempProject = Files.createTempDirectory("ide-plugins-common-npm").toFile();
            tempProject.deleteOnExit();
            FileUtils.copyDirectory(((Project) testArgs[0]).path.toFile(), tempProject);

            // If true, the test require to run "npm install"
            if ((Boolean) testArgs[1]) {
                npmDriver.install(tempProject, Lists.newArrayList(), null);
            }
            Path projectDir = tempProject.toPath();
            descriptorFilePath = projectDir.resolve("package.json").toString();
            NpmTreeBuilder npmTreeBuilder = new NpmTreeBuilder(projectDir, descriptorFilePath, null);
            depTree = npmTreeBuilder.buildTree(new NullLog());
            assertNotNull(depTree);
        } catch (IOException e) {
            fail(e.getMessage(), e);
        }
    }

    @DataProvider
    private Object[][] npm6TreeBuilderProvider() {
        return new Object[][]{
                {Project.EMPTY, false, 0},
                {Project.EMPTY, true, 0},
                {Project.DEPENDENCY, false, 0},
                {Project.DEPENDENCY, true, 2},
                {Project.DEPENDENCY_PACKAGE_LOCK, false, 2},
                {Project.DEPENDENCY_PACKAGE_LOCK, true, 2},
                {Project.DEV_AND_PROD, false, 0},
                {Project.DEV_AND_PROD, true, 1},
        };
    }

    @AfterMethod
    public void tearDown() {
        FileUtils.deleteQuietly(tempProject);
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "npm6TreeBuilderProvider")
    public void npm6TreeBuilderTest(Project project, boolean install, int expectedChildren) {
        if (isNpm7()) {
            throw new SkipException("Skip test on npm >= 7");
        }

        String expectedProjectId = project.packageId;
        checkDependencyTree(expectedProjectId, expectedChildren);
    }

    @DataProvider
    private Object[][] npm7TreeBuilderProvider() {
        return new Object[][]{
                {Project.EMPTY, true, 0},
                {Project.DEPENDENCY, false, 0},
                {Project.DEPENDENCY, true, 2},
                {Project.DEPENDENCY_PACKAGE_LOCK, false, 2},
                {Project.DEPENDENCY_PACKAGE_LOCK, true, 2},
                {Project.DEV_AND_PROD, false, 0},
                {Project.DEV_AND_PROD, true, 1},
        };
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "npm7TreeBuilderProvider")
    public void npm7TreeBuilderTest(Project project, boolean install, int expectedChildren) {
        if (!isNpm7()) {
            throw new SkipException("Skip test on npm < 7");
        }
        String expectedProjectId = project.packageId;
        boolean packageLockExist = Files.exists(tempProject.toPath().resolve("package-lock.json"));
        checkDependencyTree(expectedProjectId, expectedChildren);
    }

    private void checkDependencyTree(String expectedProjectId, int expectedChildren) {
        assertNotNull(depTree);
        assertEquals(depTree.rootId(), expectedProjectId);
        DepTreeNode rootNode = depTree.getRootNode();
        assertNotNull(rootNode);
        assertEquals(rootNode.getDescriptorFilePath(), descriptorFilePath);
        assertEquals(rootNode.getChildren().size(), expectedChildren);
        switch (expectedChildren) {
            case 1:
                oneChildScenario();
                break;
            case 2:
                twoChildrenScenario();
        }
    }

    private void oneChildScenario() {
        DepTreeNode rootNode = depTree.getRootNode();
        String childId = rootNode.getChildren().stream().findFirst().orElse(null);
        assertEquals("progress:2.0.3", childId);
        DepTreeNode childNode = depTree.nodes().get(childId);
        assertNotNull(childNode);
        Set<String> expectedScopes = Sets.newHashSet("dev");
        // If using npm 6, the dependency may be either in dev and prod scopes
        if (!isNpm7) {
            expectedScopes.add("prod");
        }
        assertEquals(childNode.getScopes(), expectedScopes);
    }

    private void twoChildrenScenario() {
        DepTreeNode rootNode = depTree.getRootNode();
        for (String childId : rootNode.getChildren()) {
            DepTreeNode childNode = depTree.nodes().get(childId);
            switch (childId) {
                case "progress:2.0.3":
                    assertTrue(childNode.getScopes().contains("prod"));
                    break;
                case "debug:4.1.1":
                    assertEquals(childNode.getScopes(), Sets.newHashSet("dev"));
                    break;
                default:
                    fail("Unexpected dependency " + childId);
            }
        }
    }

    enum Project {
        EMPTY("package-name1:0.0.1", "empty"),
        DEPENDENCY("package-name2:0.0.1", "dependency"),
        DEPENDENCY_PACKAGE_LOCK("package-name3:0.0.1", "dependencyPackageLock"),
        DEV_AND_PROD("package-name4:0.0.1", "devAndProd");

        private final String packageId;
        private final Path path;

        Project(String packageId, String path) {
            this.packageId = packageId;
            this.path = NPM_ROOT.resolve(path);
        }
    }

    private boolean isNpm7() {
        try {
            Version version = new Version(npmDriver.version(new File(".")));
            return version.isAtLeast(new Version("7.0.0"));
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}