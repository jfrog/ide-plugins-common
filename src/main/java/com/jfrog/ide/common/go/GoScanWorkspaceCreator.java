package com.jfrog.ide.common.go;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.extractor.WslUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.jfrog.ide.common.utils.Utils.createMapper;

/**
 * This FileVisitor copies all go.mod and *.go files from the input source directory to the input target directory.
 * For all go.mod files, replaces relative paths to absolute.
 * That functionality is needed, so that we can calculate the go dependencies tree on a copy of the original code project,
 * rather than on the original one, in order to avoid changing it.
 *
 * @author yahavi
 **/
public class GoScanWorkspaceCreator implements FileVisitor<Path> {
    private static final ObjectMapper jsonReader = createMapper();
    private final GoDriver goDriver;
    private final Path sourceDir;
    private final Path targetDir;
    private final Log logger;
    private final boolean runGoThroughWsl;
    private static final String[] EXCLUDED_DIRS = new String[]{".git", ".idea", ".vscode"};

    public GoScanWorkspaceCreator(String executablePath, Path sourceDir, Path targetDir,
                                  Map<String, String> env, Log logger, boolean runGoThroughWsl) {
        this.goDriver = new GoDriver(executablePath, env, targetDir.toFile(), logger, runGoThroughWsl);
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
        this.logger = logger;
        this.runGoThroughWsl = runGoThroughWsl;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        // Skip excluded directories.
        if (StringUtils.equalsAny(dir.getFileName().toString(), EXCLUDED_DIRS)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        // Skip subdirectories with go.mod files.
        // These directories are different Go projects and their go files should not be in the root project.
        if (!sourceDir.equals(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                if (files.anyMatch(file -> file.getFileName().toString().equals("go.mod"))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
        }

        Path resolve = targetDir.resolve(sourceDir.relativize(dir));
        if (Files.notExists(resolve)) {
            Files.createDirectories(resolve);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String fileName = file.getFileName().toString();

        // Go files should be copied to allow running `go list -f "{{with .Module}}{{.Path}} {{.Version}}{{end}}" all`
        // and to get the list package that are actually in use by the Go project.
        if (fileName.endsWith(".go")) {
            Files.copy(file, targetDir.resolve(sourceDir.relativize(file)));
            return FileVisitResult.CONTINUE;
        }
        // Copy the root go.mod file and replace relative path in "replace" to absolute paths.
        if (fileName.equals("go.mod")) {
            Path targetGoMod = targetDir.resolve(sourceDir.relativize(file));
            Files.copy(file, targetGoMod);
            makeReplacePathsAbsolute(targetGoMod);
            return FileVisitResult.CONTINUE;
        }
        // Files other than go.mod and *.go files are not necessary to build the dependency tree of used Go packages.
        // Therefore, we just create an empty file with the same name so go:embed files won't cause a missing file error.
        if (!fileName.equals("go.sum")) {
            Files.createFile(targetDir.resolve(sourceDir.relativize(file)));
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Rewrites every relative local path in the go.mod's replace directives to an absolute path under the source
     * directory, so the copied project still finds the modules it replaces.
     */
    private void makeReplacePathsAbsolute(Path goMod) throws IOException {
        String quotedGoModPath = quoteForShell(toPathForGo(goMod.toAbsolutePath().toString()));
        String goModJson = goDriver.runCmd(List.of("mod", "edit", "-json", quotedGoModPath), false).getRes();

        // Build a -replace flag for each directive that replaces a module with a relative local path.
        List<String> replaceFlags = new ArrayList<>();
        for (JsonNode replaceDirective : jsonReader.readTree(goModJson).path("Replace")) {
            JsonNode replacement = replaceDirective.get("New");
            String replacementPath = replacement.get("Path").asText();
            if (replacement.has("Version") || isAbsolute(replacementPath)) {
                continue;
            }
            String replaceFlag = "-replace=" + formatReplacedModule(replaceDirective.get("Old")) + "=" + makeAbsolute(replacementPath);
            replaceFlags.add(quoteForShell(replaceFlag));
        }
        if (replaceFlags.isEmpty()) {
            return;
        }

        List<String> editArgs = new ArrayList<>(List.of("mod", "edit"));
        editArgs.addAll(replaceFlags);
        editArgs.add(quotedGoModPath);
        goDriver.runCmd(editArgs, true);
    }

    /**
     * Returns the path as the go command sees it: a Linux path when Go runs through WSL, otherwise the path unchanged.
     */
    private String toPathForGo(String path) {
        if (!runGoThroughWsl) {
            return path;
        }
        return WslUtils.isWslPath(path) ? WslUtils.toLinuxPath(path) : WslUtils.windowsLocalPathToWslMount(path);
    }

    /**
     * Returns whether a path from the go.mod is absolute on the system Go runs on.
     */
    private boolean isAbsolute(String path) {
        return runGoThroughWsl ? path.startsWith("/") : Paths.get(path).isAbsolute();
    }

    /**
     * Returns the absolute path, as the go command sees it, of a path relative to the source directory.
     */
    private String makeAbsolute(String relativePath) {
        if (runGoThroughWsl) {
            return toPathForGo(sourceDir.toAbsolutePath().toString()) + "/" + relativePath;
        }
        return sourceDir.toAbsolutePath().resolve(relativePath).normalize().toString();
    }

    /**
     * Quotes an argument so that the shell GoDriver runs commands through ("/bin/sh -c" or "cmd /c") passes it to go
     * as a single argument, without interpreting its characters.
     */
    private static String quoteForShell(String arg) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "\"" + arg + "\"";
        }
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    /**
     * Returns the replaced module as the -replace flag expects it: its path, followed by "@version" when the
     * replace directive applies to one version only.
     */
    private static String formatReplacedModule(JsonNode module) {
        String path = module.get("Path").asText();
        return module.has("Version") ? path + "@" + module.get("Version").asText() : path;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        if (exc != null) {
            logger.warn("An error occurred during preparing Go workspace " + ExceptionUtils.getRootCauseMessage(exc));
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        if (exc != null) {
            logger.warn("An error occurred during preparing Go workspace " + ExceptionUtils.getRootCauseMessage(exc));
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }
}
