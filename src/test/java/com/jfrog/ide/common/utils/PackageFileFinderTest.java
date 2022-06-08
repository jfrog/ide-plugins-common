package com.jfrog.ide.common.utils;

import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.Test;
import org.testng.collections.Sets;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author yahavi
 **/
public class PackageFileFinderTest {
    private static final Path PROJECTS_DIR = Paths.get(".").toAbsolutePath().normalize()
            .resolve(Paths.get("src", "test", "resources", "packageFinder"));

    @Test
    public void testNoExclusions() throws IOException {
        PackageFileFinder packageFileFinder = new PackageFileFinder(Sets.newHashSet(PROJECTS_DIR), PROJECTS_DIR,
                "", new NullLog());
        assertPackageFilesIncluded(packageFileFinder);
        assertEquals(packageFileFinder.getExcludedDirectories(), Sets.newHashSet());
    }

    @Test
    public void testDefaultExclusions() throws IOException {
        PackageFileFinder packageFileFinder = new PackageFileFinder(Sets.newHashSet(PROJECTS_DIR), PROJECTS_DIR,
                "**/*{.idea,test,node_modules}*", new NullLog());
        assertPackageFilesIncluded(packageFileFinder);
        assertEquals(packageFileFinder.getExcludedDirectories().size(), 1);
        assertTrue(packageFileFinder.getExcludedDirectories().stream().anyMatch(path -> path.endsWith("test")));
    }

    @Test
    public void testMultiProjects() throws IOException {
        Path goProject = PROJECTS_DIR.resolve("go");
        Path npmProject = PROJECTS_DIR.resolve("npm");
        Path yarnProject = PROJECTS_DIR.resolve("yarn");
        Path gradleProject = PROJECTS_DIR.resolve("gradle");
        PackageFileFinder packageFileFinder = new PackageFileFinder(
                Sets.newHashSet(PROJECTS_DIR, goProject, npmProject, yarnProject, gradleProject), PROJECTS_DIR,
                "**/*{.idea,test,node_modules}*", new NullLog());
        assertPackageFilesIncluded(packageFileFinder);
        assertEquals(packageFileFinder.getExcludedDirectories().size(), 1);
        assertTrue(packageFileFinder.getExcludedDirectories().stream().anyMatch(path -> path.endsWith("test")));
    }

    private void assertPackageFilesIncluded(PackageFileFinder packageFileFinder) {
        assertEquals(packageFileFinder.getGoPackagesFilePairs().size(), 1);
        assertTrue(packageFileFinder.getGoPackagesFilePairs().stream().anyMatch(path -> path.endsWith("go")));
        assertEquals(packageFileFinder.getYarnPackagesFilePairs().size(), 1);
        assertTrue(packageFileFinder.getYarnPackagesFilePairs().stream().anyMatch(path -> path.endsWith("yarn")));
        assertEquals(packageFileFinder.getNpmPackagesFilePairs().size(), 1);
        assertTrue(packageFileFinder.getNpmPackagesFilePairs().stream().anyMatch(path -> path.endsWith("npm")));
        assertEquals(packageFileFinder.getBuildGradlePackagesFilePairs().size(), 3);
        assertTrue(packageFileFinder.getBuildGradlePackagesFilePairs().stream().anyMatch(path -> path.endsWith("gradle")));
    }
}
