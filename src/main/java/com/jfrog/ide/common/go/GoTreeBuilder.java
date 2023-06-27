package com.jfrog.ide.common.go;

import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.Version;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.go.GoDriver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Bar Belity on 06/02/2020.
 */

@SuppressWarnings({"unused"})
public class GoTreeBuilder {
    // Required files of the gomod-absolutizer Go program
    private static final String[] GO_MOD_ABS_COMPONENTS = new String[]{"go.mod", "go.sum", "main.go", "utils.go"};
    private static final Version MIN_GO_VERSION_FOR_BUILD_VCS_FLAG = new Version("1.18");
    public static final String GO_VERSION_PATTERN = "^go(\\d*.\\d*.*\\d*)";
    private static final String GO_SOURCE_CODE_PREFIX = "github.com/golang/go:";
    static final Version MIN_GO_VERSION = new Version("1.16");
    private final Map<String, String> env;
    private final String executablePath;
    private final Path projectDir;
    private final String descriptorFilePath;
    private final Log logger;

    public GoTreeBuilder(String executablePath, Path projectDir, String descriptorFilePath, Map<String, String> env, Log logger) {
        this.executablePath = executablePath;
        this.projectDir = projectDir;
        this.descriptorFilePath = descriptorFilePath;
        this.logger = logger;
        this.env = env;
    }

    /**
     * Create Go dependency tree of actually used dependencies.
     *
     * @param goDriver     Go driver
     * @param logger       the logger
     * @param verbose      verbose logging
     * @param dontBuildVcs skip VCS stamping - can be used only on Go later than 1.18
     * @return Go dependency tree
     * @throws IOException in case of any I/O error.
     */
    public static DepTree createDependencyTree(GoDriver goDriver, Log logger, boolean verbose, boolean dontBuildVcs) throws IOException {
        // Run go mod graph.
        // Not all the dependencies returned are used.
        CommandResults goGraphResult = goDriver.modGraph(verbose);
        String[] dependenciesGraph = goGraphResult.getRes().split("\\r?\\n");

        CommandResults usedModulesResults;
        try {
            usedModulesResults = goDriver.getUsedModules(false, false, dontBuildVcs);
        } catch (IOException e) {
            // Errors occurred during running "go list". Run again and this time ignore errors.
            usedModulesResults = goDriver.getUsedModules(false, true, dontBuildVcs);
            logger.warn("Errors occurred during building the Go dependency tree. The dependency tree may be incomplete:" +
                    System.lineSeparator() + ExceptionUtils.getRootCauseMessage(e));
        }
        Set<String> usedDependencies = Arrays.stream(usedModulesResults.getRes().split("\\r?\\n"))
                .map(String::trim)
                .map(usedModule -> usedModule.replace(" v", ":"))
                .collect(Collectors.toSet());

        String rootPackageName = goDriver.getModuleName();
        Map<String, DepTreeNode> nodes = createNodes(usedDependencies);
        DepTree depTree = new DepTree(rootPackageName, nodes);
        populateChildren(depTree, dependenciesGraph);
        return depTree;
    }

    private static Map<String, DepTreeNode> createNodes(Set<String> usedDependencies) {
        Map<String, DepTreeNode> nodes = new HashMap<>();
        for (String dependencyId : usedDependencies) {
            DepTreeNode node = new DepTreeNode();
            nodes.put(dependencyId, node);
        }
        return nodes;
    }

    /**
     * Return true if the Go version below 1.16 or in case of a failure in parsing the version.
     * Failure in parsing the go version command implies a future version and therefore >= 1.16.
     *
     * @param versionRes - "go version" command results
     * @param logger     - The logger
     * @return the parsed Go version or fallback to "1.16"
     */
    static Version parseGoVersion(CommandResults versionRes, Log logger) {
        String[] versionSplit = StringUtils.split(versionRes.getRes());
        if (ArrayUtils.getLength(versionSplit) < 4 || !versionSplit[2].matches(GO_VERSION_PATTERN)) {
            logger.info("Couldn't not retrieve Go version from version command results: " + versionRes.getRes() + ". Assuming >= 1.16");
            // This in case of a future Go version that may return a version string different than the expected.
            // This future version is not below 1.16 and therefore we should return false.
            return MIN_GO_VERSION;
        }
        return new Version(StringUtils.substringAfter(versionSplit[2], "go"));
    }

