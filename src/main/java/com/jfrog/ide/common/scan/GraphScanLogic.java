package com.jfrog.ide.common.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.tree.*;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.services.common.Cve;
import com.jfrog.xray.client.services.scan.*;
import com.jfrog.xray.client.services.system.Version;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.DependencyTree;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;

import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;
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
    private String pkgType;
    private Log log;

    public GraphScanLogic(String pkgType, Log log) {
        this.pkgType = pkgType;
        this.log = log;
    }

    @Override
    public Map<String, Artifact> scanArtifacts(DependencyTree dependencyTree, ServerConfig server, ProgressIndicator indicator, ComponentPrefix prefix, Runnable checkCanceled) throws IOException, InterruptedException {
        // Xray's graph scan API does not support progress indication currently.
        indicator.setIndeterminate(true);
        dependencyTree.setPrefix(prefix.toString());
        DependencyTree nodesToScan = createScanTree(dependencyTree);

        if (nodesToScan.isLeaf()) {
            log.debug("No components found to scan.");
            // No components found to scan
            // TODO: throw error instead? or return an empty map? if return empty map, the scan didn't fail
            return null;
        }
        try {
            // Create Xray client and check version
            Xray xrayClient = createXrayClientBuilder(server, log).build();
            if (!isSupportedInXrayVersion(xrayClient)) {
                // TODO: throw error instead? or move this check to another place?
                return null;
            }
            // Start scan
            log.debug("Starting to scan, sending a dependency graph to Xray");
            checkCanceled.run();
            Map<String, Artifact> response = scan(xrayClient, nodesToScan, server, checkCanceled, indicator);
            indicator.setFraction(1);

            return response;
        } catch (CancellationException e) {
            log.info("Xray scan was canceled.");
            return null;
        }
    }

    /**
     * Create a flat tree of all components required to scan.
     * Add all direct dependencies to cache to make sure that dependencies will not be scanned again in the next quick scan.
     *
     * @param root      - The root dependency tree node
     * @return a graph of non cached component for Xray scan.
     */
    DependencyTree createScanTree(DependencyTree root) {
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
            if (child.isMetadata()) {
                populateScanTree(child, scanTree, componentsAdded);
                continue;
            }

            // Add the dependency subtree to the scan tree
            String childFullId = child.toString();
            if (((DependencyTree) child.getParent()).isMetadata()) {
                // All direct dependencies should be in the cache. This line make sure that dependencies that
                // wouldn't return from Xray will not be scanned again during the next quick scan.
                String componentId = contains(childFullId, "://") ?
                        substringAfter(childFullId, "://") : childFullId;
            }
            if (componentsAdded.add(child.getComponentId())) {
                scanTree.add(new DependencyTree(child.getComponentId()));
            }
            populateScanTree(child, scanTree, componentsAdded);

        }
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

    // TODO: change comment
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
    private Map<String, Artifact> scan(Xray xrayClient, DependencyTree artifactsToScan, ServerConfig server, Runnable checkCanceled, ProgressIndicator indicator) throws IOException, InterruptedException {
        String projectKey = server.getPolicyType() == ServerConfig.PolicyType.PROJECT ? server.getProject() : "";
        String[] watches = server.getPolicyType() == ServerConfig.PolicyType.WATCHES ? split(server.getWatches(), ",") : null;
        GraphResponse scanResults = xrayClient.scan().graph(artifactsToScan, new XrayScanProgressImpl(indicator), checkCanceled, projectKey, watches);
        Map<String, Artifact> results = new HashMap<>();

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
                                    Artifact dep = results.get(compId);
                                    if (dep == null) {
                                        return;
                                    }
                                    dep.setLicenseName(license.getLicenseKey());
                                }
                        )
                );

        // Sort issues and licenses inside all artifacts
        results.values().forEach(Artifact::sortChildren);

        return results;
    }

    private void addViolationResult(Map<String, Artifact> results, Violation violation) {
        if (StringUtils.isBlank(violation.getLicenseKey())) {
            addSecurityViolationResult(results, violation);
        } else {
            addLicenseViolationResult(results, violation);
        }
    }

    private void addSecurityViolationResult(Map<String, Artifact> results, Violation violation) {
        addVulnerabilityResult(results, violation, violation.getWatchName());
    }

    private void addVulnerabilityResult(Map<String, Artifact> results, Vulnerability vulnerability) {
        addVulnerabilityResult(results, vulnerability, null);
    }

    private void addVulnerabilityResult(Map<String, Artifact> results, Vulnerability vulnerability, String watchName) {
        for (Map.Entry<String, ? extends Component> entry : vulnerability.getComponents().entrySet()) {
            Artifact artifact = getArtifact(results, entry);

            if (vulnerability.getCves() == null || vulnerability.getCves().size() == 0) {
            // TODO: handle no cves! possible?

            } else {
                for (Cve cve : vulnerability.getCves()) {
                    ResearchInfo researchInfo = null;
                    if (vulnerability.getExtendedInformation() != null) {
                        ExtendedInformation extInfo = vulnerability.getExtendedInformation();
                        researchInfo = new ResearchInfo(Severity.valueOf(extInfo.getJFrogResearchSeverity()), extInfo.getShortDescription(), extInfo.getFullDescription(), extInfo.getRemediation(), convertSeverityReasons(extInfo.getJFrogResearchSeverityReasons()));
                    }
                    // TODO: handle multiple watches. collect all identical issues of different watches together.
                    Issue issue = new Issue(vulnerability.getIssueId(), Severity.valueOf(vulnerability.getSeverity()),
                            StringUtils.defaultIfBlank(vulnerability.getSummary(), "N/A"), entry.getValue().getFixedVersions(),
                            entry.getValue().getInfectedVersions(),
                            new com.jfrog.ide.common.tree.Cve(cve.getId(), cve.getCvssV2Score(), cve.getCvssV2Vector(), cve.getCvssV3Score(), cve.getCvssV3Vector()),
                            vulnerability.getEdited(), Collections.singletonList(watchName), vulnerability.getReferences(), researchInfo);
                    artifact.addIssueOrLicense(issue);
                }
            }
        }
    }

    // TODO: attention!!! this method is only for adding license violations! to add license that is not violated, need to change violate=false in the ctor below.
    // TODO: add comment
    private void addLicenseViolationResult(Map<String, Artifact> results, Violation licenseViolation) {
        for (Map.Entry<String, ? extends Component> entry : licenseViolation.getComponents().entrySet()) {
            Artifact artifact = getArtifact(results, entry);
            // TODO: handle multiple watches. collect all identical violations of different watches together.
            com.jfrog.ide.common.tree.License licenseResult = new com.jfrog.ide.common.tree.License(
                    licenseViolation.getLicenseName(), licenseViolation.getLicenseKey(), licenseViolation.getReferences(),
                    Severity.valueOf(licenseViolation.getSeverity()), licenseViolation.getUpdated(),
                    Collections.singletonList(licenseViolation.getWatchName()));
            artifact.addIssueOrLicense(licenseResult);
        }
    }

    private Artifact getArtifact(Map<String, Artifact> results, Map.Entry<String, ? extends Component> compEntry) {
        String componentId = compEntry.getKey();
        if (!results.containsKey(componentId)) {
            results.put(componentId, new Artifact(new GeneralInfo().componentId(componentId).pkgType(pkgType)));
        }
        return results.get(componentId);
    }

    private SeverityReason[] convertSeverityReasons(com.jfrog.xray.client.services.scan.SeverityReasons[] xraySeverityReasons) {
        return Arrays.stream(xraySeverityReasons)
                .map(xrSeverityReason ->
                        new SeverityReason(xrSeverityReason.getName(), xrSeverityReason.getDescription(), xrSeverityReason.isPositive())
                ).toArray(SeverityReason[]::new);
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
