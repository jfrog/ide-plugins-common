package com.jfrog.ide.common.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;
import com.jfrog.xray.client.services.system.Version;


import java.io.IOException;


public class GraphScanLogic implements ScanLogic{
    public static final String MINIMAL_XRAY_VERSION_SUPPORTED_FOR_GRAPH_SCAN = "3.29.0";

    @Override
    public boolean scanAndCacheArtifacts(ServerConfig server, ProgressIndicator indicator, boolean quickScan, ComponentPrefix prefix, Runnable checkCanceled) throws IOException {
        return false;
    }

    @Override
    public Artifact getArtifactSummary(String componentId) {
        return null;
    }

    @Override
    public DependencyTree getScanResults() {
        return null;
    }

    @Override
    public void setScanResults(DependencyTree results) {
        
    }

    public static boolean isXrayVersionSupported(Version xrayVersion){
        return xrayVersion.isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED_FOR_GRAPH_SCAN);
    }
}
