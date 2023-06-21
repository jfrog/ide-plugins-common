package com.jfrog.ide.common.yarn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.jfrog.ide.common.utils.Utils.createComponentId;

/**
 * Build yarn dependency tree before the Xray scan.
 *
 * @author tala
 */
@SuppressWarnings({"unused"})
public class YarnTreeBuilder {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final YarnDriver yarnDriver;
    private final Path projectDir;
    private final String descriptorFilePath;

    public YarnTreeBuilder(Path projectDir, String descriptorFilePath, Map<String, String> env) {
        this.projectDir = projectDir;
        this.descriptorFilePath = descriptorFilePath;
        this.yarnDriver = new YarnDriver(env);
    }

    /**
     * Build the yarn dependency tree.
     *
     * @param logger      - The logger.
     * @return full dependency tree without Xray scan results.
     * @throws IOException in case of I/O error.
     */
    public DepTree buildTree(Log logger) throws IOException {
        if (!yarnDriver.isYarnInstalled()) {
            throw new IOException("Could not scan Yarn project dependencies, because Yarn is not in the PATH.");
        }
        JsonNode listResults = yarnDriver.list(projectDir.toFile());
        if (listResults.get("problems") != null) {
            logger.warn("Errors occurred during building the yarn dependency tree. " +
                    "The dependency tree may be incomplete:\n" + listResults.get("problems").toString());
        }
        return buildYarnDependencyTree(listResults);
    }

    /**
     * Build yarn dependency tree.
     */
    private DepTree buildYarnDependencyTree(JsonNode listResults) throws IOException {
        // The results of the "yarn list" command don't include the project's root, so we get the root package ID from the project's package.json.
        String packageId = getPackageId();
        Map<String, DepTreeNode> nodes = new HashMap<>();
        DepTreeNode root = new DepTreeNode().descriptorFilePath(descriptorFilePath);
        nodes.put(packageId, root);
        // Parse "yarn list" results
        JsonNode dataNode = getJsonField(listResults, "data");
        JsonNode treesNode = getJsonField(dataNode, "trees");
        treesNode.elements().forEachRemaining(subDep -> addDepTreeNodes(nodes, subDep, root));
        return new DepTree(packageId, nodes);
    }

    private void addDepTreeNodes(Map<String, DepTreeNode> nodes, JsonNode jsonDep, DepTreeNode parent) {
        JsonNode nameNode = jsonDep.get("name");
        if (nameNode == null) {
            throw new RuntimeException("The parsing of the 'yarn list' command output failed: the field 'name' could not be found.");
        }
        String compId = convertPackageNameToCompId(nameNode.asText());
        parent.getChildren().add(compId);
        DepTreeNode depNode;
        if (nodes.containsKey(compId)) {
            depNode = nodes.get(compId);
        } else {
            depNode = new DepTreeNode();
            String customScope = StringUtils.substringBetween(compId, "@", "/");
            if (customScope != null) {
                depNode.getScopes().add(customScope);
            }
            nodes.put(compId, depNode);
        }

        JsonNode dependenciesList = jsonDep.get("children");
        if (dependenciesList == null) {
            return;
        }
        dependenciesList.elements().forEachRemaining(subDep -> addDepTreeNodes(nodes, subDep, depNode));
    }

    /**
     * Convert Yarn's package name (e.g. @scope/comp@1.0.0) to Xray's component ID (e.g. @scope/comp:1.0.0).
     *
     * @param packageName Yarn's package name
     * @return Xray's component ID
     */
    private String convertPackageNameToCompId(String packageName) {
        int lastIndexOfAt = packageName.lastIndexOf('@');
        return packageName.substring(0, lastIndexOfAt) + ':' + packageName.substring(lastIndexOfAt + 1);
    }

    /**
     * Get the root package ID. Typically, "name:version".
     * The package name and version are read from the project's package.json.
     *
     * @return root package name.
     */
    private String getPackageId() throws IOException {
        JsonNode packageJson = objectMapper.readTree(projectDir.resolve("package.json").toFile());
        if (packageJson == null) {
            throw new IOException("Could not scan Yarn project dependencies, because the package.json file is missing.");
        }
        String packageName;
        JsonNode nameNode = packageJson.get("name");
        if (nameNode != null) {
            packageName = nameNode.asText();
        } else if (projectDir.getFileName() != null) {
            packageName = projectDir.getFileName().getFileName().toString();
        } else {
            return "N/A";
        }

        JsonNode versionNode = packageJson.get("version");
        if (versionNode == null) {
            return packageName;
        }
        return createComponentId(packageName, versionNode.asText());
    }

    private JsonNode getJsonField(JsonNode jsonNode, String fieldName) throws IOException {
        JsonNode fieldNode = jsonNode.get(fieldName);
        if (fieldNode == null) {
            throw new IOException(String.format("The parsing of the 'yarn list' command output failed: the field '%s' could not be found.", fieldName));
        }
        return fieldNode;
    }
}
