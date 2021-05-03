package com.jfrog.ide.common.gradle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

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
            JsonNode jsonNode = objectMapper.readTree(project);
            GeneralInfo generalInfo = createGeneralInfo(jsonNode).path(projectDir.toString());
            DependencyTree projectNode = createNode(generalInfo, jsonNode);
            projectNode.setGeneralInfo(generalInfo);
            populateDependencyTree(projectNode, jsonNode);
            rootNode.add(projectNode);
        }

        return rootNode;
    }

    private void populateDependencyTree(DependencyTree node, JsonNode jsonNode) {
        jsonNode.get("dependencies").elements().forEachRemaining(jsonChild -> {
            GeneralInfo generalInfo = createGeneralInfo(jsonChild);
            DependencyTree child = createNode(generalInfo, jsonChild);
            node.add(child);
            populateDependencyTree(child, jsonChild);
        });
    }

    private GeneralInfo createGeneralInfo(JsonNode gradleDependencies) {
        String groupId = gradleDependencies.get("groupId").asText();
        String artifactId = gradleDependencies.get("artifactId").asText();
        String version = gradleDependencies.get("version").asText();
        String scope = gradleDependencies.get("scope").asText();
        return new GeneralInfo()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .pkgType("gradle");
    }

    private DependencyTree createNode(GeneralInfo generalInfo, JsonNode jsonNode) {
        DependencyTree node = new DependencyTree(generalInfo.getGroupId() + ":" + generalInfo.getArtifactId() + ":" + generalInfo.getVersion());
        String scopeText = jsonNode.get("scope").asText();
        Scope scope = StringUtils.isBlank(scopeText) ? new Scope() : new Scope(jsonNode.get("scope").asText());
        node.setScopes(Sets.newHashSet(scope));
        node.setLicenses(Sets.newHashSet(new License()));
        return node;
    }
}
