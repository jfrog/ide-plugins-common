package com.jfrog.ide.common.gradle;

import com.jfrog.GradleDependencyNode;
import com.jfrog.ide.common.TestUtils;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.Scope;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Sets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Test correctness of DependencyTree for different Gradle projects.
 *
 * @author yahavi
 */
public class GradleTreeBuilderTest {

    private static final Path GRADLE_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "gradle"));
    private File tempProject;
    private File tempGradleUserHome;

    @BeforeMethod
    public void setUp(Object[] args) throws IOException {
        tempProject = Files.createTempDirectory("ide-plugins-common-gradle").toFile();
        tempProject.deleteOnExit();
        FileUtils.copyDirectory(GRADLE_ROOT.resolve((String) args[0]).toFile(), tempProject);
        tempGradleUserHome = Files.createTempDirectory("ide-plugins-common-gradle-user-home").toFile();
        tempGradleUserHome.deleteOnExit();
    }

    @AfterMethod
    public void tearDown() {
        FileUtils.deleteQuietly(tempProject);
        FileUtils.deleteQuietly(tempGradleUserHome);
    }

    @DataProvider
    private Object[][] gradleTreeBuilderProvider() {
        return new Object[][]{{"groovy"}, {"kotlin"}};
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "gradleTreeBuilderProvider")
    public void gradleTreeBuilderTest(String projectPath) throws IOException {
        DepTree depTree = buildGradleDependencyTree(projectPath);
        DepTreeNode shared = getAndAssertSharedModule(depTree);

        DepTreeNode junit = TestUtils.getAndAssertChild(depTree, shared, "junit:junit:4.7");
        assertEquals(junit.getScopes(), Sets.newHashSet("testImplementation", "testRuntimeClasspath", "testCompileClasspath"));
    }

    /**
     * Data provider for projects with a missing dependency. The missing dependency ID is: 'missing:dependency:404'.
     *
     * @return 'unresolvedGroovy' and 'unresolvedKotlin'.
     */
    @DataProvider
    private Object[][] gradleTreeBuilderUnresolvedProvider() {
        return new Object[][]{{"unresolvedGroovy"}, {"unresolvedKotlin"}};
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "gradleTreeBuilderUnresolvedProvider")
    public void gradleTreeBuilderUnresolvedTest(String projectPath) throws IOException {
        DepTree depTree = buildGradleDependencyTree(projectPath);
        DepTreeNode shared = getAndAssertSharedModule(depTree);

        DepTreeNode missing = TestUtils.getAndAssertChild(depTree, shared, "missing:dependency:404");
        assertTrue(missing.getScopes().contains("testImplementation"));
    }

    private DepTree buildGradleDependencyTree(String projectPath) throws IOException {
        // Add path to gradle-dep-tree JAR to "pluginLibDir" environment variable, to be read in gradle-dep-tree.gradle init script
        Map<String, String> env = new HashMap<>();
        env.put("pluginLibDir", GradleDependencyNode.class.getProtectionDomain().getCodeSource().getLocation().getPath());

        Path projectDir = tempProject.toPath();
        String descriptorFileName = "build.gradle";
        if (projectPath.toLowerCase().contains("kotlin")) {
            descriptorFileName += ".kts";
        }
        String descriptorFilePath = projectDir.resolve(descriptorFileName).toString();
        GradleTreeBuilder gradleTreeBuilder = new GradleTreeBuilder(projectDir, descriptorFilePath, env, "", tempGradleUserHome.toPath());
        DepTree depTree = gradleTreeBuilder.buildTree(new NullLog());
        assertNotNull(depTree);

        assertEquals(depTree.rootId(), tempProject.getName());
        assertEquals(depTree.getRootNodeDescriptorFilePath(), descriptorFilePath);
        assertEquals(depTree.getRootNode().getChildren().size(), 3);
        return depTree;
    }

    private DepTreeNode getAndAssertSharedModule(DepTree depTree) {
        final String COMP_ID = "org.jfrog.test.gradle.publish:shared:1.0-SNAPSHOT";
        assertTrue(depTree.getRootNode().getChildren().contains(COMP_ID));
        DepTreeNode shared = depTree.nodes().get(COMP_ID);
        assertNotNull(shared, "Couldn't find node '" + COMP_ID + "'.");
        assertEquals(shared.getChildren().size(), 1);
        assertNotEquals(Sets.newHashSet(new Scope()), shared.getScopes());
        return shared;
    }
}
