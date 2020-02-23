package com.jfrog.ide.common.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

/**
 * Find package.json and go.mod files within the input paths.
 *
 * @author yahavi
 */
public class PackageFileFinder implements FileVisitor<Path> {

    private List<String> packageJsonDirectories = Lists.newArrayList();
    private List<String> gomodDirectories = Lists.newArrayList();
    private PathMatcher pathMatcher;
    private Set<Path> projectPaths;

    /**
     * @param projectPaths  - List of project base paths.
     * @param excludedPaths - Pattern of project paths to exclude from Xray scanning for npm
     */
    public PackageFileFinder(Set<Path> projectPaths, String excludedPaths) throws IOException {
        this.projectPaths = Utils.consolidatePaths(projectPaths);
        this.pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + excludedPaths);

        for (Path projectPath : projectPaths) {
            Files.walkFileTree(projectPath, this);
        }
    }

    /**
     * Get package.json directories and their directories.
     *
     * @return List of package.json's parent directories.
     */
    public Set<String> getNpmPackagesFilePairs() {
        return Sets.newHashSet(packageJsonDirectories);
    }

    /**
     * Get go.mod directories and their directories.
     *
     * @return List of go.mod's parent directories.
     */
    public Set<String> getGoPackagesFilePairs() {
        return Sets.newHashSet(gomodDirectories);
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
        if (isNpmPackageFile(file)) {
            packageJsonDirectories.add(file.getParent().toString());
        } else if (isGoPackageFile(file)) {
            gomodDirectories.add(file.getParent().toString());
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
    private static boolean isNpmPackageFile(Path file) {
        return "package.json".equals(file.getFileName().toString());
    }

    /**
     * return true iff this file is go.mod.
     *
     * @return true iff this file is go.mod.
     */
    private static boolean isGoPackageFile(Path file) {
        return "go.mod".equals(file.getFileName().toString());
    }
}
