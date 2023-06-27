package com.jfrog.ide.common.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.utils.Utils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.npm.NpmDriver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Build npm dependency tree before the Xray scan.
 *
 * @author yahavi
 */
public class NpmTreeBuilder {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final NpmDriver npmDriver;
    private final Path projectDir;
    private final String descriptorFilePath;

    public NpmTreeBuilder(Path projectDir, String descriptorFilePath, Map<String, String> env) {
        this.projectDir = projectDir;
        this.descriptorFilePath = descriptorFilePath;
        this.npmDriver = new NpmDriver(env);
    }

    /**
     * Build the npm project dependency tree.
     *
     * @param logger the logger.
     * @return full dependency tree without Xray scan results.
     * @throws IOException in case of I/O error.
     */
    public DepTree buildTree(Log logger) throws IOException {
        if (!npmDriver.isNpmInstalled()) {
            throw new IOException("Could not scan npm project dependencies, because npm CLI is not in the PATH.");
        }
        JsonNode prodResults = npmDriver.list(projectDir.toFile(), Lists.newArrayList("--prod", "--package-lock-only"));
        if (prodResults.get("problems") != null) {
            logger.warn("Errors occurred during building the Npm dependency tree. " +
                    "The dependency tree may be incomplete:\n" + prodResults.get("problems").toString());
        }
        JsonNode devResults = npmDriver.list(projectDir.toFile(), Lists.newArrayList("--dev", "--package-lock-only"));
        Map<String, DepTreeNode> nodes = new HashMap<>();
        String packageId = getPackageId(prodResults);
        addDepTreeNodes(nodes, prodResults, packageId, "prod");
        addDepTreeNodes(nodes, devResults, packageId, "dev");
        DepTree tree = new DepTree(packageId, nodes);
        tree.getRootNode().descriptorFilePath(descriptorFilePath);
        return tree;
    }

    private void addDepTreeNodes(Map<String, DepTreeNode> nodes, JsonNode jsonDep, String depId, String scope) {
        DepTreeNode depNode;
        if (nodes.containsKey(depId)) {
            depNode = nodes.get(depId);
        } else {
            depNode = new DepTreeNode();
            nodes.put(depId, depNode);
        }
        depNode.getScopes().add(scope);

        JsonNode dependenciesList = jsonDep.get("dependencies");
        if (dependenciesList == null) {
            return;
        }
        dependenciesList.fields().forEachRemaining(stringJsonNodeEntry -> {
            JsonNode subDep = stringJsonNodeEntry.getValue();
            JsonNode versionNode = subDep.get("version");
            if (versionNode != null) {
                String subDepId = Utils.createComponentId(stringJsonNodeEntry.getKey(), versionNode.asText());
                depNode.getChildren().add(subDepId);
                addDepTreeNodes(nodes, subDep, subDepId, scope);
            }
        });
    }

    /**
     * Get root package ID. Typically, "name:version".
     *
     * @param results results of 'npm ls' command.
     * @return root package ID.
     */
    private String getPackageId(JsonNode results) throws IOException {
        String packageName;
        String packageVersion = null;
        JsonNode packageNameNode = results.get("name");
        if (packageNameNode != null) {
            packageName = packageNameNode.asText();
            JsonNode packageVersionNode = results.get("version");
            if (packageVersionNode != null) {
                packageVersion = packageVersionNode.asText();
            }
        } else {
            JsonNode packageJson = objectMapper.readTree(projectDir.resolve("package.json").toFile());
            JsonNode nameNode = packageJson.get("name");
            if (nameNode != null) {
                packageName = nameNode.asText();
            } else if (projectDir.getFileName() != null) {
                packageName = projectDir.getFileName().toString();
            } else {
                return "N/A";
            }
            JsonNode versionNode = packageJson.get("version");
            if (versionNode != null) {
                packageVersion = versionNode.asText();
            }
        }

        if (packageVersion != null) {
            return Utils.createComponentId(packageName, packageVersion);
        }
        return packageName;
    }
}