    /**
     * Copy go.mod file to a temporary directory.
     * This is necessary to bypass checksum mismatches issues in the original go.sum.
     *
     * @return the temporary directory.
     * @throws IOException in case of any I/O error.
     */
    private Path createGoWorkspace() throws IOException {
        Path targetDir = Files.createTempDirectory(null);
        Path goModAbsDir = null;
        try {
            goModAbsDir = prepareGoModAbs();
            GoScanWorkspaceCreator goScanWorkspaceCreator = new GoScanWorkspaceCreator(executablePath, projectDir, targetDir, goModAbsDir, env, logger);
            Files.walkFileTree(projectDir, goScanWorkspaceCreator);
        } finally {
            if (goModAbsDir != null) {
                FileUtils.deleteQuietly(goModAbsDir.toFile());
            }
        }
        return targetDir;
    }

    /**
     * Copy gomod-absolutizer Go files to a temp directory.
     * The gomod-absolutizer is used to change relative paths in go.mod files to absolute paths.
     *
     * @throws IOException in case of any I/O error.
     */
    private Path prepareGoModAbs() throws IOException {
        Path goModAbsDir = Files.createTempDirectory(null);
        for (String fileName : GO_MOD_ABS_COMPONENTS) {
            try (InputStream is = getClass().getResourceAsStream("/gomod-absolutizer/" + fileName);
                 OutputStream os = new FileOutputStream(goModAbsDir.resolve(fileName).toFile())) {
                if (is == null) {
                    throw new IOException("Couldn't find resource /gomod-absolutizer/" + fileName);
                }
                is.transferTo(os);
            }
        }
        return goModAbsDir;
    }

    private static void populateChildren(DepTree depTree, String[] dependenciesGraph) {
        Map<String, DepTreeNode> nodes = depTree.getNodes();
        for (String entry : dependenciesGraph) {
            if (StringUtils.isAllBlank(entry)) {
                continue;
            }
            String[] parsedEntry = entry.replace("@v", ":").split("\\s");
            String parentId = parsedEntry[0];
            String childId = parsedEntry[1];
            if (!nodes.containsKey(childId) || !nodes.containsKey(parentId)) {
                // Parent or child is not in use
                continue;
            }
            nodes.get(parentId).getChildren().add(childId);
        }
    }

    public DepTree buildTree() throws IOException {
        File tmpDir = createGoWorkspace().toFile();
        try {
            GoDriver goDriver = new GoDriver(executablePath, env, tmpDir, logger);
            if (!goDriver.isInstalled()) {
                throw new IOException("Could not scan the Go project dependencies, because the Go executable is not in the PATH.");
            }

            CommandResults versionRes = goDriver.version(false);
            Version goVersion = parseGoVersion(versionRes, logger);
            goDriver.modTidy(false, goVersion.isAtLeast(MIN_GO_VERSION));
            DepTree depTree = createDependencyTree(goDriver, logger, false, goVersion.isAtLeast(MIN_GO_VERSION_FOR_BUILD_VCS_FLAG));
            addGoVersionNode(depTree, goVersion);
            depTree.getRootNode().descriptorFilePath(descriptorFilePath);
            return depTree;
        } finally {
            FileUtils.deleteDirectory(tmpDir);
        }
    }

    private void addGoVersionNode(DepTree depTree, Version goVersion) {
        String goCompId = GO_SOURCE_CODE_PREFIX + goVersion;
        DepTreeNode goVersionNode = new DepTreeNode();
        depTree.getNodes().put(goCompId, goVersionNode);
        depTree.getRootNode().getChildren().add(goCompId);
    }
}
