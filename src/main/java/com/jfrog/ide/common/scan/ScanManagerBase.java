package com.jfrog.ide.common.scan;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jfrog.ide.common.configuration.XrayServerConfig;
import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.common.utils.Constants;
import com.jfrog.ide.common.utils.XrayConnectionUtils;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.services.summary.ComponentDetail;
import com.jfrog.xray.client.services.summary.Components;
import com.jfrog.xray.client.services.summary.SummaryResponse;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;

/**
 * Base class for the scan managers.
 *
 * @author yahavi
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@Getter
@Setter
public abstract class ScanManagerBase {
    private final static int NUMBER_OF_ARTIFACTS_BULK_SCAN = 100;

    private XrayServerConfig xrayServerConfig;
    private DependenciesTree scanResults;
    private ComponentPrefix prefix;
    private ScanCache scanCache;
    private String projectName;
    private Log log;

    /**
     * Construct a scan manager for IDE project.
     *
     * @param cachePath        - Scan cache path.
     * @param projectName      - The project name.
     * @param log              - The logger.
     * @param xrayServerConfig - Xray server config.
     * @param prefix           - Components prefix for xray scan, e.g. gav:// or npm://.
     * @throws IOException in case of an error in the scan cache initialization.
     */
    public ScanManagerBase(Path cachePath, String projectName, Log log, XrayServerConfig xrayServerConfig, ComponentPrefix prefix) throws IOException {
        this.scanCache = new ScanCache(projectName, cachePath, log);
        this.xrayServerConfig = xrayServerConfig;
        this.projectName = projectName;
        this.prefix = prefix;
        this.log = log;
    }

    /**
     * Populate a DependenciesTree node with issues, licenses and general info from the scan cache.
     *
     * @param node - The root node.
     */
    protected void populateDependenciesTreeNode(DependenciesTree node) {
        Artifact scanArtifact = getArtifactSummary(node.toString());
        if (scanArtifact != null) {
            node.setIssues(Sets.newHashSet(scanArtifact.getIssues()));
            node.setLicenses(Sets.newHashSet(scanArtifact.getLicenses()));
            if (node.getGeneralInfo() == null) {
                node.setGeneralInfo(scanArtifact.getGeneralInfo());
            }
        }
    }

    /**
     * Add licenses to filter manager in order to show them in the filter menu later.
     */
    protected void addFilterManagerLicenses(FilterManager filterManager) {
        Set<License> allLicenses = Sets.newHashSet();
        if (scanResults != null) {
            DependenciesTree node = (DependenciesTree) scanResults.getRoot();
            collectAllLicenses(node, allLicenses);
        }
        filterManager.addLicenses(allLicenses);
    }

    /**
     * Recursively, add all dependencies list licenses to the licenses set.
     *
     * @param node        - In - The root DepdendenciesTree node.
     * @param allLicenses - Out - All licenses in the tree.
     */
    private void collectAllLicenses(DependenciesTree node, Set<License> allLicenses) {
        allLicenses.addAll(node.getLicenses());
        node.getChildren().forEach(child -> collectAllLicenses(child, allLicenses));
    }

    /**
     * Return filtered issues according to the selected component and user filters.
     *
     * @param selectedNodes - Selected tree nodes that the user chose from the ui.
     * @return filtered issues according to the selected component and user filters.
     */
    public Set<Issue> getFilteredScanIssues(FilterManager filterManager, List<DependenciesTree> selectedNodes) {
        Set<Issue> filteredIssues = Sets.newHashSet();
        selectedNodes.forEach(node -> filteredIssues.addAll(filterManager.filterIssues(node.getIssues())));
        return filteredIssues;
    }

    /**
     * @param componentId artifact component ID.
     * @return {@link Artifact} according to the component ID.
     */
    protected Artifact getArtifactSummary(String componentId) {
        return scanCache.get(componentId);
    }

    /**
     * Recursively, extract all candidates for Xray scan.
     *
     * @param node       - In - The DependenciesTree root node.
     * @param components - Out - Components for Xray scan.
     * @param quickScan  - True if this is a quick scan. In slow scans we'll scan all components.
     */
    private void extractComponents(DependenciesTree node, Components components, boolean quickScan) {
        for (DependenciesTree child : node.getChildren()) {
            String componentId = child.toString();
            if (!quickScan || !scanCache.contains(componentId)) {
                components.addComponent(prefix.getPrefix() + componentId, "");
            }
            extractComponents(child, components, quickScan);
        }
    }

    /**
     * Scan and cache components.
     *
     * @param indicator - Progress bar.
     * @param quickScan - Quick or full scan.
     */
    protected void scanAndCacheArtifacts(ProgressIndicator indicator, boolean quickScan) {
        // Collect components to scan
        Components componentsToScan = ComponentsFactory.create();
        extractComponents(scanResults, componentsToScan, quickScan);
        if (componentsToScan.getComponentDetails().isEmpty()) {
            // No components found to scan
            return;
        }

        // Create Xray client and check version
        Xray xrayClient = XrayClient.create(xrayServerConfig.getUrl(), xrayServerConfig.getUsername(), xrayServerConfig.getPassword(), xrayServerConfig.isTrustSelfSignCert(), xrayServerConfig.getProxyConfForTargetUrl(""));
        if (!isXrayVersionSupported(xrayClient)) {
            return;
        }

        // Start scan
        try {
            int currentIndex = 0;
            List<ComponentDetail> componentsList = Lists.newArrayList(componentsToScan.getComponentDetails());
            while (currentIndex + NUMBER_OF_ARTIFACTS_BULK_SCAN < componentsList.size()) {
                checkCanceled();
                List<ComponentDetail> partialComponentsDetails = componentsList.subList(currentIndex, currentIndex + NUMBER_OF_ARTIFACTS_BULK_SCAN);
                Components partialComponents = ComponentsFactory.create(Sets.newHashSet(partialComponentsDetails));
                scanComponents(xrayClient, partialComponents);
                indicator.setFraction(((double) currentIndex + 1) / (double) componentsList.size());
                currentIndex += NUMBER_OF_ARTIFACTS_BULK_SCAN;
            }

            List<ComponentDetail> partialComponentsDetails = componentsList.subList(currentIndex, componentsList.size());
            Components partialComponents = ComponentsFactory.create(Sets.newHashSet(partialComponentsDetails));
            scanComponents(xrayClient, partialComponents);
            indicator.setFraction(1);
            scanCache.write();
        } catch (CancellationException e) {
            log.info("Xray scan was canceled");
        } catch (IOException e) {
            log.error("Scan failed", e);
        }
    }

    /**
     * Add Xray scan results from cache to the dependencies tree.
     *
     * @param node - The dependencies tree.
     */
    protected void addXrayInfoToTree(DependenciesTree node) {
        if (node == null || node.isLeaf()) {
            return;
        }
        for (DependenciesTree child : node.getChildren()) {
            populateDependenciesTreeNode(child);
            addXrayInfoToTree(child);
        }
    }

    /**
     * Xray scan a bulk of components.
     *
     * @param xrayClient      - The Xray client.
     * @param artifactsToScan - The bulk of components to scan.
     * @throws IOException in case of connection issues.
     */
    private void scanComponents(Xray xrayClient, Components artifactsToScan) throws IOException {
        SummaryResponse scanResults = xrayClient.summary().component(artifactsToScan);
        // Add scan results to cache
        scanResults.getArtifacts().stream()
                .filter(Objects::nonNull)
                .filter(summaryArtifact -> summaryArtifact.getGeneral() != null)
                .forEach(scanCache::add);
    }

    /**
     * Return true iff xray version is sufficient.
     *
     * @param xrayClient - The xray client.
     * @return true iff xray version is sufficient.
     */
    private boolean isXrayVersionSupported(Xray xrayClient) {
        try {
            if (XrayConnectionUtils.isXrayVersionSupported(xrayClient.system().version())) {
                return true;
            }
            log.error("Unsupported JFrog Xray version: Required JFrog Xray version " + Constants.MINIMAL_XRAY_VERSION_SUPPORTED + " and above");
        } catch (IOException e) {
            log.error("Scan failed", e);
        }
        return false;
    }

    /**
     * @throws CancellationException if the user clicked on the cancel button.
     */
    protected abstract void checkCanceled() throws CancellationException;
}
