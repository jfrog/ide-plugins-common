package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.persistency.BuildsScanCache;
import com.jfrog.xray.client.impl.services.details.DetailsResponseImpl;
import com.jfrog.xray.client.services.details.DetailsResponse;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Sets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Set;
import java.util.stream.Collectors;

import static com.jfrog.ide.common.TestUtils.getAndAssertChild;
import static com.jfrog.ide.common.utils.Utils.createMapper;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author yahavi
 **/
public class XrayBuildDetailsDownloaderTest {
    private final Transformer<Issue, Issue> issueTransformer = issue -> {
        issue.setComponent("");
        return issue;
    };
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
    public void testPopulateBuildDependencyTree() throws IOException, ParseException {
        BuildsScanCache buildsScanCache = new BuildsScanCache("maven-build", tempProject, new NullLog());
        try (InputStream artifactoryBuildStream = getClass().getResourceAsStream("/ci/artifactory-build.json");
             InputStream xrayDetailsStream = getClass().getResourceAsStream("/ci/xray-details-build.json")) {
            // Create build dependency tree
            Build build = mapper.readValue(artifactoryBuildStream, Build.class);
            assertNotNull(build);
            BuildArtifactsDownloader buildArtifactsDownloader = new BuildArtifactsDownloader(null, null, buildsScanCache, null, null, 0, new NullLog());
            DependencyTree buildDependencyTree = buildArtifactsDownloader.createBuildDependencyTree(build);

            // Populate build dependency tree with Xray data
            DetailsResponse buildDetails = mapper.readValue(xrayDetailsStream, DetailsResponseImpl.class);
            assertNotNull(buildDetails);
            XrayBuildDetailsDownloader xrayBuildDetailsDownloader = new XrayBuildDetailsDownloader(null, buildsScanCache, null, null, null, 0, null);
            xrayBuildDetailsDownloader.populateBuildDependencyTree(buildDependencyTree, buildDetails);

            // Get artifacts and dependencies nodes
            DependencyTree multi3 = getAndAssertChild(buildDependencyTree, "org.jfrog.test:multi3:3.7-SNAPSHOT");
            DependencyTree artifacts = getAndAssertChild(multi3, "artifacts");
            DependencyTree dependencies = getAndAssertChild(multi3, "dependencies");

            // Assert that multi3-3.7-SNAPSHOT.war have artifacts and dependencies
            DependencyTree multi3War = getAndAssertChild(artifacts, "multi3-3.7-SNAPSHOT.war");
            Set<Issue> artifactsIssues = multi3War.getIssues();
            assertEquals(artifactsIssues.size(), 9);
            Set<License> artifactsLicenses = multi3War.getLicenses();
            assertEquals(artifactsLicenses.size(), 5);

            // Assert that artifact's issues and the total dependencies issues are equal
            Set<Issue> dependenciesIssues = dependencies.processTreeIssues();
            assertEquals(removeComponentFromIssues(artifactsIssues), removeComponentFromIssues(dependenciesIssues));

            // Assert that artifact's licenses and the total dependencies licenses are equal
            Set<License> dependenciesLicenses = Sets.newHashSet();
            dependencies.collectAllScopesAndLicenses(Sets.newHashSet(), dependenciesLicenses);
            assertEquals(artifactsLicenses, dependenciesLicenses);
        }
    }

    /**
     * The artifact's issues and the dependencies issues are distinct only by the component ID.
     * Therefore, before comparing them, we'd like to remove the component IDs.
     *
     * @param issues - Artifact or dependencies issues
     * @return issues set without component IDs
     */
    private Set<Issue> removeComponentFromIssues(Set<Issue> issues) {
        return issues.stream().map(issueTransformer::transform).collect(Collectors.toSet());
    }

}
