package com.jfrog.ide.common.scan;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.services.summary.ComponentDetail;
import com.jfrog.xray.client.services.summary.Components;
import com.jfrog.xray.client.services.summary.SummaryResponse;
import com.jfrog.xray.client.services.system.Version;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.api.util.Log;


import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;

import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;


/**
 * This class includes the implementation of the Component Summary Scan Logic, which is used with older Xray versions.
 * The logic uses the component/summary REST API of Xray,
 * which doesn't take into consideration the policy configured in Xray.
 * @author tala
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@Getter
@Setter
public class ComponentSummaryScanLogic implements ScanLogic {
    public static final String MINIMAL_XRAY_VERSION_SUPPORTED = "2.5.0";
    private static final int NUMBER_OF_ARTIFACTS_BULK_SCAN = 100;
    private ScanCache scanCache;
    private Log log;
    private DependencyTree scanResults;

    public ComponentSummaryScanLogic(ScanCache scanCache, Log log) {
        this.scanCache = scanCache;
        this.log = log;
    }

    /**
     * Recursively, extract all candidates for Xray scan.
     *
     * @param node       - In - The DependencyTree root node.
     * @param components - Out - Components for Xray scan.
     * @param quickScan  - True if this is a quick scan. In slow scans we'll scan all components.
     */
    public void extractComponents(DependencyTree node, Components components, ComponentPrefix prefix, boolean quickScan) {
        for (DependencyTree child : node.getChildren()) {
            String componentId = child.toString();
            if (!quickScan || !scanCache.contains(componentId)) {
                components.addComponent(prefix.getPrefix() + componentId, "");
            }
            extractComponents(child, components, prefix, quickScan);
        }
    }

    /**
     * Scan and cache components.
     *
     * @param server    - JFrog platform server configuration.
     * @param indicator - Progress bar.
     * @param quickScan - Quick or full scan.
     * @return true if the scan completed successfully, false otherwise.
     */
    public boolean scanAndCacheArtifacts(ServerConfig server, ProgressIndicator indicator, boolean quickScan, ComponentPrefix prefix, Runnable checkCanceled) throws IOException {
        // Collect components to scan
        Components componentsToScan = ComponentsFactory.create();
        extractComponents(scanResults, componentsToScan, prefix, quickScan);
        if (componentsToScan.getComponentDetails().isEmpty()) {
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
            int currentIndex = 0;
            List<ComponentDetail> componentsList = Lists.newArrayList(componentsToScan.getComponentDetails());
            log.debug("Starting to scan " + componentsList.size() + " components.");
            while (currentIndex + NUMBER_OF_ARTIFACTS_BULK_SCAN < componentsList.size()) {
                checkCanceled.run();
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
            log.debug("Saving scan cache...");
            scanCache.write();
            log.debug("Scan cache saved successfully.");
        } catch (CancellationException e) {
            log.info("Xray scan was canceled.");
            return false;
        }
        return true;
    }

    @Override
    public Artifact getArtifactSummary(String componentId) {
        return scanCache.get(componentId);
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
    private boolean isSupportedInXrayVersion(Xray xrayClient) {
        try {
            if (isSupportedInXrayVersion(xrayClient.system().version())) {
                return true;
            }
            log.error("Unsupported JFrog Xray version: Required JFrog Xray version " + MINIMAL_XRAY_VERSION_SUPPORTED + " and above.");
        } catch (IOException e) {
            log.error("JFrog Xray Scan failed. Please check your credentials.", e);
        }
        return false;
    }

    public static boolean isSupportedInXrayVersion(Version version) {
        return version.isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED);
    }
}
