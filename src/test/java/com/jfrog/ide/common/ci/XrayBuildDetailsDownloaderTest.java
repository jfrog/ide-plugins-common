package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.xray.client.impl.services.details.DetailsResponseImpl;
import com.jfrog.xray.client.services.details.DetailsResponse;
import org.jfrog.build.api.Build;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

import static com.jfrog.ide.common.utils.Utils.createMapper;
import static org.testng.Assert.assertNotNull;

/**
 * @author yahavi
 **/
public class XrayBuildDetailsDownloaderTest {
    private final ObjectMapper mapper = createMapper();

    @Test
    public void testPopulateBuildDependencyTree() throws IOException {
        try (InputStream artifactoryBuildStream = getClass().getResourceAsStream("/ci/artifactory-build.json");
             InputStream xrayDetailsStream = getClass().getResourceAsStream("/ci/xray-details-build.json")) {
            Build build = mapper.readValue(artifactoryBuildStream, Build.class);
            assertNotNull(build);


            DetailsResponse buildDetails = mapper.readValue(xrayDetailsStream, DetailsResponseImpl.class);
            assertNotNull(buildDetails);
        }
    }
}
