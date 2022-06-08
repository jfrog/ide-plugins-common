package com.jfrog.ide.common.yarn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.npm.types.NpmPackageInfo;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    public YarnTreeBuilder(Path projectDir, Map<String, String> env) {
        this.projectDir = projectDir;
        this.yarnDriver = new YarnDriver(env);
    }

    /**
     * Build the yarn dependency tree.
     *
     * @param logger      - The logger.
     * @param shouldToast - True if should popup a balloon in case of errors.
     * @return full dependency tree without Xray scan results.
     * @throws IOException in case of I/O error.
     */
    public DependencyTree buildTree(Log logger, boolean shouldToast) throws IOException {
        if (!yarnDriver.isYarnInstalled()) {
            throw new IOException("Could not scan yarn project dependencies, because yarn CLI is not in the PATH.");
        }
        JsonNode listResults = yarnDriver.list(projectDir.toFile());
        JsonNode packageJson = objectMapper.readTree(projectDir.resolve("package.json").toFile());
        if (packageJson == null) {
            throw new IOException("Could not scan yarn project dependencies, because package.json file is missing.");
        }
        JsonNode nameNode = packageJson.get("name");
        String packageName = getPackageName(logger, packageJson, listResults, shouldToast);
        JsonNode versionNode = packageJson.get("version");
        String packageVersion = versionNode != null ? versionNode.asText() : "N/A";

        DependencyTree rootNode = buildYarnDependencyTree(listResults, packageName);
        rootNode.setUserObject(packageName);
        rootNode.setGeneralInfo(createGeneralInfo(packageName, packageVersion));
        return rootNode;
    }

    /**
     * Build yarn dependency tree.
     */
    private DependencyTree buildYarnDependencyTree(JsonNode listResults, String projectName) {
        // Parse "yarn list" results
        DependencyTree rootNode = new DependencyTree();
        JsonNode dataNode = listResults.get("data");
        if (dataNode == null) {
            return rootNode;
        }
        populateDependenciesTree(rootNode, dataNode.get("trees"), new String[]{projectName});
        for (DependencyTree child : rootNode.getChildren()) {
            NpmPackageInfo packageInfo = (NpmPackageInfo) child.getUserObject();
            child.setScopes(getScopes(packageInfo.getName()));
        }
        rootNode.setMetadata(true);
        return rootNode;
    }

    private static Set<Scope> getScopes(String name) {
        Set<Scope> scopes = new HashSet<>();
        String customScope = StringUtils.substringBetween(name, "@", "/");
        if (customScope != null) {
            scopes.add(new Scope(customScope));
        }
        return scopes;
    }

    private static void populateDependenciesTree(DependencyTree scanTreeNode, JsonNode dependencies, String[] pathToRoot) {
        if (dependencies == null || pathToRoot == null) {
            return;
        }

        dependencies.elements().forEachRemaining(dependency -> {
            String nameString = dependency.get("name").textValue();
            int lastIndexOfAt = nameString.lastIndexOf('@');
            String name = nameString.substring(0, lastIndexOfAt);
            String version = nameString.substring(lastIndexOfAt + 1);
            if (!version.isBlank() && (dependency.get("shadow") == null || !dependency.get("shadow").booleanValue())) {
                addSubtree(dependency, scanTreeNode, name, version, pathToRoot); // Mutual recursive call
            }
        });
    }

    private static void addSubtree(JsonNode dependency, DependencyTree node, String name, String version, String[] pathToRoot) {
        NpmPackageInfo npmPackageInfo = new NpmPackageInfo(name, version, "", pathToRoot);
        JsonNode childDependencies = dependency.get("children");
        DependencyTree childTreeNode = new DependencyTree(npmPackageInfo);
        populateDependenciesTree(childTreeNode, childDependencies, ArrayUtils.insert(0, pathToRoot, npmPackageInfo.toString())); // Mutual recursive call
        node.add(childTreeNode);
    }

    /**
     * Get root package name. Typically, "name:version".
     *
     * @param logger       - The logger.
     * @param packageJson  - The package.json.
     * @param npmLsResults - Npm ls results.
     * @param shouldToast  - True if should popup a balloon in case of errors.
     * @return root package name.
     */
    private String getPackageName(Log logger, JsonNode packageJson, JsonNode npmLsResults, boolean shouldToast) {
        JsonNode nameNode = packageJson.get("name");
        if (nameNode != null) {
            return nameNode.asText() + getPostfix(logger, npmLsResults, shouldToast);
        }
        if (projectDir.getFileName() != null) {
            return projectDir.getFileName().getFileName().toString() + getPostfix(logger, npmLsResults, shouldToast);
        }
        return "N/A";
    }

    /**
     * Append "(Not installed)" postfix if needed.
     *
     * @param logger       - The logger.
     * @param npmLsResults - Npm ls results.
     * @param shouldToast  - True if should popup a balloon in case of errors.
     * @return (Not installed) or empty.
     */
    private String getPostfix(Log logger, JsonNode npmLsResults, boolean shouldToast) {
        String postfix = "";
        if (npmLsResults.get("problems") != null) {
            postfix += " (Not installed)";
            logger.warn("Errors occurred during building the yarn dependency tree. " +
                    "The dependency tree may be incomplete:\n" + npmLsResults.get("problems").toString());
        }
        return postfix;
    }

    private GeneralInfo createGeneralInfo(String packageName, String packageVersion) {
        return new GeneralInfo().path(projectDir.toString()).componentId(createComponentId(packageName, packageVersion)).pkgType("yarn");
    }
}
