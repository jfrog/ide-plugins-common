package com.jfrog.ide.common.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.tree.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;

import java.io.IOException;
import java.util.Map;

public interface ScanLogic {
    // TODO: update comment
    /**
     * Scan and cache components.
     *
     * @param server    - JFrog platform server configuration.
     * @param indicator - Progress bar.
     * @return true if the scan completed successfully, false otherwise.
     */
    Map<String, Artifact> scanArtifacts(DependencyTree dependencyTree, ServerConfig server, ProgressIndicator indicator, ComponentPrefix prefix, Runnable checkCanceled) throws IOException, InterruptedException;
}

