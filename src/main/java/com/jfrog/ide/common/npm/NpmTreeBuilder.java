package com.jfrog.ide.common.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.utils.Utils;
import org.jfrog.build.extractor.util.WslUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.npm.NpmDriver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build npm dependency tree before the Xray scan.
 *
 * @author yahavi
 */
public class NpmTreeBuilder {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectReader jsonReader = objectMapper.reader();
    private final NpmDriver npmDriver;
    private final CommandExecutor wslExecutor;
    private final boolean isWsl;
    private final Path projectDir;
    private final String descriptorFilePath;

    public NpmTreeBuilder(Path projectDir, String descriptorFilePath, Map<String, String> env) {
        this.projectDir = projectDir;
        this.descriptorFilePath = descriptorFilePath;
        this.isWsl = WslUtils.isWslPath(projectDir);
        if (isWsl) {
            this.npmDriver = null;
            this.wslExecutor = new CommandExecutor("wsl.exe", env);
        } else {
            this.npmDriver = new NpmDriver(env);
            this.wslExecutor = null;
        }
    }

    /**
     * Build the npm project dependency tree.
     *
     * @param logger the logger.
     * @return full dependency tree without Xray scan results.
     * @throws IOException in case of I/O error.
     */
    public DepTree buildTree(Log logger) throws IOException {
        if (!isNpmInstalled()) {
            throw new IOException("Could not scan npm project dependencies, because npm CLI is not in the PATH. [WSL=" + this.isWsl + "]");
        }
        JsonNode prodResults = npmList(Lists.newArrayList("--prod", "--package-lock-only"));
        if (prodResults.get("problems") != null) {
            logger.warn("Errors occurred during building the Npm dependency tree. " +
                    "The dependency tree may be incomplete:\n" + prodResults.get("problems").toString());
        }
        JsonNode devResults = npmList(Lists.newArrayList("--dev", "--package-lock-only"));
        Map<String, DepTreeNode> nodes = new HashMap<>();
        String packageId = getPackageId(prodResults);
        addDepTreeNodes(nodes, prodResults, packageId, "prod");
        addDepTreeNodes(nodes, devResults, packageId, "dev");
        DepTree tree = new DepTree(packageId, nodes);
        tree.getRootNode().descriptorFilePath(descriptorFilePath);
        return tree;
    }

    /**
     * Check whether npm is available, accounting for WSL projects.
     * For WSL projects, npm is invoked through {@code wsl.exe}.
     */
    private boolean isNpmInstalled() {
        if (isWsl) {
            try {
                List<String> args = wslNpmInvocationPrefix();
                args.add("--version");
                CommandResults results = wslExecutor.exeCommand(null, args, null, null);
                return results.isOk() && !StringUtils.isBlank(results.getRes());
            } catch (Exception e) {
                return false;
            }
        }
        return npmDriver.isNpmInstalled();
    }

    /**
     * Prefix arguments for {@code wsl.exe} so npm runs in the project directory inside WSL
     * (same {@code --cd} context as {@link #npmList}).
     */
    private List<String> wslNpmInvocationPrefix() {
        String linuxPath = WslUtils.toLinuxPath(projectDir.toString());
        List<String> args = new ArrayList<>();
        args.add("--cd");
        args.add(linuxPath);
        args.add("--exec");
        args.add("npm");
        return args;
    }

    /**
     * Run {@code npm ls} and return the parsed JSON output.
     * For WSL projects, the command is routed through {@code wsl.exe --cd <linux-path> --exec npm ...}.
     */
    private JsonNode npmList(List<String> extraArgs) throws IOException {
        if (isWsl) {
            List<String> args = new ArrayList<>(wslNpmInvocationPrefix());
            args.add("ls");
            args.add("--json");
            args.add("--all");
            args.addAll(extraArgs);
            try {
                CommandResults commandRes = wslExecutor.exeCommand(null, args, null, null);
                String res = StringUtils.isBlank(commandRes.getRes()) ? "{}" : commandRes.getRes();
                JsonNode results = jsonReader.readTree(res);
                if (!commandRes.isOk() && !results.has("problems") && results.isObject()) {
                    ((ObjectNode) results).put("problems", commandRes.getErr());
                }
                return results;
            } catch (IOException | InterruptedException e) {
                throw new IOException("npm ls failed via WSL", e);
            }
        }
        return npmDriver.list(projectDir.toFile(), extraArgs);
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
