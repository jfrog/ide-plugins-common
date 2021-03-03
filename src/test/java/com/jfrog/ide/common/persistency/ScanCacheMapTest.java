package com.jfrog.ide.common.persistency;

import org.apache.commons.lang3.RandomStringUtils;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.testng.annotations.Test;
import org.testng.collections.Sets;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.testng.Assert.*;

/**
 * Test the scan cache maps.
 *
 * @author yahavi
 */
public class ScanCacheMapTest {

    @Test
    public void testLru() {
        // Create scan cache with capacity of 3
        ScanCacheMap scanCacheMap = new LruScanCacheMap(3);
        Map<String, ScanCacheObject> artifactsMap = scanCacheMap.getArtifactsMap();

        // Add 1000 random artifacts to cache. Each artifact contains the ith timestamp.
        int scanObjectsCount = 1000;
        for (int i = 0; i < scanObjectsCount; i++) {
            String componentId = RandomStringUtils.randomAlphanumeric(5, 20);
            ScanCacheObject scanCacheObject = new ScanCacheObject(createArtifact(componentId));
            scanCacheObject.setLastUpdated(i);
            artifactsMap.put(componentId, scanCacheObject);
        }
        // Make sure there are only 3 artifacts in the cache
        assertEquals(artifactsMap.size(), 3);

        // Make sure the artifacts in the cache are the ones with the latest timestamp
        ScanCacheObject[] actual = artifactsMap.values().toArray(new ScanCacheObject[0]);
        assertEquals(actual[0].getLastUpdated(), scanObjectsCount - 3);
        assertEquals(actual[1].getLastUpdated(), scanObjectsCount - 2);
        assertEquals(actual[2].getLastUpdated(), scanObjectsCount - 1);
    }

    @Test
    public void testLruConcurrency() {
        // Create scan cache with capacity of 10
        ScanCacheMap scanCacheMap = new LruScanCacheMap(10);
        IntStream.range(0, 100000)
                .parallel()
                .mapToObj(i -> createArtifact(RandomStringUtils.randomAlphanumeric(5, 20)))
                .forEach(scanCacheMap::put);

        // Make sure the cache map contains exactly 3 artifacts
        assertEquals(scanCacheMap.getArtifactsMap().size(), 10);
    }

    @Test
    public void testTimeBased() {
        String newArtifact = "newArtifact";
        String oldArtifact = "oldArtifact";

        // Create a scan cache that may contain only artifacts newer than 1 week
        ScanCacheMap scanCacheMap = new TimeBasedCacheMap();
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

    @Test
    public void testTimeBasedConcurrency() {
        // Create a scan cache that may contain only artifacts newer than 1 week
        Map<String, ScanCacheObject> scanCacheMap = new TimeBasedCacheMap().getArtifactsMap();

        // Populate the scan cache concurrently with 50000 expired artifacts and 50000 eligible artifacts
        LongStream.range(0, 100000)
                .parallel()
                .map(i -> System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(7) + (i % 2 == 0 ? 10000 : -10000)))
                .mapToObj(lastUpdated -> {
                    ScanCacheObject scanCacheObject = new ScanCacheObject(createArtifact(RandomStringUtils.randomAlphanumeric(5, 20)));
                    scanCacheObject.setLastUpdated(lastUpdated);
                    return scanCacheObject;
                })
                .forEach(cacheObject -> scanCacheMap.put(cacheObject.getArtifact().getGeneralInfo().getComponentId(), cacheObject));

        // Make sure the cache map contains exactly 50000 eligible artifacts
        assertEquals(scanCacheMap.size(), 50000);
        boolean expiredExist = scanCacheMap.values().stream().anyMatch(ScanCacheObject::isExpired);
        assertFalse(expiredExist);
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
