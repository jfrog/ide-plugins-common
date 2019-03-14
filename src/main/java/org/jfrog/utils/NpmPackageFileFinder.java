package org.jfrog.utils;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

/**
 * @author yahavi
 */
public class NpmPackageFileFinder implements FileVisitor<Path> {

    private static final List<String> EXCLUDED_DIRS = Lists.newArrayList("node_modules", ".idea");
    private Set<Path> projectPaths;
    private List<String> applicationPaths = Lists.newArrayList();

    NpmPackageFileFinder(Set<Path> projectPaths) {
        this.projectPaths = Utils.consolidatePaths(projectPaths);
    }

    /**
     * Get package.json directories and their application names.
     *
     * @return List of package.json's parent directories.
     * @throws IOException if an I/O error is thrown by a visitor method
     */
    List<String> getPackageFilePairs() throws IOException {
        for (Path projectPath : projectPaths) {
            Files.walkFileTree(projectPath, this);
        }
        return applicationPaths;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return !isDirExcluded(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        String fileName = file.getFileName().toString();
        if (isPackageFile(fileName)) {
            applicationPaths.add(file.getParent().toString());
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        // Skip sub directories without permissions
        return exc == null ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
    }

    private static boolean isPackageFile(String fileName) {
        return "package.json".equals(fileName);
    }

    private static boolean isDirExcluded(Path filePath) {
        for (String excludedDir : EXCLUDED_DIRS) {
            if (filePath.toString().contains(excludedDir)) {
                return true;
            }
        }
        return false;
    }
}
