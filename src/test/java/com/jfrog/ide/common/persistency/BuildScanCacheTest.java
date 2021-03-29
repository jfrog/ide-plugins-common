package com.jfrog.ide.common.persistency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.ci.BuildArtifactsDownloader;
import com.jfrog.ide.common.ci.XrayBuildDetailsDownloader;
import com.jfrog.xray.client.impl.services.details.DetailsResponseImpl;
import com.jfrog.xray.client.services.details.DetailsResponse;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

import static com.jfrog.ide.common.TestUtils.getAndAssertChild;
import static com.jfrog.ide.common.utils.Utils.createMapper;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author yahavi
 **/
public class BuildScanCacheTest {
    private final ObjectMapper mapper = createMapper();
    private Path tempProject;

    @BeforeMethod
    public void setUp(Object[] testArgs) throws IOException {
        tempProject = Files.createTempDirectory("ide-plugins-common-build-cache");
        FileUtils.forceDeleteOnExit(tempProject.toFile());
    }

    @AfterMethod
    public void tearDown() throws IOException {
        FileUtils.forceDelete(tempProject.toFile());
    }

    @Test
    public void loadAndSaveTest() throws IOException, ParseException {
        try (InputStream artifactoryBuildStream = getClass().getResourceAsStream("/ci/artifactory-build.json");
             InputStream xrayDetailsStream = getClass().getResourceAsStream("/ci/xray-details-build.json")) {
            // Create build dependency tree
            Build build = mapper.readValue(artifactoryBuildStream, Build.class);
            assertNotNull(build);
            BuildArtifactsDownloader buildArtifactsDownloader = new BuildArtifactsDownloader(null, null, null, null, 0, new NullLog());
            DependencyTree expectedDependencyTree = buildArtifactsDownloader.createBuildDependencyTree(build);

            // Populate build dependency tree with Xray data
            DetailsResponse buildDetails = mapper.readValue(xrayDetailsStream, DetailsResponseImpl.class);
            assertNotNull(buildDetails);
            XrayBuildDetailsDownloader xrayBuildDetailsDownloader = new XrayBuildDetailsDownloader(null, null, null, null, 0, null);
            xrayBuildDetailsDownloader.populateBuildDependencyTree(expectedDependencyTree, buildDetails);

            // Save build cache
            BuildsScanCache buildsScanCache = new BuildsScanCache("build-cache-test", tempProject, new NullLog());
            buildsScanCache.saveDependencyTree(expectedDependencyTree);

            DependencyTree actualDependencyTree = buildsScanCache.loadDependencyTree("maven-build", "1", "1615993718989");

            compareTrees(actualDependencyTree, expectedDependencyTree);
        }
    }

    private void compareTrees(DependencyTree actualNode, DependencyTree expectedNode) {
        assertEquals(actualNode.getUserObject(), expectedNode.getUserObject());

        GeneralInfo expectedGeneralInfo = expectedNode.getGeneralInfo();
        GeneralInfo actualGeneralInfo = actualNode.getGeneralInfo();

        assertEquals(actualGeneralInfo.getComponentId(), expectedGeneralInfo.getComponentId());
        assertEquals(actualGeneralInfo.getName(), expectedGeneralInfo.getName());
        assertEquals(actualGeneralInfo.getVersion(), expectedGeneralInfo.getVersion());
        assertEquals(actualGeneralInfo.getArtifactId(), expectedGeneralInfo.getArtifactId());
        assertEquals(actualGeneralInfo.getGroupId(), expectedGeneralInfo.getGroupId());
        assertEquals(actualGeneralInfo.getArtifact(), expectedGeneralInfo.getArtifact());
        assertEquals(actualGeneralInfo.getPkgType(), expectedGeneralInfo.getPkgType());
        assertEquals(actualGeneralInfo.getSha1(), expectedGeneralInfo.getSha1());

        assertEquals(actualNode.getIssues(), expectedNode.getIssues());
        assertEquals(actualNode.getLicenses(), expectedNode.getLicenses());
        assertEquals(actualNode.getScopes(), expectedNode.getScopes());

        for (DependencyTree expectedChild : expectedNode.getChildren()) {
            DependencyTree actualChild = getAndAssertChild(actualNode, expectedChild.getUserObject().toString());
            compareTrees(actualChild, expectedChild);
        }
    }
}
