package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.utils.Sets;
import org.jfrog.build.api.Build;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Scope;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static com.jfrog.ide.common.utils.Utils.createMapper;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author yahavi
 **/
public class BuildArtifactsDownloaderTest {
    private final ObjectMapper mapper = createMapper();

    @Test
    public void testCreateCiTree() throws IOException {
        try (InputStream io = getClass().getResourceAsStream("/ci/42-1614693966420.json")) {
            Build build = mapper.readValue(io, Build.class);
            assertNotNull(build);

            BuildArtifactsDownloader buildArtifactsDownloader = new BuildArtifactsDownloader(null, null, null, null, 0);

            DependencyTree actualBuildDependencyTree = checkAndGetBuildDependencyTree(buildArtifactsDownloader, build);
            DependencyTree actualModuleTree = checkAndGetModuleNode(actualBuildDependencyTree);
            DependencyTree directDependencyTree = checkAndGetDirectDependencyNode(actualModuleTree);
            checkAndGetTransitiveDependencyNode(directDependencyTree);
        }
    }

    private DependencyTree checkAndGetBuildDependencyTree(BuildArtifactsDownloader buildArtifactsDownloader, Build build) throws IOException {
        DependencyTree actualBuildDependencyTree = buildArtifactsDownloader.createBuildDependencyTree(build);

        String expectedCommitMessage = "Set oss.jfrog.org as the maven plugin example repo (#256)";
        String expectedBuildName = "npm-build";
        String expectedBuildNumber = "42";

        assertEquals(actualBuildDependencyTree.getUserObject(), expectedBuildName + "/" + expectedBuildNumber);
        assertEquals(actualBuildDependencyTree.getGeneralInfo().getName(), expectedBuildName);
        assertEquals(actualBuildDependencyTree.getGeneralInfo().getVersion(), expectedBuildNumber);
        assertEquals(actualBuildDependencyTree.getChildren().size(), 1);

        return actualBuildDependencyTree;
    }

    private DependencyTree checkAndGetModuleNode(DependencyTree actualCommitTree) {
        String expectedModuleId = "npm-example:0.0.3";

        DependencyTree actualModuleTree = actualCommitTree.getChildren().get(0);
        assertEquals(actualModuleTree.getUserObject(), expectedModuleId);
        assertEquals(actualModuleTree.getGeneralInfo().getComponentId(), expectedModuleId);
        assertEquals(actualModuleTree.getChildren().size(), 2);

        return actualModuleTree;
    }

    private DependencyTree checkAndGetDirectDependencyNode(DependencyTree actualModule) {
        String expectedDirectDependency = "send:0.10.0";
        Set<Scope> expectedScopes = Sets.newHashSet(new Scope("Production"));

        DependencyTree actualDirectDependency = actualModule.getChildren().stream()
                .filter(node -> expectedDirectDependency.equals(node.getUserObject()))
                .findAny()
                .orElse(null);
        assertNotNull(actualDirectDependency, "Couldn't find direct dependency '" + expectedDirectDependency + "' between " + actualModule.getChildren());
        assertEquals(actualDirectDependency.getScopes(), expectedScopes);
        assertEquals(actualDirectDependency.getChildren().size(), 10);

        return actualDirectDependency;
    }

    private void checkAndGetTransitiveDependencyNode(DependencyTree actualDirectDependency) {
        String expectedTransitiveDependency = "etag:1.5.1";
        Set<Scope> expectedScopes = Sets.newHashSet(new Scope("Production"));

        DependencyTree actualTransitiveDependency = actualDirectDependency.getChildren().stream()
                .filter(node -> expectedTransitiveDependency.equals(node.getUserObject()))
                .findAny()
                .orElse(null);
        assertNotNull(actualTransitiveDependency, "Couldn't find direct dependency '" + actualTransitiveDependency + "' between " + actualDirectDependency.getChildren());
        assertEquals(actualTransitiveDependency.getScopes(), expectedScopes);
        assertEquals(actualTransitiveDependency.getChildren().size(), 1);
        assertEquals(actualTransitiveDependency.getChildAt(0).toString(), "crc:3.2.1");
    }
}
