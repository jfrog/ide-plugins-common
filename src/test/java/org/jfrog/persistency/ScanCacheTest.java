package org.jfrog.persistency;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.jfrog.xray.client.impl.services.summary.ArtifactImpl;
import com.jfrog.xray.client.impl.services.summary.GeneralImpl;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.*;

import static org.testng.Assert.*;

public class ScanCacheTest {

    private File tempDir;

    @SuppressWarnings("UnstableApiUsage")
    @BeforeMethod
    public void setUp() {
        tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterMethod
    public void tearDown() {
        tempDir.delete();
    }

    @Test
    public void testGetScanCacheEmpty() {
        String projectName = "not_exits";
        try {
            ScanCache scanCache = new ScanCache(projectName);
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
            ScanCache scanCache = new ScanCache(projectName);
            assertNotNull(scanCache);

            ArtifactImpl artifact = createArtifact(artifactId);

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
            ScanCache scanCache1 = new ScanCache(projectName, tempDir.toPath());
            ArtifactImpl artifact = createArtifact(artifactId);

            scanCache1.add(artifact);
            scanCache1.write();

            ScanCache scanCache2 = new ScanCache(projectName, tempDir.toPath());
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
            ScanCache scanCache = new ScanCache(projectName, tempDir.toPath());
            ArtifactImpl artifact = createArtifact(artifactId);

            scanCache.add(artifact);
            scanCache.add(createArtifact(artifactId));
            scanCache.write();
            assertEquals(scanCache.get(artifactId).getGeneralInfo().getComponentId(), artifactId);

            // Read again
            scanCache = new ScanCache(projectName, tempDir.toPath());
            assertEquals(scanCache.get(artifactId).getGeneralInfo().getComponentId(), artifactId);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    private ArtifactImpl createArtifact(String id) {
        ArtifactImpl artifact = new ArtifactImpl();
        GeneralImpl general = new GeneralImpl();
        general.setComponentId(id);
        artifact.setGeneral(general);
        artifact.setIssues(Lists.newArrayList());
        artifact.setLicenses(Lists.newArrayList());

        return artifact;
    }
}