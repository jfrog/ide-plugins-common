package com.jfrog.ide.common.gradle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Build npm dependency tree before the Xray scan.
 *
 * @author yahavi
 */
@SuppressWarnings({"unused"})
public class GradleTreeBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final GradleDriver gradleDriver;
    private final Path projectDir;

    public GradleTreeBuilder(Path projectDir, Map<String, String> env) {
        this.projectDir = projectDir;
        this.gradleDriver = new GradleDriver(projectDir, env);
    }

    /**
     * Build the npm dependency tree.
     *
     * @param logger - The logger.
     * @return full dependency tree without Xray scan results.
     * @throws IOException in case of I/O error.
     */
    public DependencyTree buildTree(Log logger) throws IOException {
        if (!gradleDriver.isGradleInstalled()) {
            logger.error("Could not scan Gradle project dependencies, because Gradle CLI is not in the PATH.");
            return null;
        }
        File[] projects = gradleDriver.generateDependenciesGraphAsJson(projectDir.toFile(), logger);
        DependencyTree rootNode = new DependencyTree(projectDir.getFileName().toString());
        rootNode.setGeneralInfo(new GeneralInfo().componentId(projectDir.getFileName().toString()).path(projectDir.toString()));
        for (File project : projects) {
            GradleDependencyNode node = objectMapper.readValue(project, GradleDependencyNode.class);
            GeneralInfo generalInfo = createGeneralInfo(node).path(projectDir.toString());
            DependencyTree projectNode = createNode(generalInfo, node);
            projectNode.setGeneralInfo(generalInfo);
            populateDependencyTree(projectNode, node);
            rootNode.add(projectNode);
        }

        return rootNode;
    }

    private void populateDependencyTree(DependencyTree node, GradleDependencyNode gradleDependencyNode) {
        for (GradleDependencyNode gradleDependencyChild : CollectionUtils.emptyIfNull(gradleDependencyNode.getDependencies())) {
            GeneralInfo generalInfo = createGeneralInfo(gradleDependencyChild);
            DependencyTree child = createNode(generalInfo, gradleDependencyChild);
            node.add(child);
            populateDependencyTree(child, gradleDependencyChild);
        }
    }

    private GeneralInfo createGeneralInfo(GradleDependencyNode node) {
        return new GeneralInfo()
                .groupId(node.getGroupId())
                .artifactId(node.getArtifactId())
                .version(node.getVersion())
                .pkgType("gradle");
    }

    private DependencyTree createNode(GeneralInfo generalInfo, GradleDependencyNode gradleDependencyNode) {
        DependencyTree node = new DependencyTree(generalInfo.getGroupId() + ":" + generalInfo.getArtifactId() + ":" + generalInfo.getVersion());
        Set<Scope> scopes = CollectionUtils.emptyIfNull(gradleDependencyNode.getScopes()).stream().map(Scope::new).collect(Collectors.toSet());
        if (scopes.isEmpty()) {
            scopes.add(new Scope());
        }
        node.setScopes(scopes);
        node.setLicenses(Sets.newHashSet(new License()));
        return node;
    }
}
