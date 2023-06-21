package com.jfrog.ide.common.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.DependencyNode;

import java.io.IOException;
import java.util.Map;

public interface ScanLogic {
    Map<String, DependencyNode> scanArtifacts(DepTree dependencyTree, ServerConfig server, ProgressIndicator indicator, ComponentPrefix prefix, Runnable checkCanceled) throws IOException, InterruptedException;
}

