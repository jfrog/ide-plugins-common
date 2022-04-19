package com.jfrog.ide.common.gradle;

import com.jfrog.GradleDependencyTree;
import com.jfrog.ide.common.TestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;
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
        DependencyTree dependencyTree = buildGradleDependencyTree();
        DependencyTree shared = getAndAssertSharedModule(dependencyTree);

        DependencyTree junit = TestUtils.getAndAssertChild(shared, "junit:junit:4.7");
        assertEquals(junit.getLicenses(), Sets.newHashSet(new License()));
        assertEquals(junit.getScopes(), Sets.newHashSet(new Scope("TestImplementation"), new Scope("TestRuntimeClasspath"), new Scope("TestCompileClasspath")));
        assertGeneralInfo(junit.getGeneralInfo(), "junit", "junit", "4.7", "");
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
        DependencyTree dependencyTree = buildGradleDependencyTree();
        DependencyTree shared = getAndAssertSharedModule(dependencyTree);

        DependencyTree missing = TestUtils.getAndAssertChild(shared, "missing:dependency:404 [unresolved]");
        assertEquals(missing.getLicenses(), Sets.newHashSet(new License()));
        assertTrue(missing.getScopes().contains(new Scope("TestImplementation")));
        assertGeneralInfo(missing.getGeneralInfo(), "missing", "dependency", "404", "");
    }

    private DependencyTree buildGradleDependencyTree() throws IOException {
        // Add path to gradle-dep-tree JAR to "pluginLibDir" environment variable, to be read in gradle-dep-tree.gradle init script
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("pluginLibDir", GradleDependencyTree.class.getProtectionDomain().getCodeSource().getLocation().getPath());

        GradleTreeBuilder gradleTreeBuilder = new GradleTreeBuilder(tempProject.toPath(), env, "");
        DependencyTree dependencyTree = gradleTreeBuilder.buildTree(new NullLog());
        assertNotNull(dependencyTree);

        assertEquals(dependencyTree.getUserObject(), tempProject.getName());
        assertEquals(dependencyTree.getChildren().size(), 3);
        return dependencyTree;
    }

    private DependencyTree getAndAssertSharedModule(DependencyTree root) {
        DependencyTree shared = TestUtils.getAndAssertChild(root, "shared");
        assertEquals(shared.getChildren().size(), 1);
        assertEquals(Sets.newHashSet(new License()), shared.getLicenses());
        assertEquals(Sets.newHashSet(new Scope()), shared.getScopes());
        assertGeneralInfo(shared.getGeneralInfo(), "", "shared", "", tempProject.toString());
        return shared;
    }

    private void assertGeneralInfo(GeneralInfo generalInfo, String groupId, String artifactId, String version, String path) {
        if (StringUtils.isBlank(groupId)) {
            assertEquals(generalInfo.getComponentId(), artifactId);
        } else {
            assertEquals(generalInfo.getComponentId(), groupId + ":" + artifactId + ":" + version);
        }
        assertEquals(generalInfo.getPath(), path);
        assertEquals(generalInfo.getPkgType(), "gradle");
    }
}
