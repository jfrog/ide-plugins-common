package com.jfrog.ide.common.scan;

import com.jfrog.ide.common.components.DependencyNode;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import org.jfrog.build.extractor.scan.DependencyTree;

import java.io.IOException;
import java.util.Map;

public interface ScanLogic {
    Map<String, DependencyNode> scanArtifacts(DependencyTree dependencyTree, ServerConfig server, ProgressIndicator indicator, ComponentPrefix prefix, Runnable checkCanceled) throws IOException, InterruptedException;
}

