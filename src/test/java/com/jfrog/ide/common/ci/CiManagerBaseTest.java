package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.persistency.BuildsScanCache;
import com.jfrog.xray.client.impl.services.details.DetailsResponseImpl;
import com.jfrog.xray.client.services.details.DetailsResponse;
import com.jfrog.xray.client.services.summary.Artifact;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.jfrog.ide.common.utils.Utils.createMapper;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author yahavi
 **/
public class CiManagerBaseTest {
    private static final String BUILD_TIMESTAMP = "2021-03-17T17:08:38.989+0200";

    ObjectMapper mapper = createMapper();
    private Path cachePath;

    @BeforeMethod
    public void setUp() throws IOException {
        cachePath = Files.createTempDirectory("testLoadBuildTree");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(cachePath.toFile());
    }

    @DataProvider(name = "builds")
    private Object[][] getBuildNamesAndNumbers() {
        return new Object[][]{{"testBuild", "1"}, {"test/build", "1/f"}, {"test::build", "2::h"}, {"test::build:", "a1::23:"}, {"test%build", "test%build"}};
    }

    @Test(dataProvider = "builds")
    public void testLoadBuildTree(String buildName, String buildNumber) throws IOException, ParseException {
        // Create CI Manager Base
        CiManagerBase ciManagerBase = new CiManagerBase(cachePath, "test", new NullLog(), null);
        cacheDummyBuild(ciManagerBase, buildName, buildNumber);

        // Load build tree
        BuildDependencyTree buildDependencyTree = ciManagerBase.loadBuildTree(new BuildGeneralInfo().buildName(buildName).buildNumber(buildNumber));
        assertNotNull(buildDependencyTree);

        // Check results
        assertEquals(buildDependencyTree.getUserObject(), buildName + "/" + buildNumber);
        GeneralInfo generalInfo = buildDependencyTree.getGeneralInfo();
        assertNotNull(generalInfo);
        assertEquals(generalInfo.getComponentId(), buildName + ":" + buildNumber);
    }

    private void cacheDummyBuild(CiManagerBase ciManagerBase, String buildName, String buildNumber) throws IOException {
        Build build = new BuildInfoBuilder(buildName).number(buildNumber).started(BUILD_TIMESTAMP).build();
        ciManagerBase.buildsCache.save(mapper.writeValueAsBytes(build), buildName, buildNumber, BuildsScanCache.Type.BUILD_INFO);
        DetailsResponse detailsResponse = new MockDetailsResponse();
        ciManagerBase.buildsCache.save(mapper.writeValueAsBytes(detailsResponse), buildName, buildNumber, BuildsScanCache.Type.BUILD_SCAN_RESULTS);
    }

    private static class MockDetailsResponse extends DetailsResponseImpl {
        @Override
        public List<? extends Artifact> getComponents() {
            return new ArrayList<>();
        }
    }
}
