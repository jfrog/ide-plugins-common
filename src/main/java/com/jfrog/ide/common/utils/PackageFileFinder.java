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
import java.util.stream.Collectors;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public class PackageFileFinder implements FileVisitor<Path> {
    private static final String EXCLUDED_DIRS_MESSAGE = "The following directories are excluded from Xray scanning due to the defined Excluded Paths pattern:";
    private final Set<String> packageJsonDirectories = Sets.newHashSet();
    private final List<String> buildGradleDirectories = Lists.newArrayList();
    private final List<String> goModDirectories = Lists.newArrayList();
    private final Set<String> yarnLockDirectories = Sets.newHashSet();
    private final Set<Path> excludedDirectories = Sets.newHashSet();
    private final PathMatcher exclusions;
    private final Path basePath;

    /**
     * @param projectPaths  - List of project base paths
     * @param basePath      - The project base path to make sure it is not excluded from scanning
     * @param excludedPaths - Pattern of project paths to exclude from Xray scanning for npm and Go projects
     * @param logger        - The logger to log excluded paths when found
     */
    public PackageFileFinder(Set<Path> projectPaths, Path basePath, String excludedPaths, Log logger) throws IOException {
        this.exclusions = FileSystems.getDefault().getPathMatcher("glob:" + excludedPaths);
        this.basePath = basePath;

        for (Path projectPath : Utils.consolidatePaths(projectPaths)) {
            Files.walkFileTree(projectPath, this);
        }
        if (!excludedDirectories.isEmpty()) {
            String message = Utils.consolidatePaths(excludedDirectories).stream()
                    .map(Path::toString)
                    .collect(Collectors.joining(System.lineSeparator()));
            logger.info(EXCLUDED_DIRS_MESSAGE + System.lineSeparator() + message);
        }
    }

    /**
     * Get package.json directories and their directories.
     *
     * @return List of package.json's parent directories.
     */
    public Set<String> getNpmPackagesFilePairs() {
        Set<String> packageJsonDirectoriesSet = Sets.newHashSet(packageJsonDirectories);
        // A yarn project might contain package.json file and shouldn't be identified as npm project.
        packageJsonDirectoriesSet.removeAll(yarnLockDirectories);
        return packageJsonDirectoriesSet;
    }

    /**
     * Get package.json directories and their directories.
     *
     * @return List of yarn.lock's parent directories.
     */
    public Set<String> getYarnPackagesFilePairs() {
        return Sets.newHashSet(yarnLockDirectories);
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
     * Get the excluded directories after scan.
     *
     * @return the excluded directories.
     */
    Set<Path> getExcludedDirectories() {
        return excludedDirectories;
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
        // Exclude the directory from scanning if it matches the exclusions pattern, and it is not the root project base path.
        if (exclusions.matches(dir) && !basePath.equals(dir)) {
            // Adding path for logging.
            excludedDirectories.add(dir);
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
        if (isYarnPackageFile(file)) {
            yarnLockDirectories.add(file.getParent().toString());
        } else if (isNpmPackageFile(file)) {
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
     * Return true iff this file is yarn.lock.
     *
     * @return true iff this file is yarn.lock.
     */
    private static boolean isYarnPackageFile(Path file) {
        return "yarn.lock".equals(file.getFileName().toString());
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
