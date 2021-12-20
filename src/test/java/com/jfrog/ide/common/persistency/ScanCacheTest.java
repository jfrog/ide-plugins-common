package com.jfrog.ide.common.persistency;

import com.google.common.collect.Lists;
import com.jfrog.xray.client.impl.services.summary.ArtifactImpl;
import com.jfrog.xray.client.impl.services.summary.GeneralImpl;
import com.jfrog.xray.client.services.summary.Artifact;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Maps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.jfrog.ide.common.persistency.ScanCacheMap.CACHE_VERSION;
import static org.testng.Assert.*;

/**
 * Test the scan cache.
 *
 * @author yahavi
 */
public class ScanCacheTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("ScanCacheTest");
        tempDir.toFile().deleteOnExit();
    }

    @AfterMethod
    public void tearDown() {
        FileUtils.deleteQuietly(tempDir.toFile());
    }

    @Test
    public void testGetScanCacheEmpty() {
        String projectName = "not_exits";
        try {
            ScanCache scanCache = new XrayScanCache(projectName, tempDir, new NullLog());
            assertNotNull(scanCache);
            assertNull(scanCache.get("not_exits"));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetArtifact() {
        String projectName = "Goliath";
        String artifactId = "ant:tony:1.2.3";
        try {
            ScanCache scanCache = new XrayScanCache(projectName, tempDir, new NullLog());
            assertNotNull(scanCache);

            Artifact artifact = createArtifact(artifactId);
            scanCache.add(artifact);
            assertEquals(scanCache.get(artifactId).getGeneralInfo().getComponentId(), artifactId);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testReadWrite() {
        String projectName = "Pegasus";
        String artifactId = "red:skull:3.3.3";
        try {
            ScanCache scanCache1 = new XrayScanCache(projectName, tempDir, new NullLog());
            Artifact artifact = createArtifact(artifactId);

            scanCache1.add(artifact);
            scanCache1.write();

            ScanCache scanCache2 = new XrayScanCache(projectName, tempDir, new NullLog());
            assertEquals(scanCache2.get(artifactId).getGeneralInfo().getComponentId(), artifactId);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testSameName() {
        String projectName = "Exodus";
        String artifactId = "tony:stark:3.3.3";
        try {
            ScanCache scanCache = new XrayScanCache(projectName, tempDir, new NullLog());
            Artifact artifact = createArtifact(artifactId);

            scanCache.add(artifact);
            scanCache.add(createArtifact(artifactId));
            scanCache.write();
            assertEquals(scanCache.get(artifactId).getGeneralInfo().getComponentId(), artifactId);

            // Read again
            scanCache = new XrayScanCache(projectName, tempDir, new NullLog());
            assertEquals(scanCache.get(artifactId).getGeneralInfo().getComponentId(), artifactId);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUpgrade() {
        String projectName = "Shield";
        String artifactId = "phil:coulson:1.2.3";
        try {
            // Create ScanCacheObject
            ScanCacheObject scanCacheObject = new ScanCacheObject() {
                @SuppressWarnings("unused")
                final int fuel = 50;
            };

            // Create artifacts map
            Map<String, ScanCacheObject> artifactsMap = Maps.newHashMap();
            artifactsMap.put(artifactId, scanCacheObject);

            // Create ScanCacheMap with version -1
            ScanCacheMap scanCacheMap = new XrayScanCacheMap();
            scanCacheMap.setVersion(-1);
            scanCacheMap.setArtifactsMap(artifactsMap);

            // Create ScanCache and flush
            ScanCache scanCache = new XrayScanCache(projectName, tempDir, new NullLog());
            scanCache.setScanCacheMap(scanCacheMap);
            scanCache.write();
            assertEquals(scanCache.getScanCacheMap().getVersion(), -1);
            assertEquals(scanCache.getScanCacheMap().getArtifactsMap().get(artifactId), scanCacheObject);

            // Read from disk and expect no errors - But empty cache
            scanCache = new XrayScanCache(projectName, tempDir, new NullLog());
            assertEquals(scanCache.getScanCacheMap().getVersion(), CACHE_VERSION);
            assertTrue(scanCache.getScanCacheMap().getArtifactsMap().isEmpty());

            // Write and check again
            scanCache.write();
            scanCache = new XrayScanCache(projectName, tempDir, new NullLog());
            assertEquals(scanCache.getScanCacheMap().getVersion(), CACHE_VERSION);
            assertTrue(scanCache.getScanCacheMap().getArtifactsMap().isEmpty());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Create an empty artifact.
     *
     * @param id - Artifact id.
     * @return empty artifact.
     */
    private Artifact createArtifact(String id) {
        ArtifactImpl artifact = new ArtifactImpl();
        GeneralImpl general = new GeneralImpl();
        general.setComponentId(id);
        artifact.setGeneral(general);
        artifact.setIssues(Lists.newArrayList());
        artifact.setLicenses(Lists.newArrayList());
        return artifact;
    }
}