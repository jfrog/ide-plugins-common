package com.jfrog.ide.common.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jfrog.ide.common.npm.NpmPackageFileFinder;
import com.jfrog.xray.client.services.summary.General;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yahavi
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Utils {

    public static String createLicenseString(License license) {
        if (license.isFullNameEmpty() || StringUtils.isBlank(license.getName())) {
            return license.getName();
        }
        return license.getFullName() + " (" + license.getName() + ")";
    }

    /**
     * Get package.json parent directories.
     *
     * @param paths - Input - List of project base paths.
     * @return set of directories containing package.json files.
     * @throws IOException if an I/O error is thrown by a visitor method.
     */
    public static Set<String> findPackageJsonDirs(Set<Path> paths) throws IOException {
        NpmPackageFileFinder npmPackageFileFinder = new NpmPackageFileFinder(paths);
        return Sets.newHashSet(npmPackageFileFinder.getPackageFilePairs());
    }

    public static GeneralInfo getGeneralInfo(General other) {
        return new GeneralInfo().componentId(other.getComponentId())
                .name(other.getName())
                .path(other.getPath())
                .pkgType(other.getPkgType());
    }

    public static License toLicense(com.jfrog.xray.client.services.summary.License other) {
        List<String> moreInfoUrl = Lists.newArrayList(ListUtils.emptyIfNull(other.moreInfoUrl()));
        return new License(Lists.newArrayList(other.getComponents()), other.getFullName(), other.getName(), moreInfoUrl);
    }

    public static Issue toIssue(com.jfrog.xray.client.services.summary.Issue other) {
        Severity severity = Severity.fromString(other.getSeverity());
        return new Issue(other.getCreated(), other.getDescription(), other.getIssueType(), other.getProvider(), severity, other.getSummary());
    }

    public static Artifact getArtifact(com.jfrog.xray.client.services.summary.Artifact other) {
        Artifact artifact = new Artifact();
        artifact.setGeneralInfo(getGeneralInfo(other.getGeneral()));
        Set<Issue> issues = other.getIssues().stream().map(Utils::toIssue).collect(Collectors.toSet());
        Set<License> licenses = other.getLicenses().stream().map(Utils::toLicense).collect(Collectors.toSet());
        artifact.setIssues(issues);
        artifact.setLicenses(licenses);
        return artifact;
    }

    /**
     * Returns Set of Paths cleaned of subdirectories.
     * For example the set ["/a", "/b/c", "/a/d"] will become ["/a", "/b/c"]
     *
     * @param paths Paths to consolidate.
     * @return Set of Paths cleaned of subdirectories.
     */
    public static Set<Path> consolidatePaths(Set<Path> paths) {
        Set<Path> finalPaths = new HashSet<>();
        // Create a sorted by length list of paths
        List<Path> sortedList = paths.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .sorted(Comparator.comparingInt(Path::getNameCount))
                .collect(Collectors.toList());
        sortedList.forEach(currentPath -> {
                    boolean isRootPath = true;
                    // Iterate over the sorted by directories count length list.
                    for (Path shorterPath : sortedList) {
                        // CurrentPath is shorter or equals to the shortPath therefore all the next paths can't contain the currentPath
                        if (currentPath.getNameCount() <= shorterPath.getNameCount()) {
                            break;
                        }
                        // The currentPath is subPath and we should not add it to the list
                        if (currentPath.startsWith(shorterPath)) {
                            isRootPath = false;
                            break;
                        }
                    }
                    if (isRootPath) {
                        finalPaths.add(currentPath);
                    }
                }
        );
        return finalPaths;
    }
}
