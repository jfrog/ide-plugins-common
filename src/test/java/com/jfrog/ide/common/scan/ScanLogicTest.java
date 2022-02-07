package com.jfrog.ide.common.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.ide.common.persistency.XrayScanCache;
import com.jfrog.ide.common.utils.XrayConnectionUtils;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.impl.XrayClientBuilder;
import com.jfrog.xray.client.impl.services.scan.GraphResponseImpl;
import com.jfrog.xray.client.impl.services.scan.ScanImpl;
import com.jfrog.xray.client.impl.services.summary.SummaryImpl;
import com.jfrog.xray.client.impl.services.summary.SummaryResponseImpl;
import com.jfrog.xray.client.impl.services.system.SystemImpl;
import com.jfrog.xray.client.impl.services.system.VersionImpl;
import com.jfrog.xray.client.impl.util.ObjectMapperHelper;
import com.jfrog.xray.client.services.scan.GraphResponse;
import com.jfrog.xray.client.services.scan.XrayScanProgress;
import com.jfrog.xray.client.services.summary.Components;
import com.jfrog.xray.client.services.summary.SummaryResponse;
import com.jfrog.xray.client.services.system.Version;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author yahavi
 **/
public class ScanLogicTest {
    private static final Path SCAN_RESPONSES = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "scanResponses"));
    private static final ObjectMapper mapper = ObjectMapperHelper.get();

    private final DependencyTree scanResults = new DependencyTree("ScanLogicTest");
    private final Log log = new NullLog();
    private ScanCache scanCache;
    private AutoCloseable mocks;
    private Path baseDir;

    @Mock
    private ProgressIndicator progressIndicator;
    @Mock
    private ServerConfig serverConfig;
    @Mock
    private Runnable checkCanceled;
    @Mock
    private XrayClient xrayClient;

    @BeforeMethod
    public void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        baseDir = Files.createTempDirectory("ScanLogicTest");
        scanCache = new XrayScanCache("ScanLogicTest", baseDir, new NullLog());
        scanResults.setMetadata(true);
        scanResults.add(new DependencyTree("gav://io.netty:netty-codec-http:4.1.31.Final"));
        scanResults.add(new DependencyTree("gav://org.apache.commons:commons-lang3:3.12.0"));
        scanResults.add(new DependencyTree("gav://commons-io:commons-io:2.11.0"));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(baseDir.toFile());
        mocks.close();
    }

    @Test
    public void testScanAndCacheSummaryComponent() throws IOException, InterruptedException {
        when(xrayClient.summary()).thenReturn(new SummaryMock(xrayClient));
        ScanLogic scanLogic = new ComponentSummaryScanLogic(scanCache, log);
        testScanAndCache(scanLogic, false);
    }

    @Test
    public void testScanAndCacheVulnerabilities() throws IOException, InterruptedException {
        when(xrayClient.scan()).thenReturn(new ScanVulnerabilitiesMock(xrayClient));
        ScanLogic scanLogic = new GraphScanLogic(scanCache, log);
        testScanAndCache(scanLogic, false);
        checkCommonsIo();
    }

    @Test
    public void testScanAndCacheViolations() throws IOException, InterruptedException {
        when(xrayClient.scan()).thenReturn(new ScanViolationsMock(xrayClient));
        ScanLogic scanLogic = new GraphScanLogic(scanCache, log);
        testScanAndCache(scanLogic, true);
        checkCommonsIo();
    }

    private void testScanAndCache(ScanLogic scanLogic, boolean violations) throws IOException, InterruptedException {
        try (MockedStatic<XrayConnectionUtils> utilities = Mockito.mockStatic(XrayConnectionUtils.class)) {
            utilities.when(() -> XrayConnectionUtils.createXrayClientBuilder(serverConfig, log)).thenReturn(new DummyXrayClientBuilder(xrayClient));
            when(xrayClient.system()).thenReturn(new SystemMock(xrayClient));
            scanLogic.setScanResults(scanResults);
            scanLogic.scanAndCacheArtifacts(serverConfig, progressIndicator, false, ComponentPrefix.GAV, checkCanceled);
            checkNettyCodec(violations);
            checkCommonsLang(violations);
        }
    }

    /**
     * Make sure io.netty:netty-codec-http:4.1.31.Final exists in cache
     *
     * @param violatedLicense - true if the license is expected to be violated
     */
    private void checkNettyCodec(boolean violatedLicense) {
        // Check artifact exist in cache
        Artifact artifact = scanCache.get("io.netty:netty-codec-http:4.1.31.Final");
        assertNotNull(artifact);

        // Check issues
        assertEquals(CollectionUtils.size(artifact.getIssues()), 8);
        Issue nettyIssue = artifact.getIssues().stream()
                .filter(issue -> StringUtils.equals(issue.getIssueId(), "XRAY-89129")).findAny().orElse(null);
        assertNotNull(nettyIssue);
        assertEquals(nettyIssue.getCves().get(0).getCveId(), "CVE-2019-16869");
        assertEquals(nettyIssue.getSummary(), "Netty before 4.1.42.Final mishandles whitespace before the colon in HTTP headers (such as a \"Transfer-Encoding : chunked\" line), which leads to HTTP request smuggling.");
        assertEquals(nettyIssue.getFixedVersions(), Lists.newArrayList("[4.1.44.Final]"));

        // Check Apache-2.0 license
        checkApache2License(artifact, violatedLicense);
    }

    /**
     * Make sure org.apache.commons:commons-lang3:3.12.0 exists in cache
     *
     * @param violatedLicense - true if the license is expected to be violated
     */
    private void checkCommonsLang(boolean violatedLicense) {
        // Check artifact exist in cache
        Artifact artifact = scanCache.get("org.apache.commons:commons-lang3:3.12.0");
        assertNotNull(artifact);

        // Make sure the artifact contains no issues
        assertTrue(CollectionUtils.isEmpty(artifact.getIssues()));

        // Check Apache-2.0 license
        checkApache2License(artifact, violatedLicense);
    }

    /**
     * Make sure commons-io:commons-io:2.11.0 exists in cache.
     * This component does not return from Xray and therefore the test make sure that it stored as a dummy cache object.
     * This logic is only relevant to GraphScanLogic, since summary/component always returns missing components.
     */
    private void checkCommonsIo() {
        Artifact artifact = scanCache.get("commons-io:commons-io:2.11.0");
        assertNotNull(artifact);
        assertTrue(CollectionUtils.isEmpty(artifact.getIssues()));
        assertTrue(CollectionUtils.isEmpty(artifact.getLicenses()));
    }

    private void checkApache2License(Artifact artifact, boolean violatedLicense) {
        assertEquals(CollectionUtils.size(artifact.getLicenses()), 1);
        License apache2 = artifact.getLicenses().stream().filter(license -> StringUtils.equals(license.getName(), "Apache-2.0")).findAny().orElse(null);
        assertNotNull(apache2);
        assertEquals(apache2.getFullName(), "The Apache Software License, Version 2.0");
        assertEquals(apache2.isViolate(), violatedLicense);
        assertTrue(CollectionUtils.isNotEmpty(apache2.getMoreInfoUrl()));
    }

    private static class SystemMock extends SystemImpl {
        public SystemMock(XrayClient xray) {
            super(xray);
        }

        @Override
        public Version version() throws IOException {
            return new VersionImpl(GraphScanLogic.MINIMAL_XRAY_VERSION_SUPPORTED_FOR_GRAPH_SCAN, "");
        }
    }

    private static class SummaryMock extends SummaryImpl {
        public SummaryMock(XrayClient xray) {
            super(xray);
        }

        @Override
        public SummaryResponse component(Components components) throws IOException {
            return mapper.readValue(new File(SCAN_RESPONSES.resolve("summaryComponent.json").toString()), SummaryResponseImpl.class);
        }
    }

    private static class ScanViolationsMock extends ScanImpl {
        public ScanViolationsMock(XrayClient xray) {
            super(xray);
        }

        @Override
        public GraphResponse graph(DependencyTree dependencies, XrayScanProgress progress, Runnable checkCanceled, String projectKey, String[] watches) throws IOException {
            return mapper.readValue(new File(SCAN_RESPONSES.resolve("graphScanViolations.json").toString()), GraphResponseImpl.class);
        }
    }

    private static class ScanVulnerabilitiesMock extends ScanImpl {
        public ScanVulnerabilitiesMock(XrayClient xray) {
            super(xray);
        }

        @Override
        public GraphResponse graph(DependencyTree dependencies, XrayScanProgress progress, Runnable checkCanceled, String projectKey, String[] watches) throws IOException {
            return mapper.readValue(new File(SCAN_RESPONSES.resolve("graphScanVulnerabilities.json").toString()), GraphResponseImpl.class);
        }
    }

    private static class DummyXrayClientBuilder extends XrayClientBuilder {
        XrayClient xrayClient;

        public DummyXrayClientBuilder(XrayClient xrayClient) {
            this.xrayClient = xrayClient;
        }

        @Override
        public XrayClient build() {
            return xrayClient;
        }
    }
}