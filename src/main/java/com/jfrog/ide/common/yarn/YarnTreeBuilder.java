package com.jfrog.ide.common.yarn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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
     * Extracts a single dependency path from a raw dependency string.
     *
     * @param rawDependencyPath - The raw dependency path string.
     * @return The extracted dependency path.
     */

    private List<List<String>> extractMultiplePaths(String packageFullName, List<String> rawDependencyPath) {
        List<List<String>> paths = new ArrayList<>();
        for (String rawDependency : rawDependencyPath) {
            List<String> path = extractSinglePath(packageFullName, rawDependency);
            if (path != null) {
                paths.add(path);
            }
        }
        return paths;
    }

    private List<String> extractSinglePath(String packageFullName, String rawDependency) {
        if (StringUtils.contains(rawDependency, "Specified in")) {
            // return the package name
            return Collections.singletonList(packageFullName);
        }
        int startIndex = StringUtils.indexOf(rawDependency, '"');
        int endIndex = StringUtils.indexOf(rawDependency, '"', startIndex + 1);

        if (startIndex != -1 && endIndex != -1) {
            // split the path by #
            String[] splitPath = StringUtils.split(StringUtils.substring(rawDependency, startIndex + 1, endIndex), "#");

            // packageFullName is guaranteed to be the last element in the path
            if (!StringUtils.equals(splitPath[splitPath.length - 1], (StringUtils.substringBefore(packageFullName, ":")))) {
                splitPath = Arrays.copyOf(splitPath, splitPath.length + 1);
            }
            splitPath[splitPath.length - 1] = packageFullName;
            return Arrays.asList(splitPath);
        }
        return null;
    }

    /**
     * Finds the dependency path from the dependency to the root, based on the supplied "yarn why" command output.
     * The dependency path may appear as a part of a text or in a list of reasons.
     * <p>
     * Example 1 (Text):
     * {"type":"info","data":"This module exists because \"jest-cli#istanbul-api#mkdirp\" depends on it."}
     * <p>
     * Example 2 (List):
     * {"type":"list","data":{"type":"reasons","items":["Specified in \"dependencies\"","Hoisted from \"jest-cli#node-notifier#minimist\"","Hoisted from \"jest-cli#sane#minimist\""]}}
     *
     * @param logger          - The logger.
     * @param packageName     - The package name.
     *                        Example: "minimist".
     * @param packageVersions - The package versions.
     * @return A list of vulnerable dependency chains to the root.
     */
    public DepTree findDependencyPath(Log logger, String packageName, Set<String> packageVersions) throws IOException {
        JsonNode[] yarnWhyItem = yarnDriver.why(projectDir.toFile(), packageName);
        if (yarnWhyItem[0].has("problems")) {
            logger.warn("Errors occurred during building the yarn dependency tree. " +
                    "The dependency tree may be incomplete:\n" + yarnWhyItem[0].get("problems").toString());
        }

        // Parse "yarn why" results and generate the dependency paths
        String yarnWhyVersion = "";
        String packageFullName = packageName;
        Map<String, List<List<String>>> packageImpactPaths = new HashMap<>();
        for (JsonNode jsonNode : yarnWhyItem) {
            JsonNode typeNode = getJsonField(jsonNode, "type");
            JsonNode dataNode = getJsonField(jsonNode, "data");
            switch (typeNode.asText()) {
                case "info":
                    String dataNodeAsText = dataNode.asText();
                    if (dataNodeAsText.contains("Found \"")) {
                        String yarnWhyPackage = StringUtils.substringBetween(dataNodeAsText, "Found \"", "\"");
                        yarnWhyVersion = StringUtils.substringAfter(yarnWhyPackage, "@");
                        packageFullName = packageName + ":" + yarnWhyVersion;
                    } else if (dataNodeAsText.contains("This module exists because") && packageVersions.contains(yarnWhyVersion)) {
                        packageImpactPaths.put(packageFullName, extractMultiplePaths(packageFullName, Collections.singletonList(dataNodeAsText)));
                    }
                    break;
                case "list":
                    if (packageVersions.contains(yarnWhyVersion)) {
                        JsonNode itemsNode = getJsonField(dataNode, "items");
                        List<String> items = new ArrayList<>();
                        itemsNode.elements().forEachRemaining(item -> items.add(item.asText()));
                        packageImpactPaths.put(packageFullName, extractMultiplePaths(packageFullName, items));
                    }
                    break;
            }
        }
        System.out.println(packageImpactPaths);
        return new DepTree("123", new HashMap<>());
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
            packageName = projectDir.getFileName().toString();
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
            throw new IOException(String.format("The parsing of a yarn command output failed: the field '%s' could not be found.", fieldName));
        }
        return fieldNode;
    }
}
