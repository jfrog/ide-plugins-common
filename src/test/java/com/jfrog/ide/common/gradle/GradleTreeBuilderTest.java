package com.jfrog.ide.common.gradle;

import com.jfrog.GradleDependencyNode;
import com.jfrog.ide.common.TestUtils;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeModule;
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

    @BeforeMethod
    public void setUp(Object[] args) throws IOException {
        tempProject = Files.createTempDirectory("ide-plugins-common-gradle").toFile();
        tempProject.deleteOnExit();
        FileUtils.copyDirectory(GRADLE_ROOT.resolve((String) args[0]).toFile(), tempProject);
    }

    @AfterMethod
    public void tearDown() {
        FileUtils.deleteQuietly(tempProject);
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

    /**
     * Data provider for a project whose modules resolve the same dependency with different transitive
     * dependencies - 'modb' excludes 'commons-lang3' from 'commons-text', 'moda' doesn't.
     *
     * @return 'sharedDependency'.
     */
    @DataProvider
    private Object[][] gradleTreeBuilderSharedDependencyProvider() {
        return new Object[][]{{"sharedDependency"}};
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "gradleTreeBuilderSharedDependencyProvider")
    public void gradleTreeBuilderSharedDependencyTest(String projectPath) throws IOException {
        DepTree depTree = buildGradleDependencyTree(projectPath);

        DepTreeNode commonsText = depTree.nodes().get("org.apache.commons:commons-text:1.9");
        assertNotNull(commonsText, "Couldn't find node 'org.apache.commons:commons-text:1.9'.");
        assertTrue(commonsText.getChildren().contains("org.apache.commons:commons-lang3:3.11"),
                "The dependency resolved in 'moda' was dropped by the module of 'modb': " + commonsText.getChildren());
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "gradleTreeBuilderSharedDependencyProvider")
    public void gradleTreeBuilderModuleScopesTest(String projectPath) throws IOException {
        DepTree depTree = buildGradleDependencyTree(projectPath);

        Map<String, DepTreeModule> modulesByRoot = new HashMap<>();
        for (DepTreeModule module : depTree.modules()) {
            modulesByRoot.put(module.rootId(), module);
        }
        assertEquals(modulesByRoot.size(), 3);

        DepTreeModule moda = modulesByRoot.get("org.jfrog.test.gradle.shared:moda:1.0-SNAPSHOT");
        assertNotNull(moda, "Couldn't find the 'moda' module scope in " + modulesByRoot.keySet());
        assertTrue(moda.nodes().get("org.apache.commons:commons-text:1.9").getChildren()
                        .contains("org.apache.commons:commons-lang3:3.11"),
                "'moda' resolves commons-lang3 through commons-text");

        DepTreeModule modb = modulesByRoot.get("org.jfrog.test.gradle.shared:modb:1.0-SNAPSHOT");
        assertNotNull(modb, "Couldn't find the 'modb' module scope in " + modulesByRoot.keySet());
        assertFalse(modb.nodes().containsKey("org.apache.commons:commons-lang3:3.11"),
                "'modb' excludes commons-lang3, so its scope must not contain it");
    }

    private DepTree buildGradleDependencyTree(String projectPath) throws IOException {
        // Add path to gradle-dep-tree JAR to "pluginLibDir" environment variable, to be read in gradle-dep-tree.gradle init script
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("pluginLibDir", GradleDependencyNode.class.getProtectionDomain().getCodeSource().getLocation().getPath());

        Path projectDir = tempProject.toPath();
        String descriptorFileName = "build.gradle";
        if (projectPath.toLowerCase().contains("kotlin")) {
            descriptorFileName += ".kts";
        }
        String descriptorFilePath = projectDir.resolve(descriptorFileName).toString();
        GradleTreeBuilder gradleTreeBuilder = new GradleTreeBuilder(projectDir, descriptorFilePath, env, "");
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
