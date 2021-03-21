package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Sets;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Scope;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import static com.jfrog.ide.common.utils.Utils.createMapper;
import static org.testng.Assert.*;

/**
 * @author yahavi
 **/
public class BuildArtifactsDownloaderTest {
    private final ObjectMapper mapper = createMapper();

    @Test
    public void testCreateBuildDependencyTree() throws IOException, ParseException {
        try (InputStream io = getClass().getResourceAsStream("/ci/artifactory-build.json")) {
            Build build = mapper.readValue(io, Build.class);
            assertNotNull(build);

            BuildArtifactsDownloader buildArtifactsDownloader = new BuildArtifactsDownloader(null, null, null, null, 0, new NullLog());
            DependencyTree actualBuildDependencyTree = buildArtifactsDownloader.createBuildDependencyTree(build);

            checkBuildInformation(actualBuildDependencyTree);
            DependencyTree actualModuleTree = checkAndGetModuleNode(actualBuildDependencyTree);
            checkArtifactsNode(actualModuleTree);
            DependencyTree directDependencyTree = checkAndGetDirectDependenciesNode(actualModuleTree);
            checkAndGetTransitiveDependencyNode(directDependencyTree);
        }
    }

    private void checkBuildInformation(DependencyTree actualBuildDependencyTree) {
        String expectedBuildName = "maven-build";
        String expectedBuildNumber = "1";

        assertEquals(actualBuildDependencyTree.getUserObject(), expectedBuildName + "/" + expectedBuildNumber);
        assertEquals(actualBuildDependencyTree.getChildren().size(), 4);

        // Check general info
        BuildGeneralInfo buildGeneralInfo = (BuildGeneralInfo) actualBuildDependencyTree.getGeneralInfo();
        assertNotNull(buildGeneralInfo);
        assertEquals(buildGeneralInfo.getName(), expectedBuildName);
        assertEquals(buildGeneralInfo.getVersion(), expectedBuildNumber);
        assertEquals(buildGeneralInfo.getStatus(), BuildGeneralInfo.Status.PASSED);
        assertEquals(buildGeneralInfo.getStarted().getTime(), 1615993718989L);
        assertEquals(buildGeneralInfo.getPath(), "https://bob.jfrog.io/ui/pipelines/myPipelines/default/maven_build/482/StepName");

        // Check VCS
        Vcs vcs = buildGeneralInfo.getVcs();
        assertEquals(vcs.getRevision(), "9ec95ff3a9cf92d587bf8fa4f6f4d31b46f75d82");
        assertEquals(vcs.getBranch(), "master");
        assertEquals(vcs.getUrl(), "https://github.com/jfrog/project-examples.git");
        assertEquals(vcs.getMessage(), "Set oss.jfrog.org as the maven plugin example repo (#256)");
    }

    private DependencyTree checkAndGetModuleNode(DependencyTree actualCommitTree) {
        String expectedModuleId = "org.jfrog.test:multi1:3.7-SNAPSHOT";

        DependencyTree actualModuleTree = getAndAssertChild(actualCommitTree, expectedModuleId);
        assertEquals(actualModuleTree.getGeneralInfo().getComponentId(), expectedModuleId);
        assertEquals(actualModuleTree.getChildren().size(), 2);

        return actualModuleTree;
    }

    private void checkArtifactsNode(DependencyTree actualModule) {
        DependencyTree dependenciesNode = getAndAssertChild(actualModule, CiManagerBase.ARTIFACTS_NODE);
        assertEquals(CollectionUtils.size(dependenciesNode.getChildren()), 4);
        getAndAssertChild(dependenciesNode, "multi1-3.7-SNAPSHOT.jar");
    }

    private DependencyTree checkAndGetDirectDependenciesNode(DependencyTree actualModule) {
        DependencyTree dependenciesNode = getAndAssertChild(actualModule, CiManagerBase.DEPENDENCIES_NODE);
        DependencyTree actualDirectDependency = getAndAssertChild(dependenciesNode, "org.apache.commons:commons-email:1.1");
        assertEquals(actualDirectDependency.getScopes(), Sets.newHashSet(new Scope("Compile")));
        assertEquals(actualDirectDependency.getChildren().size(), 2);

        return actualDirectDependency;
    }

    private void checkAndGetTransitiveDependencyNode(DependencyTree actualDirectDependency) {
        DependencyTree actualTransitiveDependency = getAndAssertChild(actualDirectDependency, "javax.mail:mail:1.4");
        assertEquals(actualTransitiveDependency.getScopes(), Sets.newHashSet(new Scope("Compile")));
        assertTrue(CollectionUtils.isEmpty(actualTransitiveDependency.getChildren()));
    }

    private DependencyTree getAndAssertChild(DependencyTree node, String childName) {
        DependencyTree childNode = node.getChildren().stream()
                .filter(child -> childName.equals(child.getUserObject()))
                .findAny()
                .orElse(null);
        assertNotNull(childNode, "Couldn't find node '" + childName + "' between " + node + ".");
        return childNode;
    }
}


