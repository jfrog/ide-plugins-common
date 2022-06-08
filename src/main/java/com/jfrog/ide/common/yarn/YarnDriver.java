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
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Tal Arian
 */
public class YarnDriver implements Serializable {
    private static final long serialVersionUID = 1L;

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