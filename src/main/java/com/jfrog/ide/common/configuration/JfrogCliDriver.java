package com.jfrog.ide.common.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jfrog.ide.common.utils.Utils.createMapper;

/**
 * @author Tal Arian
 */
public class JfrogCliDriver {

    private static final ObjectMapper jsonReader = createMapper();
    private final CommandExecutor commandExecutor;

    public JfrogCliDriver(Map<String, String> env) {
        this(env, "");
    }

    public JfrogCliDriver(Map<String, String> env, String path) {
        String jfrogExec = "jf";
        if (SystemUtils.IS_OS_WINDOWS) {
            jfrogExec += ".exe";
        }
        this.commandExecutor = new CommandExecutor(Paths.get(path, jfrogExec).toString(), env);
    }

    @SuppressWarnings("unused")
    public boolean isJfrogCliInstalled() {
        try {
            version(null);
            return true;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public JfrogCliServerConfig getServerConfig() throws IOException {
        return getServerConfig(Paths.get(".").toAbsolutePath().normalize().toFile(), Collections.emptyList());
    }

    public JfrogCliServerConfig getServerConfig(File workingDirectory, List<String> extraArgs) throws IOException {
        List<String> args = new ArrayList<>();
        args.add("config");
        args.add("export");
        args.addAll(extraArgs);
        try {
            CommandResults commandResults = commandExecutor.exeCommand(workingDirectory, args, null, null);
            String res = commandResults.getRes();
            if (StringUtils.isBlank(res) || !commandResults.isOk()) {
                throw new IOException(commandResults.getErr());
            }
            // The output of the export command should be decoded before being parsed.
            byte[] decodedBytes = Base64.getDecoder().decode(res.trim());
            String decodedString = new String(decodedBytes);
            return new JfrogCliServerConfig(jsonReader.readTree(decodedString));
        } catch (IOException | InterruptedException e) {
            throw new IOException("jfrog config export command failed." +
                    "That might be happen if you haven't config any CLI server yet or using the config encryption feature.", e);
        }
    }

    public String version(File workingDirectory) throws IOException, InterruptedException {
        return runCommand(workingDirectory, new String[]{"--version"}, Collections.emptyList()).getRes();
    }

    private CommandResults runCommand(File workingDirectory, String[] args, List<String> extraArgs) throws IOException, InterruptedException {
        return runCommand(workingDirectory, args, extraArgs, null);
    }

    public CommandResults runCommand(File workingDirectory, String[] args, List<String> extraArgs, Log logger) throws IOException, InterruptedException {
        List<String> finalArgs = Stream.concat(Arrays.stream(args), extraArgs.stream()).collect(Collectors.toList());
        CommandResults commandResults = commandExecutor.exeCommand(workingDirectory, finalArgs, null, logger);
        if (!commandResults.isOk()) {
            throw new IOException(commandResults.getErr() + commandResults.getRes());
        }

        return commandResults;
    }
}