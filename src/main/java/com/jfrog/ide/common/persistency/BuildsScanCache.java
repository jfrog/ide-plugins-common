package com.jfrog.ide.common.persistency;

import com.google.common.collect.Sets;
import com.jfrog.ide.common.ci.BuildGeneralInfo;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.jfrog.ide.common.ci.Utils.*;
import static com.jfrog.ide.common.persistency.Utils.dependencyNodeToArtifact;

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

    public void saveDependencyTree(DependencyTree buildDependencyTree) throws IOException {
        BuildGeneralInfo generalInfo = (BuildGeneralInfo) buildDependencyTree.getGeneralInfo();
        SingleBuildCache buildCache = new SingleBuildCache(generalInfo.getArtifactId(), generalInfo.getVersion(),
                Long.toString(generalInfo.getStarted().getTime()), buildsDir, logger, generalInfo.getStatus(), generalInfo.getVcs());

        // Add build
        buildCache.add(dependencyNodeToArtifact(buildDependencyTree, false));

        // Add modules
        for (DependencyTree module : buildDependencyTree.getChildren()) {
            Set<String> moduleChildren = Sets.newHashSet();
            for (DependencyTree artifactsOrDependenciesNode : module.getChildren()) {

                // Add dependencies and artifacts
                boolean isArtifactsNode = ARTIFACTS_NODE.equals(artifactsOrDependenciesNode.getUserObject());
                for (DependencyTree child : artifactsOrDependenciesNode.getChildren()) {
                    moduleChildren.add(child.getGeneralInfo().getComponentId());
                    Enumeration<?> enumeration = child.breadthFirstEnumeration();
                    while (enumeration.hasMoreElements()) {
                        DependencyTree node = (DependencyTree) enumeration.nextElement();
                        buildCache.add(dependencyNodeToArtifact(node, isArtifactsNode));
                    }
                }
            }
            Artifact moduleArtifact = new Artifact(module.getGeneralInfo(), module.getIssues(), module.getLicenses(), module.getScopes(), moduleChildren);
            buildCache.add(moduleArtifact);
        }
        buildCache.write();
    }

    public DependencyTree loadDependencyTree(String buildName, String buildNumber, String timestamp) {
        SingleBuildCache singleBuildCache = getBuildCache(buildName, buildNumber, timestamp);
        if (singleBuildCache == null) {
            return null;
        }
        Artifact artifact = singleBuildCache.get(buildName + ":" + buildNumber);
        DependencyTree buildDependencyTree = new DependencyTree(artifact.getGeneralInfo().getComponentId().replace(":", "/"));
        GeneralInfo buildGeneralInfo = new BuildGeneralInfo()
                .started(Long.parseLong(timestamp))
                .status(singleBuildCache.getBuildStatus().toString())
                .vcs(singleBuildCache.getVcs())
                .componentId(buildName + ":" + buildNumber);
        buildDependencyTree.setGeneralInfo(buildGeneralInfo);
        buildDependencyTree.setScopes(Sets.newHashSet(new Scope()));

        // Populate modules
        for (String moduleId : artifact.getChildren()) {
            Artifact moduleComponents = singleBuildCache.get(moduleId);
            DependencyTree module = new DependencyTree(moduleId);
            module.setGeneralInfo(moduleComponents.getGeneralInfo());
            buildDependencyTree.add(module);

            // Populate artifacts node
            DependencyTree artifactsNode = createArtifactsNode(moduleId);
            module.add(artifactsNode);
            // Populate dependencies node
            DependencyTree dependenciesNode = createDependenciesNode(moduleId);
            module.add(dependenciesNode);

            for (String componentId : moduleComponents.getChildren()) {
                Artifact component = singleBuildCache.get(componentId);
                DependencyTree subTree = buildDependencyTree(component, singleBuildCache);
                if (component.getGeneralInfo().getArtifact()) {
                    artifactsNode.add(subTree);
                } else {
                    dependenciesNode.add(subTree);
                }
            }
        }
        return buildDependencyTree;
    }

    private SingleBuildCache getBuildCache(String buildName, String buildNumber, String timestamp) {
        try {
            return SingleBuildCache.getBuildCache(buildName, buildNumber, timestamp, buildsDir, logger);
        } catch (IOException exception) {
            logger.error(String.format("Failed reading cache file for build %s/%s, " +
                    "zapping the old cache and starting a new one", buildName, buildNumber));
        }
        return null;
    }

    private DependencyTree buildDependencyTree(Artifact artifact, SingleBuildCache singleBuildCache) {
        DependencyTree node = new DependencyTree(artifact.getGeneralInfo().getComponentId());
        node.setGeneralInfo(artifact.getGeneralInfo());
        node.setIssues(artifact.getIssues());
        node.setLicenses(artifact.getLicenses().isEmpty() ? newHashSet(new License()) : artifact.getLicenses());
        node.setScopes(artifact.getScopes().isEmpty() ? newHashSet(new Scope()) : artifact.getScopes());
        for (String child : artifact.getChildren()) {
            node.add(buildDependencyTree(singleBuildCache.get(child), singleBuildCache));
        }
        return node;
    }
}
