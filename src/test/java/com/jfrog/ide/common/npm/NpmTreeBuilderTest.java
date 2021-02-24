package com.jfrog.ide.common.npm;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;
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
 * Test correctness of DependencyTree for different npm projects.
 * The tests verifies correctness before and after 'npm install' command.
 *
 * @author yahavi
 */
public class NpmTreeBuilderTest {

    private static final Path NPM_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "npm"));
    private static final DependencyTree progress = new DependencyTree("progress:2.0.3");
    private static final DependencyTree debug = new DependencyTree("debug:4.1.1");

    static {
        progress.setScopes(Sets.newHashSet(new Scope("production")));
        debug.setScopes(Sets.newHashSet(new Scope("development")));
    }

    enum Project {
        EMPTY("package-name1", "empty"),
        DEPENDENCY("package-name2", "dependency", progress, debug),
        DEPENDENCY_PACKAGE_LOCK("package-name3", "dependencyPackageLock", progress, debug);

        private final DependencyTree[] children;
        private final String name;
        private final Path path;

        Project(String name, String path, DependencyTree... children) {
            this.name = name;
            this.path = NPM_ROOT.resolve(path);
            this.children = children;
        }
    }

    private final NpmDriver npmDriver = new NpmDriver(null);
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
    private void npmTreeBuilderTest(Project project, boolean install, boolean expectChildren) {
        try {
            if (install) {
                npmDriver.install(tempProject, Lists.newArrayList(), null);
            }
            NpmTreeBuilder npmTreeBuilder = new NpmTreeBuilder(tempProject.toPath(), null);
            DependencyTree DependencyTree = npmTreeBuilder.buildTree(new NullLog());
            assertNotNull(DependencyTree);
            String projectName = project.name;
            if (!install && ArrayUtils.isNotEmpty(project.children)) {
                projectName += " (Not installed)";
            }
            checkGeneralInfo(DependencyTree.getGeneralInfo(), projectName, tempProject);
            if (!expectChildren) {
                assertTrue(DependencyTree.isLeaf());
                return;
            }
            assertEquals(DependencyTree.getChildren().size(), 2);
            for (DependencyTree child : DependencyTree.getChildren()) {
                assertEquals(child.getScopes().size(), 1);
                switch (child.toString()) {
                    case "progress:2.0.3":
                        assertEquals(child.getScopes().toArray()[0], new Scope("production"));
                        break;
                    case "debug:4.1.1":
                        assertEquals(child.getScopes().toArray()[0], new Scope("development"));
                        break;
                    default:
                        fail("Unexpected dependency " + child.toString());
                }
                assertEquals(child.getParent().toString(), projectName);
            }
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