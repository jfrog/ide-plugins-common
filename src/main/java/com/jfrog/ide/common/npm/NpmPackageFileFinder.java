package com.jfrog.ide.common.npm;

import com.google.common.collect.Lists;
import com.jfrog.ide.common.utils.Utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Find package.json files within the input paths.
 *
 * @author yahavi
 */
public class NpmPackageFileFinder implements FileVisitor<Path> {

    private static final String[] EXCLUDED_DIRS = {"node_modules", ".idea"};
    private List<String> packageJsonDirectories = Lists.newArrayList();
    private Set<Path> projectPaths;

    public NpmPackageFileFinder(Set<Path> projectPaths) {
        this.projectPaths = Utils.consolidatePaths(projectPaths);
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
        return !isDirExcluded(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
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

    /**
     * Return true if directory is excluded from search.
     *
     * @param dir - The directory to check.
     * @return true iff the input directory is excluded.
     */
    private static boolean isDirExcluded(Path dir) {
        return Arrays.stream(EXCLUDED_DIRS)
                .anyMatch(excludedDir -> dir.toString().contains(excludedDir));
    }
}
