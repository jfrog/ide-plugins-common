package com.jfrog.ide.common.go;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.go.GoDriver;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.stream.Stream;

/**
 * This FileVisitor copies all go.mod and *.go files from the input source directory to the input target directory.
 * For all go.mod files, replaces relative paths to absolute.
 * That functionality is needed, so that we can calculate the go dependencies tree on a copy of the original code project,
 * rather than on the original one, in order to avoid changing it.
 *
 * @author yahavi
 **/
public class GoScanWorkspaceCreator implements FileVisitor<Path> {
    private final GoDriver goDriver;
    private final Path sourceDir;
    private final Path targetDir;
    private final Log logger;

    public GoScanWorkspaceCreator(String executablePath, Path sourceDir, Path targetDir, Path goModAbsDir, Map<String, String> env, Log logger) {
        this.goDriver = new GoDriver(executablePath, env, goModAbsDir.toFile(), logger);
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
        this.logger = logger;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
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
            goDriver.runCmd("run . -goModPath=" + targetGoMod.toAbsolutePath() + " -wd=" + sourceDir.toAbsolutePath(), true);
        }
        // Files other than go.mod and *.go files are not necessary to build the dependency tree of used Go packages.
        return FileVisitResult.CONTINUE;
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
