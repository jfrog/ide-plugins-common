package com.jfrog.ide.common.persistency;

import com.jfrog.ide.common.ci.BuildGeneralInfo;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.stream.Collectors;

/**
 * @author yahavi
 **/
public class BuildsScanCache {

    public static final int MAX_BUILDS = 100;
    private String[] currentBuildScanCaches = new String[]{};
    private final Path buildsDir;
    private final Log logger;

    public BuildsScanCache(String projectName, Path basePath, Log logger) throws IOException {
        this.buildsDir = basePath.resolve(Base64.getEncoder().encodeToString(projectName.getBytes(StandardCharsets.UTF_8))).resolve(projectName);
        this.logger = logger;
        if (!Files.exists(buildsDir)) {
            Files.createDirectories(buildsDir);
            return;
        }
        this.currentBuildScanCaches = Arrays.stream(buildsDir.toFile().listFiles())
                .map(File::getName)
                .sorted()
                .toArray(String[]::new);
        cleanUpOldBuilds();
    }

    private void cleanUpOldBuilds() throws IOException {
        for (int i = MAX_BUILDS; i < currentBuildScanCaches.length; i++) {
            Files.delete(Paths.get(currentBuildScanCaches[i]));
        }
    }

    public SingleBuildCache getBuildCache(String buildName, String buildNumber, String timestamp) {
        try {
            return SingleBuildCache.getBuildCache(buildName, buildNumber, timestamp, buildsDir, logger);
        } catch (IOException exception) {
            logger.error(String.format("Failed reading cache file for build %s/%s, " +
                    "zapping the old cache and starting a new one", buildName, buildNumber));
        }
        return null;
    }

    public void cacheDependencyTree(DependencyTree ciDependencyTree) throws IOException {
        BuildGeneralInfo generalInfo = (BuildGeneralInfo) ciDependencyTree.getGeneralInfo();
        SingleBuildCache buildCache = new SingleBuildCache(generalInfo.getArtifactId(), generalInfo.getVersion(),
                Long.toString(generalInfo.getStarted().getTime()), buildsDir, logger);
        Enumeration<?> enumeration = ciDependencyTree.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DependencyTree node = (DependencyTree) enumeration.nextElement();
            Artifact artifact = new Artifact(node.getGeneralInfo(), node.getIssues(), node.getLicenses());
            artifact.setScopes(node.getScopes());
            artifact.setChildren(node.getChildren().stream()
                    .map(DependencyTree::getGeneralInfo)
                    .map(GeneralInfo::getComponentId)
                    .collect(Collectors.toSet()));
            buildCache.add(artifact);
        }
        buildCache.write();
    }
}
