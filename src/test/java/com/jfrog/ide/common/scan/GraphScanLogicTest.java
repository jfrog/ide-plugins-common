package com.jfrog.ide.common.scan;

import com.google.common.collect.Sets;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.ide.common.persistency.XrayScanCache;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.jfrog.ide.common.TestUtils.getAndAssertChild;
import static org.testng.Assert.*;

/**
 * @author yahavi
 **/
public class GraphScanLogicTest {
    private GraphScanLogic graphScanLogic;
    private DependencyTree root;
    private ScanCache scanCache;
    private Path baseDir;

    @BeforeMethod
    public void setUp() throws IOException {
        baseDir = Files.createTempDirectory("GraphScanLogicTest");
        scanCache = new XrayScanCache("GraphScanLogicTest", baseDir, new NullLog());
        graphScanLogic = new GraphScanLogic(scanCache, new NullLog());
        root = new DependencyTree("root");
        root.setMetadata(true);
        DependencyTree module = new DependencyTree("module");
        module.setMetadata(true);
        root.add(module);

        // Add a direct and a transitive dependencies that does exist in cache
        DependencyTree directDep = new DependencyTree("directDep");
        module.add(directDep);
        DependencyTree transitiveDep = new DependencyTree("transitiveDep");
        directDep.add(transitiveDep);
        scanCache.add(new Artifact(new GeneralInfo().componentId("directDep"), Sets.newHashSet(), Sets.newHashSet()));
        scanCache.add(new Artifact(new GeneralInfo().componentId("transitiveDep"), Sets.newHashSet(), Sets.newHashSet()));

        // Add ad direct and a transitive dependencies that doesn't exist in cache
        DependencyTree directNewDep = new DependencyTree("directNewDep");
        module.add(directNewDep);
        DependencyTree transitiveNewDep = new DependencyTree("transitiveNewDep");
        directNewDep.add(transitiveNewDep);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (baseDir != null) {
            FileUtils.deleteDirectory(baseDir.toFile());
        }
    }

    @Test
    public void createScanTreeQuickTest() {
        DependencyTree nodesToScan = graphScanLogic.createScanTree(root, true);
        assertEquals(nodesToScan.getChildCount(), 2);
        assertLeaf(nodesToScan, "directNewDep");
        assertLeaf(nodesToScan, "transitiveNewDep");

        // Assert that new direct dependency was added to the cache
        assertTrue(scanCache.contains("directNewDep"));
        assertFalse(scanCache.contains("transitiveNewDep"));
    }

    @Test
    public void createScanTreeFullTest() {
        DependencyTree nodesToScan = graphScanLogic.createScanTree(root, false);
        assertEquals(nodesToScan.getChildCount(), 4);
        assertLeaf(nodesToScan, "directDep");
        assertLeaf(nodesToScan, "transitiveDep");
        assertLeaf(nodesToScan, "directNewDep");
        assertLeaf(nodesToScan, "transitiveNewDep");

        // Assert that new direct dependency was added to the cache
        assertTrue(scanCache.contains("directNewDep"));
        assertFalse(scanCache.contains("transitiveNewDep"));
    }

    private void assertLeaf(DependencyTree root, String nodeName) {
        DependencyTree node = getAndAssertChild(root, nodeName);
        assertTrue(node.isLeaf());
    }
}
