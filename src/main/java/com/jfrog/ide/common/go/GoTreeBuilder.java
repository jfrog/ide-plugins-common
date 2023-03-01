package com.jfrog.ide.common.go;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.Version;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.extractor.go.extractor.GoDependencyTree;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

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
    private static final ObjectMapper objectMapper = new ObjectMapper();
    static final Version MIN_GO_VERSION = new Version("1.16");
    private final Map<String, String> env;
    private final String executablePath;
    private final Path projectDir;
    private final Log logger;

    public GoTreeBuilder(String executablePath, Path projectDir, Map<String, String> env, Log logger) {
        this.executablePath = executablePath;
        this.projectDir = projectDir;
        this.logger = logger;
        this.env = env;
    }

    public DependencyTree buildTree() throws IOException {
        File tmpDir = createGoWorkspace().toFile();
        try {
            GoDriver goDriver = new GoDriver(executablePath, env, tmpDir, logger);
            if (!goDriver.isInstalled()) {
                throw new IOException("Could not scan go project dependencies, because go CLI is not in the PATH.");
            }

            CommandResults versionRes = goDriver.version(false);
            Version goVersion = parseGoVersion(versionRes, logger);
            goDriver.modTidy(false, goVersion.isAtLeast(MIN_GO_VERSION));
            DependencyTree rootNode = GoDependencyTree.createDependencyTree(goDriver, logger, false, goVersion.isAtLeast(MIN_GO_VERSION_FOR_BUILD_VCS_FLAG));
            addGoVersionNode(rootNode, goVersion);
            setGeneralInfo(rootNode);
            setNoneScope(rootNode);
            return rootNode;
        } finally {
            FileUtils.deleteDirectory(tmpDir);
        }
    }

    private void addGoVersionNode(DependencyTree rootNode, Version goVersion) {
        DependencyTree goVersionNode = new DependencyTree(GO_SOURCE_CODE_PREFIX + goVersion);
        rootNode.add(goVersionNode);
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

    private void setGeneralInfo(DependencyTree rootNode) {
        rootNode.setGeneralInfo(new GeneralInfo()
                .componentId(rootNode.getUserObject().toString())
                .pkgType("go")
                .path(projectDir.toString()));
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
}
