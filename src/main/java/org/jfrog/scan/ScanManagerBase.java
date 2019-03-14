package org.jfrog.scan;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.impl.services.summary.ComponentDetailImpl;
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
import org.jfrog.configuration.XrayServerConfig;
import org.jfrog.filter.FilterManager;
import org.jfrog.log.ProgressIndicator;
import org.jfrog.persistency.ScanCache;
import org.jfrog.utils.XrayConnectionUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.jfrog.utils.Constants.MINIMAL_XRAY_VERSION_SUPPORTED;

/**
 * @author yahavi
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@Getter
@Setter
public abstract class ScanManagerBase {

    private final static int NUMBER_OF_ARTIFACTS_BULK_SCAN = 100;
    protected static final String GAV_PREFIX = "gav://";

    private DependenciesTree scanResults;
    private XrayServerConfig xrayServerConfig;
    private ComponentPrefix prefix;
    private ScanCache scanCache;
    private String projectName;
    private Log log;

    public ScanManagerBase(String projectName, Log log, XrayServerConfig xrayServerConfig, ComponentPrefix prefix) throws IOException {
        this.projectName = projectName;
        this.scanCache = new ScanCache(projectName);
        this.log = log;
        this.xrayServerConfig = xrayServerConfig;
        this.prefix = prefix;
    }

    /**
     * Populate a ScanTreeNode with issues, licenses and general info from the scan cache.
     *
     * @param scanTreeNode - The root node.
     */
    protected void populateScanTreeNode(DependenciesTree scanTreeNode) {
        Artifact scanArtifact = getArtifactSummary(scanTreeNode.toString());
        if (scanArtifact != null) {
            scanTreeNode.setIssues(Sets.newHashSet(scanArtifact.getIssues()));
            scanTreeNode.setLicenses(Sets.newHashSet(scanArtifact.getLicenses()));
            scanTreeNode.setGeneralInfo(scanArtifact.getGeneralInfo());
        }
    }

    protected void scanTree(DependenciesTree rootNode) {
        rootNode.getChildren().forEach(child -> {
            populateScanTreeNode(child);
            scanTree(child);
        });
    }

    protected void addAllArtifacts(Components components, DependenciesTree rootNode, String prefix) {
        rootNode.getChildren().forEach(child -> {
            ComponentDetailImpl scanComponent = (ComponentDetailImpl) child.getUserObject();
            components.addComponent(prefix + scanComponent.getComponentId(), scanComponent.getSha1());
            addAllArtifacts(components, child, prefix);
        });
    }

    protected void setUiLicenses() {
        FilterManager.getInstance().setLicenses(getAllLicenses());
    }

    /**
     * @return all licenses available from the current scan results.
     */
    private Set<License> getAllLicenses() {
        Set<License> allLicenses = new HashSet<>();
        if (scanResults == null) {
            return allLicenses;
        }
        DependenciesTree node = (DependenciesTree) scanResults.getRoot();
        collectAllLicenses(node, allLicenses);
        return allLicenses;
    }

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
    public Set<Issue> getFilteredScanIssues(List<DependenciesTree> selectedNodes) {
        FilterManager filterManager = FilterManager.getInstance();
        Set<Issue> filteredIssues = Sets.newHashSet();
        selectedNodes.forEach(node -> filteredIssues.addAll(filterManager.filterIssues(node.getIssues())));
        return filteredIssues;
    }

    /**
     * @param componentId artifact component ID
     * @return {@link Artifact} according to the component ID.
     */
    protected Artifact getArtifactSummary(String componentId) {
        return scanCache.get(componentId);
    }

    private void extractComponents(DependenciesTree node, Components components, boolean quickScan) {
        for (DependenciesTree child : node.getChildren()) {
            String componentId = child.toString();
            if (!quickScan || !scanCache.contains(componentId)) {
                components.addComponent(prefix.getPrefix() + componentId, "");
                extractComponents(child, components, quickScan);
            }
        }
    }

    /**
     * Scan and cache components.
     *
     * @param indicator - Progress bar.
     * @param quickScan - Quick or full scan.
     */
    protected void scanAndCacheArtifacts(ProgressIndicator indicator, boolean quickScan) {
        Components componentsToScan = ComponentsFactory.create();
        extractComponents(scanResults, componentsToScan, quickScan);
        if (componentsToScan.getComponentDetails().isEmpty()) {
            return;
        }

        Xray xray = XrayClient.create(xrayServerConfig.getUrl(), xrayServerConfig.getUsername(), xrayServerConfig.getPassword());

        if (!isXrayVersionSupported(xray)) {
            return;
        }

        try {
            int currentIndex = 0;
            List<ComponentDetail> componentsList = Lists.newArrayList(componentsToScan.getComponentDetails());
            while (currentIndex + NUMBER_OF_ARTIFACTS_BULK_SCAN < componentsList.size()) {
                checkCanceled();
                List<ComponentDetail> partialComponentsDetails = componentsList.subList(currentIndex, currentIndex + NUMBER_OF_ARTIFACTS_BULK_SCAN);
                Components partialComponents = ComponentsFactory.create(Sets.newHashSet(partialComponentsDetails));
                scanComponents(xray, partialComponents);
                indicator.setFraction(((double) currentIndex + 1) / (double) componentsList.size());
                currentIndex += NUMBER_OF_ARTIFACTS_BULK_SCAN;
            }

            List<ComponentDetail> partialComponentsDetails = componentsList.subList(currentIndex, componentsList.size());
            Components partialComponents = ComponentsFactory.create(Sets.newHashSet(partialComponentsDetails));
            scanComponents(xray, partialComponents);
            indicator.setFraction(1);
            scanCache.write();
        } catch (RuntimeException e) {
            log.info("Xray scan was canceled");
        } catch (IOException e) {
            log.error("Scan failed", e);
        }
    }

    protected void addXrayInfoToTree(DependenciesTree node) {
        if (node == null || node.isLeaf()) {
            return;
        }
        for (DependenciesTree child : node.getChildren()) {
            populateScanTreeNode(child);
            addXrayInfoToTree(child);
        }
    }

    private void scanComponents(Xray xray, Components artifactsToScan) throws IOException {
        SummaryResponse summary = xray.summary().component(artifactsToScan);
        // Update cached artifact summary
        for (com.jfrog.xray.client.services.summary.Artifact summaryArtifact : summary.getArtifacts()) {
            if (summaryArtifact == null || summaryArtifact.getGeneral() == null) {
                continue;
            }
            scanCache.add(summaryArtifact);
        }
    }

    private boolean isXrayVersionSupported(Xray xray) {
        try {
            if (XrayConnectionUtils.isXrayVersionSupported(xray.system().version())) {
                return true;
            }
            log.error("Unsupported JFrog Xray version: Required JFrog Xray version " + MINIMAL_XRAY_VERSION_SUPPORTED + " and above");
        } catch (IOException e) {
            log.error("Scan failed", e);
        }
        return false;
    }

    protected abstract void checkCanceled();
}
