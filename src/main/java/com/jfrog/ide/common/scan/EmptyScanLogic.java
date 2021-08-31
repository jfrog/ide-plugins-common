package com.jfrog.ide.common.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.utils.Constants;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.api.util.Log;


import java.io.IOException;

public class EmptyScanLogic implements ScanLogic{
    private  Log log;
    public EmptyScanLogic(Log log){
        this.log =log;

    }
    @Override
    public boolean scanAndCacheArtifacts(ServerConfig server, ProgressIndicator indicator, boolean quickScan, ComponentPrefix prefix, Runnable checkCanceled) throws IOException {
        log.error("Unsupported JFrog Xray version: Required JFrog Xray version " + Constants.MINIMAL_XRAY_VERSION_SUPPORTED + " and above.");
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
}
