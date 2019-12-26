package com.jfrog.ide.common.npm;

import com.google.common.collect.Lists;
import com.jfrog.ide.common.utils.Utils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

/**
 * Find package.json files within the input paths.
 *
 * @author yahavi
 */
public class NpmPackageFileFinder implements FileVisitor<Path> {

    private List<String> packageJsonDirectories = Lists.newArrayList();
    private PathMatcher pathMatcher;
    private Set<Path> projectPaths;

    /**
     * @param projectPaths  - List of project base paths.
     * @param excludedPaths - Pattern of project paths to exclude from Xray scanning for npm
     */
    public NpmPackageFileFinder(Set<Path> projectPaths, String excludedPaths) {
        this.projectPaths = Utils.consolidatePaths(projectPaths);
        this.pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + excludedPaths);
    }

    /**
     * Get package.json directories and their directories.
     *
     * @return List of package.json's parent directories.
     * @throws IOException if an I/O error is thrown by a visitor method.
     */
    public List<String> getPackageFilePairs() throws IOException {
        for (Path projectPath : projectPaths) {
            Files.walkFileTree(projectPath, this);
        }
        return packageJsonDirectories;
    }

    /**
     * Skip excluded directories like node_modules.
     *
     * @param dir   - Current directory.
     * @param attrs - Directory attributes.
     * @return FileVisitResult.CONTINUE or FileVisitResult.SKIP_SUBTREE respectfully.
     */
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return !pathMatcher.matches(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
    }

    /**
     * Add package.json files to packageJsonDirectories set.
     *
     * @param file  - Current file.
     * @param attrs - File attributes.
     * @return FileVisitResult.CONTINUE.
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (isPackageFile(file)) {
            packageJsonDirectories.add(file.getParent().toString());
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    /**
     * Skip sub directories without permissions
     *
     * @param dir - Current directory.
     * @param exc - IOException or null.
     * @return FileVisitResult.CONTINUE or FileVisitResult.SKIP_SUBTREE respectfully.
     */
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        return exc == null ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
    }

    /**
     * return true iff this file is package.json.
     *
     * @return true iff this file is package.json.
     */
    private static boolean isPackageFile(Path file) {
        return "package.json".equals(file.getFileName().toString());
    }
}
