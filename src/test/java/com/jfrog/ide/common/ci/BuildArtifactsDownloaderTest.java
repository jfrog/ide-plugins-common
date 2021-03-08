package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.utils.Sets;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Scope;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import static com.jfrog.ide.common.utils.Utils.createMapper;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author yahavi
 **/
public class BuildArtifactsDownloaderTest {
    private final ObjectMapper mapper = createMapper();

    @Test
    public void testCreateBuildDependencyTree() throws IOException, ParseException {
        try (InputStream io = getClass().getResourceAsStream("/ci/42-1614693966420.json")) {
            Build build = mapper.readValue(io, Build.class);
            assertNotNull(build);

            BuildArtifactsDownloader buildArtifactsDownloader = new BuildArtifactsDownloader(null, null, null, null, 0);
            DependencyTree actualBuildDependencyTree = buildArtifactsDownloader.createBuildDependencyTree(build);

            checkBuildInformation(actualBuildDependencyTree);
            DependencyTree actualModuleTree = checkAndGetModuleNode(actualBuildDependencyTree);
            DependencyTree directDependencyTree = checkAndGetDirectDependencyNode(actualModuleTree);
            checkAndGetTransitiveDependencyNode(directDependencyTree);
        }
    }

    private void checkBuildInformation(DependencyTree actualBuildDependencyTree) {
        String expectedBuildName = "npm-build";
        String expectedBuildNumber = "42";

        assertEquals(actualBuildDependencyTree.getUserObject(), expectedBuildName + "/" + expectedBuildNumber);
        assertEquals(actualBuildDependencyTree.getChildren().size(), 1);

        // Check general info
        BuildGeneralInfo buildGeneralInfo = (BuildGeneralInfo) actualBuildDependencyTree.getGeneralInfo();
        assertNotNull(buildGeneralInfo);
        assertEquals(buildGeneralInfo.getName(), expectedBuildName);
        assertEquals(buildGeneralInfo.getVersion(), expectedBuildNumber);
        assertEquals(buildGeneralInfo.getStatus(), BuildGeneralInfo.Status.PASSED);
        assertEquals(buildGeneralInfo.getStarted().getTime(), 1614693966420L);
        assertEquals(buildGeneralInfo.getPath(), "https://ecosysjfrog.jfrog.io/ui/pipelines/myPipelines/default/test_cli/460/Artifactory?branch=dev");

        // Check VCS
        Vcs vcs = buildGeneralInfo.getVcs();
        assertEquals(vcs.getRevision(), "9ec95ff3a9cf92d587bf8fa4f6f4d31b46f75d82");
        assertEquals(vcs.getBranch(), "master");
        assertEquals(vcs.getUrl(), "https://github.com/jfrog/project-examples.git");
        assertEquals(vcs.getMessage(), "Set oss.jfrog.org as the maven plugin example repo (#256)");
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

        DependencyTree actualDirectDependency = actualModule.getChildren().stream()
                .filter(node -> expectedDirectDependency.equals(node.getUserObject()))
                .findAny()
                .orElse(null);
        assertNotNull(actualDirectDependency, "Couldn't find direct dependency '" + expectedDirectDependency + "' between " + actualModule.getChildren());
        assertEquals(actualDirectDependency.getScopes(), Sets.newHashSet(new Scope("Production")));
        assertEquals(actualDirectDependency.getChildren().size(), 10);

        return actualDirectDependency;
    }

    private void checkAndGetTransitiveDependencyNode(DependencyTree actualDirectDependency) {
        DependencyTree actualTransitiveDependency = actualDirectDependency.getChildren().stream()
                .filter(node -> "etag:1.5.1".equals(node.getUserObject()))
                .findAny()
                .orElse(null);
        assertNotNull(actualTransitiveDependency, "Couldn't find direct dependency '" + actualTransitiveDependency + "' between " + actualDirectDependency.getChildren());
        assertEquals(actualTransitiveDependency.getScopes(), Sets.newHashSet(new Scope("Production")));
        assertEquals(actualTransitiveDependency.getChildren().size(), 1);
        assertEquals(actualTransitiveDependency.getChildAt(0).toString(), "crc:3.2.1");
    }
}
