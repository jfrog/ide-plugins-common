package com.jfrog.ide.common.go;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by Bar Belity on 06/02/2020.
 */

@SuppressWarnings({"unused"})
public class GoTreeBuilder {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final GoDriver goDriver;
    private final Path projectDir;
    private final Log logger;

    public GoTreeBuilder(Path projectDir, Map<String, String> env, Log logger) {
        this.projectDir = projectDir;
        this.goDriver = new GoDriver(null, env, projectDir.toFile(), logger);
        this.logger = logger;
    }

    public DependencyTree buildTree() throws IOException {
        if (!goDriver.isInstalled()) {
            logger.error("Could not scan go project dependencies, because go CLI is not in the PATH.");
            return null;
        }

        DependencyTree rootNode = createDependencyTree();
        rootNode.setGeneralInfo(new GeneralInfo()
                .componentId(rootNode.getUserObject().toString())
                .pkgType("go")
                .path(projectDir.toString())
                .artifactId(rootNode.getUserObject().toString())
                .version(""));

        setNoneScope(rootNode);
        return rootNode;
    }

    private DependencyTree createDependencyTree() throws IOException {
        // Run go mod graph.
        CommandResults goGraphResult = goDriver.modGraph(false);
        String[] dependenciesGraph = goGraphResult.getRes().split("\\r?\\n");

        // Create root node.
        String rootPackageName = goDriver.getModuleName();
        DependencyTree rootNode = new DependencyTree(goDriver.getModuleName());

        // Build dependency tree.
        Map<String, List<String>> allDependencies = new HashMap<>();
        populateAllDependenciesMap(dependenciesGraph, allDependencies);
        populateDependencyTree(rootNode, rootPackageName, allDependencies);

        return rootNode;
    }

    /**
     * Since Go doesn't have scopes, populate the direct dependencies with 'None' scope
     *
     * @param rootNode - The dependency tree root
     */
    private static void setNoneScope(DependencyTree rootNode) {
        Set<Scope> scopes = Sets.newHashSet(new Scope());
        rootNode.getChildren().forEach(child -> child.setScopes(scopes));
    }

    static void populateAllDependenciesMap(String[] dependenciesGraph, Map<String, List<String>> allDependencies) {
        for (String entry : dependenciesGraph) {
            if (StringUtils.isAllBlank(entry)) {
                continue;
            }
            String[] parsedEntry = entry.split("\\s");
            List<String> pkgDeps = allDependencies.computeIfAbsent(parsedEntry[0], k -> new ArrayList<>());
            pkgDeps.add(parsedEntry[1]);
        }
    }

    static void populateDependencyTree(DependencyTree currNode, String currNameVersionString, Map<String, List<String>> allDependencies) {
        List<String> currDependencies = allDependencies.get(currNameVersionString);
        if (currDependencies == null) {
            return;
        }
        for (String dependency : currDependencies) {
            String[] dependencyNameVersion = dependency.split("@v");
            DependencyTree DependencyTree = new DependencyTree(dependencyNameVersion[0] + ":" + dependencyNameVersion[1]);
            populateDependencyTree(DependencyTree, dependency, allDependencies);
            currNode.add(DependencyTree);
        }
    }
}
