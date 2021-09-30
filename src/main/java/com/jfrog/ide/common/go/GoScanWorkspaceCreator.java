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

/**
 * This FileVisitor copies all go.mod and *.go files from the input source directory to the input target directory.
 * For all go.mod files, replaces relative paths to absolute.
 *
 * @author yahavi
 **/
public class GoScanWorkspaceCreator implements FileVisitor<Path> {
    private final GoDriver goDriver;
    private final Path sourceDir;
    private final Path targetDir;
    private final Log logger;

    public GoScanWorkspaceCreator(Path sourceDir, Path targetDir, Path goModAbsDir, Map<String, String> env, Log logger) {
        this.goDriver = new GoDriver(null, env, goModAbsDir.toFile(), logger);
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
        this.logger = logger;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
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
        // Copy go.mod files and replace relative path in "replace" to absolute paths.
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
