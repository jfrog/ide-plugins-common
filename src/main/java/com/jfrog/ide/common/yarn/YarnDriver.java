package com.jfrog.ide.common.yarn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Tal Arian
 */
public class YarnDriver {
    private static final ObjectReader jsonReader = new ObjectMapper().reader();
    private final CommandExecutor commandExecutor;

    public YarnDriver(Map<String, String> env) {
        this.commandExecutor = new CommandExecutor("yarn", env);
    }

    @SuppressWarnings("unused")
    public boolean isYarnInstalled() {
        try {
            return !version(null).isEmpty();
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Runs 'yarn list' command.
     *
     * @return the command output.
     */
    public JsonNode list(File workingDirectory, List<String> extraArgs) throws IOException {
        List<String> args = new ArrayList<>();
        args.add("list");
        args.add("--json");
        args.add("--no-progress");
        args.addAll(extraArgs);
        try {
            CommandResults commandRes = runCommand(workingDirectory, args.toArray(new String[0]));
            String res = StringUtils.isBlank(commandRes.getRes()) ? "{}" : commandRes.getRes();
            JsonNode jsonResults = jsonReader.readTree(res);

            if (!commandRes.isOk() && !jsonResults.has("problems")) {
                ((ObjectNode) jsonResults).put("problems", commandRes.getErr());
            }
            return jsonResults;
        } catch (IOException | InterruptedException e) {
            throw new IOException("yarn list failed", e);
        }
    }

    public JsonNode list(File workingDirectory) throws IOException {
        return list(workingDirectory, Collections.emptyList());
    }

    public String version(File workingDirectory) throws IOException, InterruptedException {
        return runCommand(workingDirectory, new String[]{"--version"}).getRes();
    }

    /**
     * Runs 'yarn why' command.
     *
     * @return the command output.
     */
    public JsonNode[] why(File workingDirectory, String componentName) throws IOException {
        List<String> args = new ArrayList<>();
        args.add("why");
        args.add(componentName);
        args.add("--json");
        args.add("--no-progress");
        try {
            CommandResults commandRes = runCommand(workingDirectory, args.toArray(new String[0]));
            String res = StringUtils.isBlank(commandRes.getRes()) ? "{}" : commandRes.getRes();
            String[] jsons = res.split("\n");
            JsonNode[] jsonResults = new JsonNode[jsons.length];
            for (int i = 0; i < jsons.length; i++) {
                jsonResults[i] = jsonReader.readTree(jsons[i]);
            }
//          note that although the command may succeed (commandRes.isOk() == true), the result may still contain errors (such as no match found)
            String err = commandRes.getErr();
            if (!StringUtils.isBlank(err)) {
                ((ObjectNode) jsonResults[0]).put("problems", commandRes.getErr());
            }

            return jsonResults;

        } catch (IOException | InterruptedException e) {
            throw new IOException("yarn why failed", e);
        }
    }

    /**
     * Yarn upgrade command (supported by Yarn1 only)
     *
     * @param workingDirectory  - Working directory
     * @param componentFullName - Component full name
     */
    public void upgrade(File workingDirectory, String componentFullName) throws IOException {
        try {
            runCommand(workingDirectory, new String[]{"upgrade", componentFullName});
        } catch (IOException | InterruptedException e) {
            throw new IOException("yarn upgrade command failed", e);
        }
    }

    private CommandResults runCommand(File workingDirectory, String[] args) throws IOException, InterruptedException {
        return runCommand(workingDirectory, args, Collections.emptyList());
    }

    private CommandResults runCommand(File workingDirectory, String[] args, List<String> extraArgs) throws IOException, InterruptedException {
        List<String> finalArgs = Stream.concat(Arrays.stream(args), extraArgs.stream()).collect(Collectors.toList());
        CommandResults commandRes = commandExecutor.exeCommand(workingDirectory, finalArgs, null, null);
        if (!commandRes.isOk()) {
            throw new IOException(commandRes.getErr() + commandRes.getRes());
        }
        return commandRes;
    }
}