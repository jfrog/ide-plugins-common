package com.jfrog.ide.common.scan;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.services.graph.GraphResponse;
import com.jfrog.xray.client.services.summary.ComponentDetail;
import com.jfrog.xray.client.services.summary.Components;
import com.jfrog.xray.client.services.summary.SummaryResponse;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;
import com.jfrog.xray.client.services.system.Version;


import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;

import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;

@SuppressWarnings({"WeakerAccess", "unused"})
@Getter
@Setter
public class GraphScanLogic implements ScanLogic {
    public static final String MINIMAL_XRAY_VERSION_SUPPORTED_FOR_GRAPH_SCAN = "3.29.0";
    private ScanCache scanCache;
    private Log log;
    private DependencyTree scanResults;

    @Override
    public boolean scanAndCacheArtifacts(ServerConfig server, ProgressIndicator indicator, boolean quickScan, ComponentPrefix prefix, Runnable checkCanceled) throws IOException {
        // Xray's graph scan API does not support progress indication at this moment.
        indicator.setIndeterminate(true);

        if (scanResults.getChildren().isEmpty()) {
            log.debug("No components found to scan. '");
            // No components found to scan
            return false;
        }

        try {
            // Create Xray client and check version
            Xray xrayClient = createXrayClientBuilder(server, log).build();
            if (!isXrayVersionSupported(xrayClient)) {
                return false;
            }

            // Start scan
            log.debug("Starting to scan, sending a dependency graph to Xray");
            checkCanceled.run();
            scanComponents(xrayClient, scanResults, server.getProject());

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

    public static boolean isXrayVersionSupported(Version xrayVersion) {
        return xrayVersion.isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED_FOR_GRAPH_SCAN);
    }

    private boolean isXrayVersionSupported(Xray xrayClient) {
        try {
            if (isXrayVersionSupported(xrayClient.system().version())) {
                return true;
            }
            log.error("Unsupported JFrog Xray version: Required JFrog Xray version " + MINIMAL_XRAY_VERSION_SUPPORTED_FOR_GRAPH_SCAN + " and above.");
        } catch (IOException e) {
            log.error("JFrog Xray Scan failed. Please check your credentials.", e);
        }
        return false;
    }

    /**
     * Xray scan a bulk of components.
     *
     * @param xrayClient      - The Xray client.
     * @param artifactsToScan - The bulk of components to scan.
     * @throws IOException in case of connection issues.
     */
    private void scanComponents(Xray xrayClient, DependencyTree artifactsToScan, String project) throws IOException {
        GraphResponse scanResults = null;
        try {
            scanResults = xrayClient.graph().graph(artifactsToScan, project);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Add scan results to cache
//        scanResults.getViolations().stream()
//                .filter(Objects::nonNull)
//                .filter(summaryArtifact -> summaryArtifact.getSummary() != null)
//                .forEach(scanCache::add);
    }
}
