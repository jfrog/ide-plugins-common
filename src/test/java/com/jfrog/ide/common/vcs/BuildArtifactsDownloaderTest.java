package com.jfrog.ide.common.vcs;

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
    public void testCreateVcsTree() throws IOException {
        try (InputStream io = getClass().getResourceAsStream("/vcs/42-1614693966420.json")) {
            Build build = mapper.readValue(io, Build.class);
            assertNotNull(build);

            BuildArtifactsDownloader buildArtifactsDownloader = new BuildArtifactsDownloader(null, null, null, null, 0);
            VcsDependencyTree actualVcsDependencyTree = buildArtifactsDownloader.createVcsDependencyTree(build);
            checkBranchNode(actualVcsDependencyTree);

            DependencyTree actualCommitTree = checkAndGetCommitNode(actualVcsDependencyTree);
            DependencyTree actualModuleTree = checkAndGetModuleNode(actualCommitTree);
            DependencyTree directDependencyTree = checkAndGetDirectDependencyNode(actualModuleTree);
            checkAndGetTransitiveDependencyNode(directDependencyTree);
        }
    }

    private void checkBranchNode(VcsDependencyTree actualVcsDependencyTree) {
        String expectedBranch = "master";

        assertEquals(actualVcsDependencyTree.getBranch(), expectedBranch);
    }

    private DependencyTree checkAndGetCommitNode(VcsDependencyTree actualVcsDependencyTree) {
        String expectedCommitMessage = "Set oss.jfrog.org as the maven plugin example repo (#256)";
        String expectedBuildName = "npm-build";
        String expectedBuildNumber = "42";

        DependencyTree actualCommitTree = actualVcsDependencyTree.getBranchDependencyTree();
        assertEquals(actualCommitTree.getUserObject(), expectedCommitMessage);
        assertEquals(actualCommitTree.getGeneralInfo().getName(), expectedBuildName);
        assertEquals(actualCommitTree.getGeneralInfo().getVersion(), expectedBuildNumber);
        assertEquals(actualCommitTree.getChildren().size(), 1);

        return actualCommitTree;
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
