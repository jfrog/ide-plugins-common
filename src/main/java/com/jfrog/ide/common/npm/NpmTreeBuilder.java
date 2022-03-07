package com.jfrog.ide.common.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.jfrog.build.extractor.npm.extractor.NpmDependencyTree;
import org.jfrog.build.extractor.npm.types.NpmScope;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;

import static com.jfrog.ide.common.utils.Utils.createComponentId;

/**
 * Build npm dependency tree before the Xray scan.
 *
 * @author yahavi
 */
@SuppressWarnings({"unused"})
public class NpmTreeBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final NpmDriver npmDriver;
    private final Path projectDir;

    public NpmTreeBuilder(Path projectDir, Map<String, String> env) {
        this.projectDir = projectDir;
        this.npmDriver = new NpmDriver(env);
    }

    /**
     * Build the npm dependency tree.
     *
     * @param logger      - The logger.
     * @param shouldToast - True if should popup a balloon in case of errors.
     * @return full dependency tree without Xray scan results.
     * @throws IOException in case of I/O error.
     */
    public DependencyTree buildTree(Log logger, boolean shouldToast) throws IOException {
        if (!npmDriver.isNpmInstalled()) {
            throw new IOException("Could not scan npm project dependencies, because npm CLI is not in the PATH.");
        }
        JsonNode npmLsResults = npmDriver.list(projectDir.toFile(), Lists.newArrayList("--prod", "--package-lock-only"));
        DependencyTree rootNode = buildUnifiedDependencyTree(npmLsResults);
        JsonNode packageJson = objectMapper.readTree(projectDir.resolve("package.json").toFile());
        JsonNode nameNode = packageJson.get("name");
        String packageName = getPackageName(logger, packageJson, npmLsResults, shouldToast);
        JsonNode versionNode = packageJson.get("version");
        String packageVersion = versionNode != null ? versionNode.asText() : "N/A";
        rootNode.setUserObject(packageName);
        rootNode.setGeneralInfo(createGeneralInfo(packageName, packageVersion));
        return rootNode;
    }

    /**
     * Build dependency tree from development and production scopes.
     * If a dependency appears when running "npm ls --prod", it will have a production scope.
     * If a dependency appears when running "npm ls --dev", it will have a development scope.
     * If a dependency appears in both scenarios, this method will add both production and development scopes to it.
     *
     * @throws IOException - In case of failure in running "npm ls"
     */
    private DependencyTree buildUnifiedDependencyTree(JsonNode npmLsResults) throws IOException {
        // Parse "npm ls" results on the production scope
        DependencyTree rootNode = NpmDependencyTree.createDependencyTree(npmLsResults, NpmScope.PRODUCTION, projectDir);
        rootNode.setMetadata(true);

        // Run "npm ls" on the development scope
        npmLsResults = npmDriver.list(projectDir.toFile(), Lists.newArrayList("--dev", "--package-lock-only"));
        DependencyTree devRootNode = NpmDependencyTree.createDependencyTree(npmLsResults, NpmScope.DEVELOPMENT, projectDir);

        // Merge trees. We'll convert to ArrayList to avoid ConcurrentModificationException on the vector.
        Lists.newArrayList(devRootNode.getChildren()).forEach(devChild -> mergeDevNode(devChild, rootNode));

        return rootNode;
    }

    /**
     * Merge the child node of the dev dependency tree to the prod dependency tree.
     *
     * @param devChild - Direct dependency of the dev dependency tree
     * @param rootNode - Root node of the prod dependency tree
     */
    private void mergeDevNode(DependencyTree devChild, DependencyTree rootNode) {
        // duplicatedProdChild - a direct dependency on the prod tree, that appear also on the dev tree as a direct dependency.
        DependencyTree duplicatedProdChild = rootNode.getChildren().stream()
                .filter(child -> StringUtils.equals(child.toString(), devChild.toString()))
                .findAny().orElse(null);
        if (duplicatedProdChild != null) {
            // Add 'development' scope to all of the prod node's child
            Enumeration<?> enumeration = duplicatedProdChild.breadthFirstEnumeration();
            while (enumeration.hasMoreElements()) {
                DependencyTree child = (DependencyTree) enumeration.nextElement();
                child.getScopes().add(new Scope(NpmScope.DEVELOPMENT.toString()));
            }
        } else {
            rootNode.add(devChild);
        }
    }

    /**
     * Get root package name. Typically "name:version".
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
            logger.warn("Errors occurred during building the Npm dependency tree. " +
                    "The dependency tree may be incomplete:\n" + npmLsResults.get("problems").toString());
        }
        return postfix;
    }

    private GeneralInfo createGeneralInfo(String packageName, String packageVersion) {
        return new GeneralInfo().path(projectDir.toString()).componentId(createComponentId(packageName, packageVersion)).pkgType("npm");
    }
}
