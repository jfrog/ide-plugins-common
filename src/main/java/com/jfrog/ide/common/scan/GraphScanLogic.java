package com.jfrog.ide.common.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.services.graph.GraphResponse;
import com.jfrog.xray.client.services.system.Version;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.CancellationException;

import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;

/**
 * This class includes the implementation of the Graph Scan Logic, which is used with Xray 3.29.0 and above.
 * The logic uses the graph scan REST API of Xray,
 * which take into consideration the policy configured in Xray according to given context.
 *
 * @author tala
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@Getter
@Setter
public class GraphScanLogic implements ScanLogic {
    public static final String MINIMAL_XRAY_VERSION_SUPPORTED_FOR_GRAPH_SCAN = "3.29.0";
    private ScanCache scanCache;
    private Log log;
    private DependencyTree scanResults;

    public GraphScanLogic(ScanCache scanCache, Log log) {
        this.scanCache = scanCache;
        this.log = log;
    }

    /**
     * Scan and cache components.
     *
     * @param server    - JFrog platform server configuration.
     * @param indicator - Progress bar.
     * @param quickScan - Quick or full scan.
     * @return true if the scan completed successfully, false otherwise.
     */
    @Override
    public boolean scanAndCacheArtifacts(ServerConfig server, ProgressIndicator indicator, boolean quickScan, ComponentPrefix prefix, Runnable checkCanceled) throws IOException, InterruptedException {
        // Xray's graph scan API does not support progress indication currently.
        indicator.setIndeterminate(true);
        scanResults.setPrefix(prefix.toString());
        DependencyTree nodesToScan = quickScan ? reduceComponents(scanResults) : scanResults;
        if (nodesToScan.getChildren().isEmpty()) {
            log.debug("No components found to scan. '");
            // No components found to scan
            return false;
        }
        try {
            // Create Xray client and check version
            Xray xrayClient = createXrayClientBuilder(server, log).build();
            if (!isSupportedInXrayVersion(xrayClient)) {
                return false;
            }
            // Start scan
            log.debug("Starting to scan, sending a dependency graph to Xray");
            checkCanceled.run();
            scanComponents(xrayClient, nodesToScan, server.getProject(), checkCanceled);

            indicator.setFraction(1);
            log.debug("Saving scan cache...");
            scanCache.write();
            log.debug("Scan cache saved successfully.");
        } catch (CancellationException e) {
            log.info("Xray scan was canceled.");
            return false;
        }
        return true;
    }

    /**
     * Removes components from the dependencies tree, if they already exist in the cache.
     * This is done to avoid having Xray match the components, for which already have the results.
     * Please notice that this method checks only the direct dependencies as they are the only ones to
     * be changed by a developer.
     *
     * @param root - The DependencyTree root node.
     * @return a graph of non cached component for Xray scan.
     */
    public DependencyTree reduceComponents(DependencyTree root) {
        DependencyTree reducedTree = (DependencyTree) root.clone();
        for (DependencyTree child : root.getChildren()) {
            String componentId = child.toString();
            if (!scanCache.contains(componentId)) {
                DependencyTree childCopy = (DependencyTree) child.clone();
                reducedTree.add(childCopy);
            }
        }
        return reducedTree;
    }

    @Override
    public Artifact getArtifactSummary(String componentId) {
        return scanCache.get(componentId);
    }

    public static boolean isSupportedInXrayVersion(Version xrayVersion) {
        return xrayVersion.isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED_FOR_GRAPH_SCAN);
    }

    private boolean isSupportedInXrayVersion(Xray xrayClient) {
        try {
            if (isSupportedInXrayVersion(xrayClient.system().version())) {
                return true;
            }
            log.error("Unsupported JFrog Xray version: Required JFrog Xray version " + MINIMAL_XRAY_VERSION_SUPPORTED_FOR_GRAPH_SCAN + " and above.");
        } catch (IOException e) {
            log.error("JFrog Xray Scan failed. Please check your credentials.", e);
        }
        return false;
    }

    /**
     * Xray scan a graph of components.
     *
     * @param xrayClient      - The Xray client.
     * @param artifactsToScan - The bulk of components to scan.
     * @param project         - The JFrog platform project-key parameter to be sent to Xray as context.
     * @throws IOException in case of connection issues.
     */
    private void scanComponents(Xray xrayClient, DependencyTree artifactsToScan, String project, Runnable checkCanceled) throws IOException, InterruptedException {
        if (project != null && !project.isEmpty()) {
            scanComponentsWithContext(xrayClient, artifactsToScan, project, checkCanceled);
        } else {
            scanComponentsWithoutContext(xrayClient, artifactsToScan, checkCanceled);
        }
        // Add to cache non-vulnerable direct dependencies in order to mark them as scanned successfully.
        // This will allow us to avoid unnecessary future scans.
        for (DependencyTree child : artifactsToScan.getChildren()) {
            if (!scanCache.contains(child.getComponentId())) {
                scanCache.add(new Artifact(new GeneralInfo(child.toString(), "", "", ""), new HashSet<>(), new HashSet<>()));
            }
        }
    }

    /**
     * Xray scan a graph of components.
     * a scan without supplying a context, meaning no specific Xray's watches will be triggered.
     * only general vulnerabilities and licenses info will be returned (if found).
     *
     * @param xrayClient      - The Xray client.
     * @param artifactsToScan - The bulk of components to scan.
     * @throws IOException          in case of connection issues.
     * @throws InterruptedException in case of scan canceled.
     */
    private void scanComponentsWithoutContext(Xray xrayClient, DependencyTree artifactsToScan, Runnable checkCanceled) throws IOException, InterruptedException {
        GraphResponse scanResults = xrayClient.graph().graph(artifactsToScan, checkCanceled);
        String packageType = scanResults.getPackageType();
        // Without context, expect vulnerabilities.
        // Add scan results to cache
        if (scanResults.getVulnerabilities() != null) {
            scanResults.getVulnerabilities().stream()
                    .filter(Objects::nonNull)
                    .filter(vulnerability -> vulnerability.getComponents() != null)
                    .forEach(vulnerability -> scanCache.add(vulnerability, packageType));
        }

        if (scanResults.getLicenses() != null) {
            scanResults.getLicenses().stream()
                    .filter(Objects::nonNull)
                    .filter(license -> license.getComponents() != null)
                    .forEach(license -> scanCache.add(license, packageType, false));
        }
    }

    /**
     * Xray scan a graph of components.
     * a scan with a context, meaning only specific Xray's watches that configured in the given JFrog platform project
     * will be triggered.
     * The response form Xray will include violations security and licenses (if found) according to those watches.
     *
     * @param xrayClient      - The Xray client.
     * @param artifactsToScan - The bulk of components to scan.
     * @param project         - The JFrog platform project-key parameter to be sent to Xray as context.
     * @throws IOException          in case of connection issues.
     * @throws InterruptedException in case of scan canceled.
     */
    private void scanComponentsWithContext(Xray xrayClient, DependencyTree artifactsToScan, String project, Runnable checkCanceled) throws IOException, InterruptedException {
        GraphResponse scanResults = xrayClient.graph().graph(artifactsToScan, checkCanceled, project);
        String packageType = scanResults.getPackageType();
        // Add scan results to cache
        // with context, expect violations.
        if (scanResults.getViolations() != null) {
            scanResults.getViolations().stream()
                    .filter(Objects::nonNull)
                    .filter(violation -> violation.getComponents() != null)
                    .forEach(violation -> scanCache.add(violation, packageType));
        }
        // Add violated licenses
        if (scanResults.getLicenses() != null) {
            scanResults.getLicenses().stream()
                    .filter(Objects::nonNull)
                    .filter(license -> license.getComponents() != null)
                    .forEach(license -> scanCache.add(license, packageType, true));
        }
    }
}
