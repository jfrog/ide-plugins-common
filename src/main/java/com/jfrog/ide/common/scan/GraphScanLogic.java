package com.jfrog.ide.common.scan;

import com.google.common.collect.Sets;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.services.scan.GraphResponse;
import com.jfrog.xray.client.services.scan.XrayScanProgress;
import com.jfrog.xray.client.services.system.Version;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.CancellationException;

import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.*;

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
        DependencyTree nodesToScan = createScanTree(scanResults, quickScan);
        if (nodesToScan.isLeaf()) {
            log.debug("No components found to scan.");
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
            scanAndCache(xrayClient, nodesToScan, server, checkCanceled, indicator);

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
     * Create a flat tree of all components required to scan.
     * Add all direct dependencies to cache to make sure that dependencies will not be scanned again in the next quick scan.
     *
     * @param root      - The root dependency tree node
     * @param quickScan - Quick or full scan
     * @return a graph of non cached component for Xray scan.
     */
    DependencyTree createScanTree(DependencyTree root, boolean quickScan) {
        DependencyTree scanTree = new DependencyTree(root.getUserObject());
        populateScanTree(root, scanTree, quickScan);
        return scanTree;
    }

    /**
     * Recursively, populate scan tree with the project's dependencies.
     * The result is a flat tree with only dependencies needed for the Xray scan.
     *
     * @param root      - The root dependency tree node
     * @param scanTree  - The result
     * @param quickScan - Quick or full scan
     */
    private void populateScanTree(DependencyTree root, DependencyTree scanTree, boolean quickScan) {
        for (DependencyTree child : root.getChildren()) {
            // Don't add metadata nodes to the scan tree
            if (child.isMetadata()) {
                populateScanTree(child, scanTree, quickScan);
                continue;
            }

            // If dependency not in cache or this is a full scan - add the dependency subtree to the scan tree.
            // If dependency is in cache and this is a quick scan - skip subtree.
            String childFullId = child.toString();
            if (!quickScan || !scanCache.contains(childFullId)) {
                if (((DependencyTree) child.getParent()).isMetadata()) {
                    // All direct dependencies should be in the cache. This line make sure that dependencies that
                    // wouldn't return from Xray will not be scanned again during the next quick scan.
                    String componentId = contains(childFullId, "://") ?
                            substringAfter(childFullId, "://") : childFullId;
                    scanCache.add(new Artifact(new GeneralInfo().componentId(componentId), new HashSet<>(), new HashSet<>()));
                }
                scanTree.add(new DependencyTree(child.getComponentId()));
                populateScanTree(child, scanTree, quickScan);
            }
        }
    }

    @Override
    public Artifact getArtifactSummary(String componentId) {
        Artifact artifact = scanCache.get(componentId);
        if (artifact == null) {
            return new Artifact(new GeneralInfo().componentId(componentId), Sets.newHashSet(), Sets.newHashSet(new License()));
        }
        if (isEmpty(artifact.getLicenses())) {
            // If no license detected in Xray, set a new unknown license
            artifact.getLicenses().add(new License());
        }
        return artifact;
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
     * A scan with project key may produce a list of licenses and a list of violated licenses and violated vulnerabilities.
     * A scan without project key may produce a list of licenses and a list of vulnerabilities.
     * The response form Xray will include violations security and licenses (if found) or vulnerabilities according to those watches.
     *
     * @param xrayClient      - The Xray client.
     * @param artifactsToScan - The bulk of components to scan.
     * @param server          - JFrog platform server configuration.
     * @param checkCanceled   - Callback that throws an exception if scan was cancelled by user
     * @throws IOException          in case of connection issues.
     * @throws InterruptedException in case of scan canceled.
     */
    private void scanAndCache(Xray xrayClient, DependencyTree artifactsToScan, ServerConfig server, Runnable checkCanceled, ProgressIndicator indicator) throws IOException, InterruptedException {
        String projectKey = server.getPolicyType() == ServerConfig.PolicyType.PROJECT ? server.getProject() : "";
        String[] watches = server.getPolicyType() == ServerConfig.PolicyType.WATCHES ? split(server.getWatches(), ",") : null;
        GraphResponse scanResults = xrayClient.scan().graph(artifactsToScan, new XrayScanProgressImpl(indicator), checkCanceled, projectKey, watches);

        // Add licenses to all components
        emptyIfNull(scanResults.getLicenses()).stream()
                .filter(Objects::nonNull)
                .filter(license -> license.getComponents() != null)
                .forEach(license -> scanCache.add(license, false));

        // If a project key provided, add all returned violated licenses and vulnerabilities.
        // In case of a violated license, the license added in above section will be overridden with violated=true.
        emptyIfNull(scanResults.getViolations()).stream()
                .filter(Objects::nonNull)
                .filter(violation -> violation.getComponents() != null)
                .forEach(violation -> scanCache.add(violation));

        // If no project key provided, Add all returned vulnerabilities.
        emptyIfNull(scanResults.getVulnerabilities()).stream()
                .filter(Objects::nonNull)
                .filter(vulnerability -> vulnerability.getComponents() != null)
                .forEach(vulnerability -> scanCache.add(vulnerability));
    }

    /**
     * Utility class reporting progress in Xray client's {@link XrayScanProgress}, using {@link ProgressIndicator}.
     */
    private static class XrayScanProgressImpl implements XrayScanProgress {
        private final ProgressIndicator indicator;

        public XrayScanProgressImpl(ProgressIndicator indicator) {
            this.indicator = indicator;
        }

        @Override
        public void setFraction(double fraction) {
            indicator.setFraction(fraction);
        }
    }
}
