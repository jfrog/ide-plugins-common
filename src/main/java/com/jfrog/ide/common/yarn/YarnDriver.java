package com.jfrog.ide.common.yarn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
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
    private final Log log;

    public YarnDriver(Map<String, String> env) {
        this(env, new NullLog());
    }

    public YarnDriver(Map<String, String> env, Log log) {
        this.commandExecutor = new CommandExecutor("yarn", env);
        this.log = log;
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
     * Runs 'yarn list' command and returns the output as a JsonNode.
     * @param workingDirectory - The working directory to run the command from.
     * @param extraArgs - Extra arguments to pass to the command.
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
            String res = StringUtils.defaultIfBlank(commandRes.getRes(), "{}");
            JsonNode jsonResults = jsonReader.readTree(res);

            if (!commandRes.isOk()) {
                log.error("Errors occurred during Yarn list command. " +
                        "The dependency tree may be incomplete:\n" + commandRes.getErr());
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
     * Runs 'yarn why' command and returns the output as an array of JsonNodes.
     * @param workingDirectory - The working directory to run the command from.
     * @param componentName - The component name to run the command for.
     * @return the command output.
     */
    public JsonNode[] why(File workingDirectory, String componentName) throws IOException {
        String[] args = {"why", componentName, "--json", "--no-progress"};
        try {
            CommandResults commandRes = runCommand(workingDirectory, args);

            // Note that although the command may succeed (commandRes.isOk() == true), the result may still contain errors (such as no match found)
            String err = commandRes.getErr();
            if (!StringUtils.isBlank(err)) {
                log.error("Errors occurred during Yarn why command for dependency '" + componentName + "'. " +
                        "The dependency tree may be incomplete:\n" + err);
                return new JsonNode[0];
            }

            String res = commandRes.getRes();
            String[] splitResults = res.split("\n");
            JsonNode[] yarnWhyResults = new JsonNode[splitResults.length];
            for (int i = 0; i < splitResults.length; i++) {
                yarnWhyResults[i] = jsonReader.readTree(splitResults[i]);
            }

            return yarnWhyResults;

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