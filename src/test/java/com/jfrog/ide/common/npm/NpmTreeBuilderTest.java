package com.jfrog.ide.common.npm;

import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
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

import static org.testng.Assert.*;

/**
 * Test correctness of DependenciesTree for different npm projects.
 * The tests verifies correctness before and after 'npm install' command.
 *
 * @author yahavi
 */
public class NpmTreeBuilderTest {

    private static final Path NPM_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "npm"));

    enum Project {
        EMPTY("package-name1", null, "empty"),
        DEPENDENCY("package-name2", new DependenciesTree("progress:2.0.3"), "dependency"),
        DEPENDENCY_PACKAGE_LOCK("package-name3", new DependenciesTree("progress:2.0.3"), "dependencyPackageLock");

        private DependenciesTree child;
        private String name;
        private Path path;

        Project(String name, DependenciesTree child, String path) {
            this.name = name;
            this.child = child;
            this.path = NPM_ROOT.resolve(path);
        }
    }

    private NpmDriver npmDriver = new NpmDriver("", null);
    private File tempProject;

    @BeforeMethod
    public void setUp(Object[] testArgs) {
        try {
            tempProject = Files.createTempDirectory("ide-plugins-common-npm").toFile();
            tempProject.deleteOnExit();
            FileUtils.copyDirectory(((Project) testArgs[0]).path.toFile(), tempProject);
        } catch (IOException e) {
            fail(e.getMessage(), e);
        }
    }

    @AfterMethod
    public void tearDown() {
        FileUtils.deleteQuietly(tempProject);
    }

    @DataProvider
    private Object[][] npmTreeBuilderProvider() {
        return new Object[][]{
                {Project.EMPTY, false, false},
                {Project.EMPTY, true, false},
                {Project.DEPENDENCY, false, false},
                {Project.DEPENDENCY, true, true},
                {Project.DEPENDENCY_PACKAGE_LOCK, false, true},
                {Project.DEPENDENCY_PACKAGE_LOCK, true, true},
        };
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "npmTreeBuilderProvider")
    private void npmTreeBuilderTest(Project project, boolean install, boolean expectChild) {
        try {
            if (install) {
                npmDriver.install(tempProject, Lists.newArrayList(), null);
            }
            NpmTreeBuilder npmTreeBuilder = new NpmTreeBuilder(tempProject.toPath(), null);
            DependenciesTree dependenciesTree = npmTreeBuilder.buildTree(new NullLog());
            assertNotNull(dependenciesTree);
            String projectName = project.name;
            if (!install && project.child != null) {
                projectName += " (Not installed)";
            }
            checkGeneralInfo(dependenciesTree.getGeneralInfo(), projectName, tempProject);
            if (!expectChild) {
                assertTrue(dependenciesTree.isLeaf());
                return;
            }
            DependenciesTree child = (DependenciesTree) dependenciesTree.getFirstChild();
            assertEquals(child.toString(), "progress:2.0.3");
            assertEquals(child.getParent().toString(), projectName);
        } catch (IOException e) {
            fail(e.getMessage(), e);
        }
    }

    private void checkGeneralInfo(GeneralInfo actual, String name, File path) {
        assertNotNull(actual);
        assertEquals(actual.getComponentId(), name + ":" + "0.0.1");
        assertEquals(actual.getPath(), path.toString());
        assertEquals(actual.getPkgType(), "npm");
        assertEquals(actual.getArtifactId(), name);
        assertEquals(actual.getVersion(), "0.0.1");
    }
}