package org.jfrog.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.jfrog.build.extractor.npm.extractor.NpmDependencyTree;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Build npm dependencies tree before the Xray scan.
 *
 * @author yahavi
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NpmTreeBuilder {

    private static NpmDriver npmDriver = new NpmDriver("", null);
    private static ObjectMapper objectMapper = new ObjectMapper();
    private Path projectDir;

    public NpmTreeBuilder(Path projectDir) {
        this.projectDir = projectDir;
    }

    /**
     * Build the npm dependencies tree.
     *
     * @param logger - The logger.
     * @return full dependencies tree without Xray scan results.
     * @throws IOException in case of I/O error.
     */
    public DependenciesTree buildTree(Log logger) throws IOException {
        JsonNode npmLsResults = npmDriver.list(projectDir.toFile(), Lists.newArrayList());
        DependenciesTree rootNode = NpmDependencyTree.createDependenciesTree(null, npmLsResults);
        JsonNode packageJson = objectMapper.readTree(projectDir.resolve("package.json").toFile());
        JsonNode nameNode = packageJson.get("name");
        String packageName = getPackageName(logger, packageJson, npmLsResults);
        JsonNode versionNode = packageJson.get("version");
        String packageVersion = versionNode != null ? versionNode.asText() : "N/A";
        rootNode.setUserObject(packageName);
        rootNode.setGeneralInfo(createGeneralInfo(packageName, packageVersion));
        return rootNode;
    }

    /**
     * Get root package name. Typically "name:version".
     *
     * @param logger       - The logger.
     * @param packageJson  - The package.json.
     * @param npmLsResults - Npm ls results.
     * @return root package name.
     */
    private String getPackageName(Log logger, JsonNode packageJson, JsonNode npmLsResults) {
        JsonNode nameNode = packageJson.get("name");
        if (nameNode != null) {
            return nameNode.asText() + getPostfix(logger, nameNode, npmLsResults);
        }
        if (projectDir.getFileName() != null) {
            return projectDir.getFileName().getFileName().toString() + getPostfix(logger, null, npmLsResults);
        }
        return "N/A";
    }

    /**
     * Append "(Not installed)" postfix if needed.
     *
     * @param logger       - The logger.
     * @param nameNode     - The "name" in the package.json file.
     * @param npmLsResults - Npm ls results.
     * @return (Not installed) or empty.
     */
    private String getPostfix(Log logger, JsonNode nameNode, JsonNode npmLsResults) {
        String postfix = "";
        if (nameNode == null) {
            postfix += " (Not installed)";
            logger.error("JFrog Xray - Failed while running npm ls command at " + projectDir.toString());
        } else if (npmLsResults.get("problems") != null) {
            postfix += " (Not installed)";
            logger.error("JFrog Xray - npm ls command at " + projectDir.toString() + " result had errors:" + "\n" + npmLsResults.get("problems").toString());
        }
        return postfix;
    }

    private GeneralInfo createGeneralInfo(String packageName, String packageVersion) {
        return new GeneralInfo()
                .componentId(packageName + ":" + packageVersion)
                .pkgType("npm")
                .path(projectDir.toString())
                .artifactId(packageName)
                .version(packageVersion);
    }
}
