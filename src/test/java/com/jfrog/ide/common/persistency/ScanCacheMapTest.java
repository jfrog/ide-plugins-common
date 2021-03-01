package com.jfrog.ide.common.persistency;

import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.testng.annotations.Test;
import org.testng.collections.Sets;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author yahavi
 **/
public class ScanCacheMapTest {

    @Test
    public void testLru() {
        String artifact1 = "wanda:vision:1";
        String artifact2 = "wanda:vision:2";
        String artifact3 = "wanda:vision:3";
        String artifact4 = "wanda:vision:4";

        // Create scan cache with capacity of 3
        ScanCacheMap scanCacheMap = new ScanCacheMap(3);
        scanCacheMap.put(createArtifact(artifact1));
        scanCacheMap.put(createArtifact(artifact2));
        scanCacheMap.put(createArtifact(artifact3));

        // Make sure 3 artifacts contained in the cache
        assertTrue(scanCacheMap.contains(artifact1));
        assertTrue(scanCacheMap.contains(artifact2));
        assertTrue(scanCacheMap.contains(artifact3));

        // Add 4th artifact and make sure that only the last three of them are in the cache
        scanCacheMap.put(createArtifact(artifact4));
        assertFalse(scanCacheMap.contains(artifact1));
        assertTrue(scanCacheMap.contains(artifact2));
        assertTrue(scanCacheMap.contains(artifact3));
        assertTrue(scanCacheMap.contains(artifact4));
    }

    @Test
    public void testAddInvalidated() {
        String newArtifact = "newArtifact";
        String oldArtifact = "oldArtifact";

        // Create a scan cache that should contain only artifacts newer than 1 week
        ScanCacheMap scanCacheMap = new ScanCacheMap();
        Map<String, ScanCacheObject> artifactsMap = scanCacheMap.getArtifactsMap();

        // Add an artifact and make sure it's included in the cache
        ScanCacheObject newObject = new ScanCacheObject();
        artifactsMap.put(newArtifact, newObject);
        assertTrue(scanCacheMap.contains(newArtifact));

        // Add an artifact with timestamp older than 1 week and make sure it's not included in the cache
        ScanCacheObject oldObject = new ScanCacheObject();
        oldObject.setLastUpdated(System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(7) + 1));
        artifactsMap.put(oldArtifact, oldObject);
        assertFalse(scanCacheMap.contains(oldArtifact));
    }

    /**
     * Create an empty artifact.
     *
     * @param id - Artifact id.
     * @return empty artifact.
     */
    private Artifact createArtifact(String id) {
        Artifact artifact = new Artifact();
        GeneralInfo general = new GeneralInfo().componentId(id);
        artifact.setGeneralInfo(general);
        artifact.setIssues(Sets.newHashSet());
        artifact.setLicenses(Sets.newHashSet());
        return artifact;
    }
}
