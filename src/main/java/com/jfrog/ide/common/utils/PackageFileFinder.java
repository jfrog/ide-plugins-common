package com.jfrog.ide.common.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public class PackageFileFinder implements FileVisitor<Path> {
    private final List<String> packageJsonDirectories = Lists.newArrayList();
    private final List<String> buildGradleDirectories = Lists.newArrayList();
    private final List<String> goModDirectories = Lists.newArrayList();
    private final PathMatcher exclusions;
    private final Log logger;

    /**
     * @param projectPaths  - List of project base paths
     * @param excludedPaths - Pattern of project paths to exclude from Xray scanning for npm and Go projects
     * @param logger        - The logger to log excluded paths when found
     */
    public PackageFileFinder(Set<Path> projectPaths, String excludedPaths, Log logger) throws IOException {
        Set<Path> consolidatedPaths = Utils.consolidatePaths(projectPaths);
        this.exclusions = FileSystems.getDefault().getPathMatcher("glob:" + excludedPaths);
        this.logger = logger;

        for (Path projectPath : consolidatedPaths) {
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
     * Get build.gradle and build.gradle.kts directories and their directories.
     *
     * @return List of build.gradle and build.gradle.kts's parent directories.
     */
    public Set<String> getBuildGradlePackagesFilePairs() {
        return Sets.newHashSet(buildGradleDirectories);
    }

    /**
     * Get go.mod directories and their directories.
     *
     * @return List of go.mod's parent directories.
     */
    public Set<String> getGoPackagesFilePairs() {
        return Sets.newHashSet(goModDirectories);
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
        if (exclusions.matches(dir)) {
            logger.info("Excluding directory '" + dir + "' from Xray scanning due to the defined Excluded Paths pattern.");
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
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
        } else if (isGradlePackageFile(file)) {
            buildGradleDirectories.add(file.getParent().toString());
        } else if (isGoPackageFile(file)) {
            goModDirectories.add(file.getParent().toString());
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
     * Return true iff this file is package.json.
     *
     * @return true iff this file is package.json.
     */
    private static boolean isNpmPackageFile(Path file) {
        return "package.json".equals(file.getFileName().toString());
    }

    /**
     * Return true iff this file is build.gradle or build.gradle.kts.
     *
     * @return true iff this file is build.gradle or build.gradle.kts.
     */
    private static boolean isGradlePackageFile(Path file) {
        return StringUtils.equalsAny(file.getFileName().toString(), "build.gradle", "build.gradle.kts");
    }

    /**
     * Return true iff this file is go.mod.
     *
     * @return true iff this file is go.mod.
     */
    private static boolean isGoPackageFile(Path file) {
        return "go.mod".equals(file.getFileName().toString());
    }
}
