package com.jfrog.ide.common.gradle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.GradleDepTreeResults;
import com.jfrog.GradleDependencyNode;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build Gradle dependency tree before the Xray scan.
 *
 * @author yahavi
 */
@SuppressWarnings({"unused"})
public class GradleTreeBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final GradleDriver gradleDriver;
    private final Path projectDir;
    private final String descriptorFilePath;
    private Path pluginLibDir;

    public GradleTreeBuilder(Path projectDir, String descriptorFilePath, Map<String, String> env, String gradleExe) {
        this.projectDir = projectDir;
        this.descriptorFilePath = descriptorFilePath;
        this.gradleDriver = new GradleDriver(gradleExe, env);
    }

    /**
     * Build the Gradle dependency tree.
     *
     * @param logger - The logger.
     * @return full dependency tree without Xray scan results.
     * @throws IOException in case of I/O error.
     */
    public DepTree buildTree(Log logger) throws IOException {
        gradleDriver.verifyGradleInstalled();
        List<File> gradleDependenciesFiles = gradleDriver.generateDependenciesGraphAsJson(projectDir.toFile(), logger);
        return createDependencyTrees(gradleDependenciesFiles);
    }

    /**
     * Create dependency trees from files generated by running the 'generateDependenciesGraphAsJson' task.
     *
     * @param gradleDependenciesFiles - The files containing the dependency trees
     * @return a dependency tree contain one or more Gradle projects.
     * @throws IOException in case of any I/O error.
     */
    private DepTree createDependencyTrees(List<File> gradleDependenciesFiles) throws IOException {
        String rootId = projectDir.getFileName().toString();
        DepTreeNode rootNode = new DepTreeNode().descriptorFilePath(descriptorFilePath);

        Map<String, DepTreeNode> nodes = new HashMap<>();
        for (File moduleDepsFile : gradleDependenciesFiles) {
            GradleDepTreeResults results = objectMapper.readValue(moduleDepsFile, GradleDepTreeResults.class);
            for (Map.Entry<String, GradleDependencyNode> nodeEntry : results.getNodes().entrySet()) {
                String compId = nodeEntry.getKey();
                GradleDependencyNode gradleDep = nodeEntry.getValue();
                DepTreeNode node = new DepTreeNode().scopes(gradleDep.getConfigurations()).children(gradleDep.getChildren());
                nodes.put(compId, node);
            }
            String moduleRootId = results.getRoot();
            nodes.get(moduleRootId).descriptorFilePath(descriptorFilePath);
            rootNode.getChildren().add(moduleRootId);
        }
        if (rootNode.getChildren().size() == 1) {
            return new DepTree(rootNode.getChildren().iterator().next(), nodes);
        }
        nodes.put(rootId, rootNode);
        return new DepTree(rootId, nodes);
    }

    private GeneralInfo createGeneralInfo(String id, GradleDependencyNode node) {
        return new GeneralInfo().pkgType("gradle").componentId(id);
    }
}
