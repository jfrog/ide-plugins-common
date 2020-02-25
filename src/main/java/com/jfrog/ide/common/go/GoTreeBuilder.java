package com.jfrog.ide.common.go;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Bar Belity on 06/02/2020.
 */

@SuppressWarnings({"unused"})
public class GoTreeBuilder {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private GoDriver goDriver;
    private Path projectDir;
    private Log logger;

    public GoTreeBuilder(Path projectDir, Map<String, String> env, Log logger) {
        this.projectDir = projectDir;
        this.goDriver = new GoDriver(null, env, projectDir.toFile(), logger);
        this.logger = logger;
    }

    public DependenciesTree buildTree() throws IOException {
        if (!goDriver.isInstalled()) {
            logger.error("Could not scan go project dependencies, because go CLI is not in the PATH.");
            return null;
        }

        DependenciesTree rootNode = createDependenciesTree();
        rootNode.setGeneralInfo(new GeneralInfo()
                .componentId(rootNode.getUserObject().toString())
                .pkgType("go")
                .path(projectDir.toString())
                .artifactId(rootNode.getUserObject().toString())
                .version(""));

        return rootNode;
    }

    private DependenciesTree createDependenciesTree() throws IOException {
        // Run go mod graph.
        CommandResults goGraphResult = goDriver.modGraph(false);
        String[] dependenciesGraph = goGraphResult.getRes().split("\\r?\\n");

        // Create root node.
        String rootPackageName = goDriver.getModuleName();
        DependenciesTree rootNode = new DependenciesTree(goDriver.getModuleName());

        // Build dependencies tree.
        Map<String, List<String>> allDependencies = new HashMap<>();
        populateAllDependenciesMap(dependenciesGraph, allDependencies);
        populateDependenciesTree(rootNode, rootPackageName, allDependencies);

        return rootNode;
    }

    static void populateAllDependenciesMap(String[] dependenciesGraph, Map<String, List<String>> allDependencies) {
        for (String entry : dependenciesGraph) {
            if (StringUtils.isAllBlank(entry)) {
                continue;
            }
            String[] parsedEntry = entry.split("\\s");
            List<String> pkgDeps = allDependencies.get(parsedEntry[0]);
            if (pkgDeps == null) {
                pkgDeps = new ArrayList<>();
                allDependencies.put(parsedEntry[0], pkgDeps);
            }
            pkgDeps.add(parsedEntry[1]);
        }
    }

    static void populateDependenciesTree(DependenciesTree currNode, String currNameVersionString, Map<String, List<String>> allDependencies) {
        List<String> currDependencies = allDependencies.get(currNameVersionString);
        if (currDependencies == null) {
            return;
        }
        for (String dependency : currDependencies) {
            String[] dependencyNameVersion = dependency.split("@v");
            DependenciesTree dependenciesTree = new DependenciesTree(dependencyNameVersion[0] + ":" + dependencyNameVersion[1]);
            populateDependenciesTree(dependenciesTree, dependency, allDependencies);
            currNode.add(dependenciesTree);
        }
    }
}
