package com.jfrog.ide.common.persistency;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author yahavi
 **/
public class BuildScanCacheTest {
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
    public void cacheNotExistTest() throws IOException {
        BuildsScanCache buildsScanCache = new BuildsScanCache("build-not-exist-test", tempProject, new NullLog());

        byte[] res = buildsScanCache.load("build-not-exist", "42", BuildsScanCache.Type.BUILD_INFO);
        assertNull(res);
    }

    @Test
    public void buildInfoCacheTest() throws IOException {
        BuildsScanCache buildsScanCache = new BuildsScanCache("build-cache-test", tempProject, new NullLog());

        // Save build info cache
        byte[] expectedBuildInfo = IOUtils.resourceToByteArray("/ci/artifactory-build.json");
        buildsScanCache.save(expectedBuildInfo, "maven-build", "1", BuildsScanCache.Type.BUILD_INFO);

        // Load build info cache
        byte[] actualBuildInfo = buildsScanCache.load("maven-build", "1", BuildsScanCache.Type.BUILD_INFO);
        assertEquals(actualBuildInfo, expectedBuildInfo);
    }

    @Test
    public void xrayScanCacheTest() throws IOException {
        BuildsScanCache buildsScanCache = new BuildsScanCache("xray-scan-cache-test", tempProject, new NullLog());

        // Save build info cache
        byte[] expectedBuildInfo = IOUtils.resourceToByteArray("/ci/xray-details-build.json");
        buildsScanCache.save(expectedBuildInfo, "maven-build", "1", BuildsScanCache.Type.XRAY_BUILD_SCAN);

        // Load build info cache
        byte[] actualBuildInfo = buildsScanCache.load("maven-build", "1", BuildsScanCache.Type.XRAY_BUILD_SCAN);
        assertEquals(actualBuildInfo, expectedBuildInfo);
    }
}
