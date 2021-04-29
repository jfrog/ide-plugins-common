package com.jfrog.ide.common.go;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by Bar Belity on 06/02/2020.
 */

@SuppressWarnings({"unused"})
public class GoTreeBuilder {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> env;
    private final Path projectDir;
    private final Log logger;

    public GoTreeBuilder(Path projectDir, Map<String, String> env, Log logger) {
        this.projectDir = projectDir;
        this.logger = logger;
        this.env = env;
    }

    public DependencyTree buildTree() throws IOException {
        File tmpDir = copyModFileToTmpDir(projectDir).toFile();
        try {
            GoDriver goDriver = new GoDriver(null, env, tmpDir, logger);
            if (!goDriver.isInstalled()) {
                logger.error("Could not scan go project dependencies, because go CLI is not in the PATH.");
                return null;
            }

            DependencyTree rootNode = createDependencyTree(goDriver);
            setGeneralInfo(rootNode);
            setNoneScope(rootNode);
            return rootNode;
        } finally {
            FileUtils.deleteDirectory(tmpDir);
        }
    }

    /**
     * Copy go.mod file to a temporary directory.
     * This is necessary to bypass checksum mismatches issues in the original go.sum.
     *
     * @param projectDir - Go project directory
     * @return the temporary directory.
     * @throws IOException in case of any I/O error.
     */
    private Path copyModFileToTmpDir(Path projectDir) throws IOException {
        Path tmpDir = Files.createTempDirectory(null);
        Files.copy(projectDir.resolve("go.mod"), tmpDir.resolve("go.mod"));
        return tmpDir;
    }

    private DependencyTree createDependencyTree(GoDriver goDriver) throws IOException {
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

    private void setGeneralInfo(DependencyTree rootNode) {
        rootNode.setGeneralInfo(new GeneralInfo()
                .componentId(rootNode.getUserObject().toString())
                .pkgType("go")
                .path(projectDir.toString())
                .artifactId(rootNode.getUserObject().toString())
                .version(""));
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

    void populateDependencyTree(DependencyTree currNode, String currNameVersionString, Map<String, List<String>> allDependencies) {
        if (hasLoop(currNode)) {
            return;
        }
        List<String> currDependencies = allDependencies.get(currNameVersionString);
        if (currDependencies == null) {
            return;
        }
        for (String dependency : currDependencies) {
            String[] dependencyNameVersion = dependency.split("@v");
            DependencyTree DependencyTree = new DependencyTree(dependencyNameVersion[0] + ":" + dependencyNameVersion[1]);
            currNode.add(DependencyTree);
            populateDependencyTree(DependencyTree, dependency, allDependencies);
        }
    }

    /**
     * Return true if the node contains a loop.
     *
     * @param node - The dependency tree node
     * @return true if the node contains a loop
     */
    private boolean hasLoop(DependencyTree node) {
        for (DependencyTree parent = (DependencyTree) node.getParent(); parent != null; parent = (DependencyTree) parent.getParent()) {
            if (Objects.equals(node.getUserObject(), parent.getUserObject())) {
                logger.debug("Loop detected in " + node.getUserObject());
                return true;
            }
        }
        return false;
    }
}
