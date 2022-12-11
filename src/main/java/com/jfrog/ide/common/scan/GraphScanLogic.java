package com.jfrog.ide.common.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.tree.Artifact;
import com.jfrog.ide.common.tree.GeneralInfo;
import com.jfrog.ide.common.tree.Issue;
import com.jfrog.ide.common.tree.Severity;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.services.scan.ImpactPathImpl;
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
import java.util.stream.Collectors;

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
    private Log log;

    public GraphScanLogic(Log log) {
        this.log = log;
    }

    // TODO: update comment
    /**
     * Scan and cache components.
     *
     * @param server    - JFrog platform server configuration.
     * @param indicator - Progress bar.
     * @return true if the scan completed successfully, false otherwise.
     */
    @Override
    public Map<String, Artifact> scanArtifacts(DependencyTree dependencyTree, ServerConfig server, ProgressIndicator indicator, ComponentPrefix prefix, Runnable checkCanceled) throws IOException, InterruptedException {
        // Xray's graph scan API does not support progress indication currently.
        indicator.setIndeterminate(true);
        dependencyTree.setPrefix(prefix.toString());

        // TODO: needed?
//        DependencyTree nodesToScan = createScanTree(scanResults);
        if (dependencyTree.isLeaf()) {
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
            Map<String, Artifact> response = scan(xrayClient, dependencyTree, server, checkCanceled, indicator);

            indicator.setFraction(1);

            // TODO: remove
//            log.debug("Saving scan cache...");
//            scanCache.write();
//            log.debug("Scan cache saved successfully.");

            return response;
        } catch (CancellationException e) {
            log.info("Xray scan was canceled.");
            // TODO: throw error instead?
            return null;
        }
    }

    // TODO: consider removing
//    // TODO: update comment, if needed
//    /**
//     * Create a flat tree of all components required to scan.
//     * Add all direct dependencies to cache to make sure that dependencies will not be scanned again in the next quick scan.
//     *
//     * @param root      - The root dependency tree node
//     * @return a graph of non cached component for Xray scan.
//     */
//    DependencyTree createScanTree(DependencyTree root) {
//        DependencyTree scanTree = new DependencyTree(root.getUserObject());
//        populateScanTree(root, scanTree);
//        return scanTree;
//    }

    // TODO: update comment, if needed
    // TODO: consider removing
//    /**
//     * Recursively, populate scan tree with the project's dependencies.
//     * The result is a flat tree with only dependencies needed for the Xray scan.
//     *
//     * @param root      - The root dependency tree node
//     * @param scanTree  - The result
//     */
//    private void populateScanTree(DependencyTree root, DependencyTree scanTree) {
//        for (DependencyTree child : root.getChildren()) {
//            // Don't add metadata nodes to the scan tree
//            if (child.isMetadata()) {
//                populateScanTree(child, scanTree);
//                continue;
//            }
//
//            // TODO: consider removing this comment
//            // If dependency not in cache or this is a full scan - add the dependency subtree to the scan tree.
//            // If dependency is in cache and this is a quick scan - skip subtree.
//            String childFullId = child.toString();
//            if (((DependencyTree) child.getParent()).isMetadata()) {
//                // All direct dependencies should be in the cache. This line makes sure that dependencies that
//                // wouldn't return from Xray will not be scanned again during the next quick scan.
//                String componentId = contains(childFullId, "://") ?
//                        substringAfter(childFullId, "://") : childFullId;
//            }
//            scanTree.add(new DependencyTree(child.getComponentId()));
//            populateScanTree(child, scanTree);
//        }
//    }

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

        // TODO: consider removing
        //        // Add licenses to all components
//        emptyIfNull(scanResults.getLicenses()).stream()
//                .filter(Objects::nonNull)
//                .filter(license -> license.getComponents() != null)
//                .forEach(license -> addLicenseResult(results, license));

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

        // Sort issues and licenses inside all artifacts
        results.values().forEach(Artifact::sortChildren);

        return results;
    }

    // TODO: add comment
    private void addViolationResult(Map<String, Artifact> results, Violation violation) {
        if (StringUtils.isBlank(violation.getLicenseKey())) {
            addVulnerabilityResult(results, violation);
        } else {
            addLicenseViolationResult(results, violation);
        }
    }

    // TODO: add comment
    private void addVulnerabilityResult(Map<String, Artifact> results, Vulnerability vulnerability) {
        for (Map.Entry<String, ? extends Component> entry : vulnerability.getComponents().entrySet()) {
            Artifact artifact = getArtifact(results, entry);
            // TODO: handle no cves! possible?
            for (Cve cve : vulnerability.getCves()) {
                Issue issue = new Issue(vulnerability.getIssueId(), Severity.valueOf(vulnerability.getSeverity()),
                        StringUtils.defaultIfBlank(vulnerability.getSummary(), "N/A"),
                        entry.getValue().getFixedVersions(), new com.jfrog.ide.common.tree.Cve(cve.getId(), cve.getCvssV2Score(), cve.getCvssV3Score()), vulnerability.getReferences(), vulnerability.getIgnoreRuleUrl());
//                        entry.getValue().getFixedVersions(), toCves(vulnerability.getCves()), vulnerability.getReferences(), vulnerability.getIgnoreRuleUrl());
// TODO: remove if not reverted
                //            artifact.getIssues().add(issue);
                artifact.addIssueOrLicense(issue);
            }
        }
    }

    // TODO: attention!!! this method is only for adding license violations! to add license that is not violated, need to change violate=false in the ctor below.
    // TODO: add comment
    private void addLicenseViolationResult(Map<String, Artifact> results, Violation licenseViolation) {
        for (Map.Entry<String, ? extends Component> entry : licenseViolation.getComponents().entrySet()) {
            Artifact artifact = getArtifact(results, entry);
            com.jfrog.ide.common.tree.License licenseResult = new com.jfrog.ide.common.tree.License(
                    licenseViolation.getLicenseName(), licenseViolation.getLicenseKey(), licenseViolation.getReferences(),
                    Severity.valueOf(licenseViolation.getSeverity()));
            // TODO: remove if not reverted
//            artifact.getLicenses().add(licenseResult);
            artifact.addIssueOrLicense(licenseResult);
        }
    }

    private Artifact getArtifact(Map<String, Artifact> results, Map.Entry<String, ? extends Component> compEntry) {
        String componentId = compEntry.getKey();
        if (!results.containsKey(componentId)) {
            results.put(componentId, new Artifact(new GeneralInfo().componentId(componentId), convertImpactPaths(compEntry.getValue().getImpactPaths())));
        }
        return results.get(componentId);
    }

    private List<List<String>> convertImpactPaths(List<List<ImpactPathImpl>> xrayImpactPaths) {
        return xrayImpactPaths.stream()
                .map(impactPath -> impactPath.stream()
                        .map(impactPathNode -> impactPathNode.getComponentId())
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
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
