package com.jfrog.ide.common.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.LicenseViolationNode;
import com.jfrog.ide.common.nodes.VulnerabilityNode;
import com.jfrog.ide.common.nodes.subentities.License;
import com.jfrog.ide.common.nodes.subentities.ResearchInfo;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SeverityReason;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.services.common.Cve;
import com.jfrog.xray.client.services.scan.*;
import com.jfrog.xray.client.services.system.Version;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.DependencyTree;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

import static com.jfrog.ide.common.utils.Utils.removeComponentIdPrefix;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.split;

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
    private Log log;

    public GraphScanLogic(Log log) {
        this.log = log;
    }

    @Override
    public Map<String, DependencyNode> scanArtifacts(DepTree depTree, ServerConfig server, ProgressIndicator indicator, ComponentPrefix prefix, Runnable checkCanceled) throws IOException, InterruptedException {
        // Xray's graph scan API does not support progress indication currently.
        indicator.setIndeterminate(true);
        DependencyTree nodesToScan = createScanTree(depTree, prefix);

        if (nodesToScan.isLeaf()) {
            log.debug("No components found to scan.");
            // No components found to scan
            return null;
        }
        try (Xray xrayClient = createXrayClientBuilder(server, log).build()) {
            validateXraySupport(xrayClient.system().version());
            // Start scan
            log.debug("Starting to scan, sending a dependency graph to Xray");
            checkCanceled.run();
            Map<String, DependencyNode> response = scan(xrayClient, nodesToScan, server, checkCanceled, indicator);
            indicator.setFraction(1);
            return response;
        } catch (CancellationException e) {
            throw new InterruptedIOException("Xray scan was canceled.");
        }
    }

    /**
     * Create a flat tree of all components required to scan.
     * Add all direct dependencies to cache to make sure that dependencies will not be scanned again in the next quick scan.
     *
     * @param root - The root dependency tree node
     * @return a graph of non cached component for Xray scan.
     */
    private DependencyTree createScanTree(DependencyTree root) {
        DependencyTree scanTree = new DependencyTree(root.getUserObject());
        Set<String> componentsAdded = new HashSet<>();
        populateScanTree(root, scanTree, componentsAdded);
        return scanTree;
    }

    /**
     * Recursively, populate scan tree with the project's dependencies.
     * The result is a flat tree with only dependencies needed for the Xray scan.
     *
     * @param root            - The root dependency tree node
     * @param scanTree        - The result
     * @param componentsAdded - Set of added components used to remove duplications
     */
    private void populateScanTree(DependencyTree root, DependencyTree scanTree, Set<String> componentsAdded) {
        for (DependencyTree child : root.getChildren()) {
            // Don't add metadata nodes to the scan tree
            if (!child.isMetadata()) {
                // Add the dependency subtree to the scan tree
                if (componentsAdded.add(child.getComponentId())) {
                    scanTree.add(new DependencyTree(child.getComponentId()));
                }
            }
            populateScanTree(child, scanTree, componentsAdded);
        }
    }

    /**
     * Create a tree of all components required to scan.
     *
     * @param tree   - the dependency tree to scan.
     * @param prefix - components prefix for xray scan, e.g. gav:// or npm://.
     * @return a graph of components for Xray scan.
     */
    private DependencyTree createScanTree(DepTree tree, ComponentPrefix prefix) {
        String rootFullId = prefix.getPrefix() + tree.getRootId();
        DependencyTree scanTree = new DependencyTree(rootFullId);
        Set<String> componentsAdded = new HashSet<>();
        for (Map.Entry<String, DepTreeNode> nodeEntry : tree.getNodes().entrySet()) {
            String compId = nodeEntry.getKey();
            if (nodeEntry.getValue().getDescriptorFilePath() == null && componentsAdded.add(compId)) {
                String nodeFullId = prefix.getPrefix() + compId;
                scanTree.add(new DependencyTree(nodeFullId));
            }
        }
        return scanTree;
    }

    public static void validateXraySupport(Version xrayVersion) {
        if (xrayVersion.isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED_FOR_GRAPH_SCAN)) {
            return;
        }
        throw new UnsupportedOperationException("JFrog Xray version " + xrayVersion.getVersion() + " is unsupported. Required JFrog Xray version " + MINIMAL_XRAY_VERSION_SUPPORTED_FOR_GRAPH_SCAN + " and above.");
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
    private Map<String, DependencyNode> scan(Xray xrayClient, DependencyTree artifactsToScan, ServerConfig server, Runnable checkCanceled, ProgressIndicator indicator) throws IOException, InterruptedException {
        String projectKey = server.getPolicyType() == ServerConfig.PolicyType.PROJECT ? server.getProject() : "";
        String[] watches = server.getPolicyType() == ServerConfig.PolicyType.WATCHES ? split(server.getWatches(), ",") : null;
        GraphResponse scanResults = xrayClient.scan().graph(artifactsToScan, new XrayScanProgressImpl(indicator), checkCanceled, projectKey, watches);
        Map<String, DependencyNode> results = new HashMap<>();

        // If a project key provided, add all returned violated licenses and vulnerabilities.
        // In case of a violated license, the license added in above section will be overridden with violated=true.
        emptyIfNull(scanResults.getViolations()).stream()
                .filter(Objects::nonNull)
                .filter(violation -> violation.getComponents() != null)
                .forEach(violation -> addViolationResult(results, violation));

        // If no project key provided, Add all returned vulnerabilities.
        emptyIfNull(scanResults.getVulnerabilities()).stream()
                .filter(Objects::nonNull)
                .filter(vulnerability -> vulnerability.getComponents() != null)
                .forEach(vulnerability -> addVulnerabilityResult(results, vulnerability));

        emptyIfNull(scanResults.getLicenses()).stream()
                .filter(Objects::nonNull)
                .filter(license -> license.getComponents() != null)
                .forEach(license ->
                        license.getComponents().forEach(
                                (compId, comp) -> {
                                    DependencyNode dep = results.get(compId);
                                    if (dep == null) {
                                        return;
                                    }
                                    String moreInfoUrl = null;
                                    if (!CollectionUtils.isEmpty(license.getReferences())) {
                                        moreInfoUrl = license.getReferences().get(0);
                                    }
                                    dep.addLicense(new License(license.getLicenseKey(), moreInfoUrl));
                                }
                        )
                );

        return results;
    }

    private void addViolationResult(Map<String, DependencyNode> results, Violation violation) {
        if (StringUtils.isBlank(violation.getLicenseKey())) {
            addSecurityViolationResult(results, violation);
        } else {
            addLicenseViolationResult(results, violation);
        }
    }

    private void addSecurityViolationResult(Map<String, DependencyNode> results, Violation violation) {
        addVulnerabilityResult(results, violation, violation.getWatchName(), violation.getIgnoreRuleUrl());
    }

    private void addVulnerabilityResult(Map<String, DependencyNode> results, Vulnerability vulnerability) {
        addVulnerabilityResult(results, vulnerability, null, null);
    }

    private void addVulnerabilityResult(Map<String, DependencyNode> results, Vulnerability vulnerability, String watchName, String ignoreRuleUrl) {
        for (Map.Entry<String, ? extends Component> entry : vulnerability.getComponents().entrySet()) {
            DependencyNode dependencyNode = getDependency(results, entry.getKey());

            if (vulnerability.getCves() == null || vulnerability.getCves().size() == 0) {
                VulnerabilityNode vulnerabilityNode = convertToIssue(vulnerability, entry.getValue(), null, watchName, ignoreRuleUrl);
                dependencyNode.addIssue(vulnerabilityNode);
            } else {
                for (Cve cve : vulnerability.getCves()) {
                    VulnerabilityNode vulnerabilityNode = convertToIssue(vulnerability, entry.getValue(), cve, watchName, ignoreRuleUrl);
                    dependencyNode.addIssue(vulnerabilityNode);
                }
            }
        }
    }

    private VulnerabilityNode convertToIssue(Vulnerability vulnerability, Component component, Cve cve, String watchName, String ignoreRuleUrl) {
        ResearchInfo researchInfo = null;
        if (vulnerability.getExtendedInformation() != null) {
            ExtendedInformation extInfo = vulnerability.getExtendedInformation();
            researchInfo = new ResearchInfo(Severity.valueOf(extInfo.getJFrogResearchSeverity()), extInfo.getShortDescription(), extInfo.getFullDescription(), extInfo.getRemediation(), convertSeverityReasons(extInfo.getJFrogResearchSeverityReasons()));
        }
        String cveId = null, cvssV2Score = null, cvssV2Vector = null, cvssV3Score = null, cvssV3Vector = null;
        if (cve != null) {
            cveId = cve.getId();
            cvssV2Score = cve.getCvssV2Score();
            cvssV2Vector = cve.getCvssV2Vector();
            cvssV3Score = cve.getCvssV3Score();
            cvssV3Vector = cve.getCvssV3Vector();
        }
        List<String> watchNames = null;
        if (watchName != null) {
            // Since singleton lists and unmodifiable lists cannot be read from the cache, an ArrayList is used instead
            watchNames = new ArrayList<>();
            watchNames.add(watchName);
        }
        return new VulnerabilityNode(vulnerability.getIssueId(), Severity.valueOf(vulnerability.getSeverity()),
                StringUtils.defaultIfBlank(vulnerability.getSummary(), "N/A"), component.getFixedVersions(),
                component.getInfectedVersions(),
                new com.jfrog.ide.common.nodes.subentities.Cve(cveId, cvssV2Score, cvssV2Vector, cvssV3Score, cvssV3Vector),
                vulnerability.getEdited(), watchNames, vulnerability.getReferences(), researchInfo, ignoreRuleUrl);
    }

    private void addLicenseViolationResult(Map<String, DependencyNode> results, Violation licenseViolation) {
        for (Map.Entry<String, ? extends Component> entry : licenseViolation.getComponents().entrySet()) {
            DependencyNode dependencyNode = getDependency(results, entry.getKey());
            List<String> watchNames = null;
            if (licenseViolation.getWatchName() != null) {
                // Since singleton lists and unmodifiable lists cannot be read from the cache, an ArrayList is used instead
                watchNames = new ArrayList<>();
                watchNames.add(licenseViolation.getWatchName());
            }
            LicenseViolationNode licenseResult = new LicenseViolationNode(
                    licenseViolation.getLicenseName(), licenseViolation.getLicenseKey(), licenseViolation.getReferences(),
                    Severity.valueOf(licenseViolation.getSeverity()), licenseViolation.getUpdated(), watchNames);
            dependencyNode.addIssue(licenseResult);
        }
    }

    private DependencyNode getDependency(Map<String, DependencyNode> results, String componentId) {
        String depKey = removeComponentIdPrefix(componentId);
        results.putIfAbsent(depKey, new DependencyNode().componentId(componentId));
        return results.get(depKey);
    }

    private List<SeverityReason> convertSeverityReasons(com.jfrog.xray.client.services.scan.SeverityReasons[] xraySeverityReasons) {
        if (xraySeverityReasons == null) {
            return null;
        }
        return Arrays.stream(xraySeverityReasons)
                .map(xrSeverityReason ->
                        new SeverityReason(xrSeverityReason.getName(), xrSeverityReason.getDescription(), xrSeverityReason.isPositive())
                ).collect(Collectors.toList());
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
