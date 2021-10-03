package com.jfrog.ide.common.gradle;

import com.jfrog.ide.common.TestUtils;
import org.apache.commons.io.FileUtils;
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

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
        assertEquals(junit.getScopes(), Sets.newHashSet(new Scope("Compile"), new Scope("Runtime")));
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
        assertEquals(Sets.newHashSet(new License()), missing.getLicenses());
        assertEquals(Sets.newHashSet(new Scope()), missing.getScopes());
        assertGeneralInfo(missing.getGeneralInfo(), "missing", "dependency", "404", "");
    }

    private DependencyTree buildGradleDependencyTree() throws IOException {
        GradleTreeBuilder gradleTreeBuilder = new GradleTreeBuilder(tempProject.toPath(), null, "");
        DependencyTree dependencyTree = gradleTreeBuilder.buildTree(new NullLog());
        assertNotNull(dependencyTree);

        assertEquals(dependencyTree.getUserObject(), tempProject.getName());
        assertEquals(5, dependencyTree.getChildren().size());
        return dependencyTree;
    }

    private DependencyTree getAndAssertSharedModule(DependencyTree root) {
        DependencyTree shared = TestUtils.getAndAssertChild(root, "shared");
        assertEquals(shared.getChildren().size(), 1);
        assertEquals(Sets.newHashSet(new License()), shared.getLicenses());
        assertEquals(Sets.newHashSet(new Scope()), shared.getScopes());
        assertGeneralInfo(shared.getGeneralInfo(), "org.jfrog.test.gradle.publish", "shared", "1.0-SNAPSHOT", tempProject.toString());
        return shared;
    }

    private void assertGeneralInfo(GeneralInfo generalInfo, String groupId, String artifactId, String version, String path) {
        assertEquals(generalInfo.getGroupId(), groupId);
        assertEquals(generalInfo.getArtifactId(), artifactId);
        assertEquals(generalInfo.getVersion(), version);
        assertEquals(generalInfo.getPath(), path);
        assertEquals(generalInfo.getPkgType(), "gradle");
    }
}