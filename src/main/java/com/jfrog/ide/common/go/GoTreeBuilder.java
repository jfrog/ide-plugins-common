package com.jfrog.ide.common.go;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.extractor.go.dependencyTree.GoDependencyTree;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Created by Bar Belity on 06/02/2020.
 */

@SuppressWarnings({"unused"})
public class GoTreeBuilder {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private GoDriver goDriver;
    private Path projectDir;

    public GoTreeBuilder(Path projectDir, Map<String, String> env, Log logger) {
        this.projectDir = projectDir;
        this.goDriver = new GoDriver(null, env, projectDir.toFile(), logger);
    }

    public DependenciesTree buildTree(Log logger) throws IOException {
        if (!goDriver.isGoInstalled()) {
            logger.error("Could not scan go project dependencies, because go CLI is not in the PATH.");
            return null;
        }

        DependenciesTree rootNode = GoDependencyTree.createDependenciesTree(goDriver);
        rootNode.setGeneralInfo(new GeneralInfo()
                .componentId(rootNode.getUserObject().toString())
                .pkgType("go")
                .path(projectDir.toString())
                .artifactId(rootNode.getUserObject().toString())
                .version(""));

        return rootNode;
    }
}
