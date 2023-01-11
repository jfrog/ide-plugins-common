package com.jfrog.ide.common.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;

import java.io.IOException;

public interface ScanLogic {
    /**
     * Scan and cache components.
     *
     * @param server        - JFrog platform server configuration.
     * @param indicator     - Progress bar.
     * @param prefix        - Components prefix for xray scan, e.g. gav:// or npm://
     * @param checkCanceled - Callback that throws an exception if scan was cancelled by user
     * @return true if the scan completed successfully, false otherwise.
     */
    boolean scanAndCacheArtifacts(ServerConfig server, ProgressIndicator indicator, ComponentPrefix prefix, Runnable checkCanceled) throws IOException, InterruptedException;

    /**
     * @param componentId artifact component ID.
     * @return {@link Artifact} according to the component ID.
     */
    Artifact getArtifactSummary(String componentId);

    /**
     * @return {@link DependencyTree} according to the last cached scan.
     */
    DependencyTree getScanResults();

    void setScanResults(DependencyTree results);
}

