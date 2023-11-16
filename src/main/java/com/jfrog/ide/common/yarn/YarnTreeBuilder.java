package com.jfrog.ide.common.yarn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.nodes.subentities.ImpactTree;
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
     * @param logger - The logger.
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
     * Extracts a single dependency path from a raw dependency Json string returned from 'Yarn why' command.
     *
     * @param projectRootId   - The name of the project to display in the root of the impact tree.
     * @param packageFullName - The vulnerable dependency in <NAME>:<VERSION> format.
     * @param rawDependency   - The raw dependency Json string returned from 'Yarn why' command.
     * @return The extracted dependency path as a list of dependencies starting from projectRootId till packageFullName.
     */
    private List<String> extractSinglePath(String projectRootId, String packageFullName, String rawDependency) {
        List<String> pathResult = new ArrayList<>();
        pathResult.add(projectRootId); // The root project is guaranteed to be the first element in the path

        rawDependency = StringUtils.lowerCase(rawDependency); // the word specified can be in upper or lower case
        if (StringUtils.contains(rawDependency, "specified in")) { // This is a direct dependency
            pathResult.add(packageFullName);
            return pathResult;
        }

        // Split the path by '#'
        String[] splitPath = StringUtils.split(StringUtils.substringBetween(rawDependency, "\""), "#");

        if (splitPath == null) {
            return null;
        }

        // packageFullName is guaranteed to be the last element in the path
        if (!StringUtils.equals(splitPath[splitPath.length - 1], (StringUtils.substringBefore(packageFullName, ":")))) {
            splitPath = Arrays.copyOf(splitPath, splitPath.length + 1);
        }
        splitPath[splitPath.length - 1] = packageFullName;
        pathResult.addAll(Arrays.asList(splitPath));
        return pathResult;
    }

    /**
     * Extracts multiple dependency paths from a list of raw dependency Json strings returned from 'Yarn why' command.
     *
     * @param projectRootId      - The name of the project to display in the root of the impact tree.
     * @param packageFullName    - The vulnerable dependency in <NAME>:<VERSION> format.
     * @param rawDependencyPaths - The raw dependency Json strings returned from 'Yarn why' command.
     * @return The extracted dependency paths as a list of dependencies starting from projectRootId till packageFullName.
     */
    List<List<String>> extractMultiplePaths(String projectRootId, String packageFullName, List<String> rawDependencyPaths) {
        List<List<String>> paths = new ArrayList<>();
        int limit = rawDependencyPaths.size() < ImpactTree.IMPACT_PATHS_LIMIT ? rawDependencyPaths.size() : 50;
        for (int i = 0; i < limit; i++) {
            List<String> path = extractSinglePath(projectRootId, packageFullName, rawDependencyPaths.get(i));
            if (path != null) {
                paths.add(path);
            }
        }
        return paths;
    }

    /**
     * Finds the dependency paths from the dependency to the root project, based on the supplied "yarn why" command output.
     * A dependency path may appear as a part of a text or in a list of items.
     * <p>
     * Example 1 (Text):
     * {"type":"info","data":"This module exists because \"jest-cli#istanbul-api#mkdirp\" depends on it."}
     * <p>
     * Example 2 (List):
     * {"type":"list","data":{"type":"reasons","items":["Specified in \"dependencies\"","Hoisted from \"jest-cli#node-notifier#minimist\"","Hoisted from \"jest-cli#sane#minimist\""]}}
     *
     * @param logger          - The logger.
     * @param projectRootId   - The name of the project to display in the root of the impact tree.
     * @param packageName     - The package name (without version).
     *                        Example: "minimist".
     * @param packageVersions - The package versions.
     * @return A map of package full name (<NAME>:<VERSION>) to a list of dependency paths.
     * @throws IOException in case of I/O error returned from the running "yarn why" command in the yarnDriver.
     */
    public Map<String, List<List<String>>> findDependencyImpactPaths(Log logger, String projectRootId, String packageName, Set<String> packageVersions) throws IOException {
        JsonNode[] yarnWhyItem = yarnDriver.why(projectDir.toFile(), packageName);
        if (yarnWhyItem[0].has("problems")) {
            logger.warn("Errors occurred during building the Yarn dependency tree. " +
                    "The dependency tree may be incomplete:\n" + yarnWhyItem[0].get("problems").toString());

        }

        // Parse "yarn why" results and generate the dependency paths
        String packageFullName = packageName;
        String yarnWhyVersion = "";
        Map<String, List<List<String>>> packageImpactPaths = new HashMap<>();
        for (JsonNode jsonNode : yarnWhyItem) {
            JsonNode typeNode = getJsonField(jsonNode, "type");
            JsonNode dataNode = getJsonField(jsonNode, "data");
            switch (typeNode.asText()) {
                case "info":
                    String dataNodeAsText = dataNode.asText();
                    if (dataNodeAsText.contains("Found \"")) { // This is an info node telling the package version
                        String yarnWhyPackage = StringUtils.substringBetween(dataNodeAsText, "Found \"", "\"");
                        yarnWhyVersion = StringUtils.substringAfterLast(yarnWhyPackage, "@");
                        packageFullName = packageName + ":" + yarnWhyVersion;
                    } else if (dataNodeAsText.contains("This module exists because") && packageVersions.contains(yarnWhyVersion)) {
                        // This is an info node containing a single dependency path of a relevant vulnerable package version.
                        packageImpactPaths.put(packageFullName, extractMultiplePaths(projectRootId, packageFullName, Collections.singletonList(dataNodeAsText)));
                    }
                    break;
                case "list":
                    if (packageVersions.contains(yarnWhyVersion)) {
                        // This is a list node containing a list of dependency paths of a relevant vulnerable package version.
                        JsonNode itemsNode = getJsonField(dataNode, "items");
                        List<String> items = new ArrayList<>();
                        itemsNode.elements().forEachRemaining(item -> items.add(item.asText()));
                        packageImpactPaths.put(packageFullName, extractMultiplePaths(projectRootId, packageFullName, items));
                    }
                    break;
            }
        }
        return packageImpactPaths;
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
