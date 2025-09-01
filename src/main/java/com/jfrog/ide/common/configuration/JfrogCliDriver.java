package com.jfrog.ide.common.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jfrog.ide.common.utils.Utils.*;

/**
 * @author Tal Arian
 */
public class JfrogCliDriver {
    private static final String JFROG_CLI_RELEASES_URL = "https://releases.jfrog.io/artifactory";
    private static final ObjectMapper jsonReader = createMapper();
    private final Log log;
    private final String path;
    private final Map<String, String> env;
    @Getter
    private String jfrogExec = "jf";

    @SuppressWarnings("unused")
    public JfrogCliDriver(Map<String, String> env, Log log) {
        this(env, "", log);
    }

    public JfrogCliDriver(Map<String, String> env, String path, Log log) {
        if (SystemUtils.IS_OS_WINDOWS) {
            this.jfrogExec += ".exe";
        }
        addDefaultEnvVars(env);
        this.env = env;
        this.path = path;
        this.log = log;
    }

    @SuppressWarnings("unused")
    public boolean isJfrogCliInstalled() {
        return runVersion(null) != null;
    }

    @SuppressWarnings("unused")
    public JfrogCliServerConfig getServerConfig() throws IOException {
        return getServerConfig(Paths.get(".").toAbsolutePath().normalize().toFile(), Collections.emptyList(), env);
    }

    public JfrogCliServerConfig getServerConfig(File workingDirectory, List<String> extraArgs, Map<String, String> envVars) throws IOException {
        List<String> args = new ArrayList<>();
        args.add("config");
        args.add("export");
        args.addAll(extraArgs);
        try {
            CommandResults commandResults = runCommand(workingDirectory, envVars, args.toArray(new String[0]), Collections.emptyList(),null, log);
            String res = commandResults.getRes();
            if (StringUtils.isBlank(res) || !commandResults.isOk()) {
                throw new IOException(commandResults.getErr());
            }
            // The output of the export command should be decoded before being parsed.
            byte[] decodedBytes = Base64.getDecoder().decode(res.trim());
            String decodedString = new String(decodedBytes);
            return new JfrogCliServerConfig(jsonReader.readTree(decodedString));
        } catch (IOException | InterruptedException e) {
            throw new IOException("'jfrog config export' command failed. " +
                    "That might be happen if you haven't config any CLI server yet or using the config encryption feature.", e);
        }
    }

    public String runVersion(File workingDirectory) {
        String versionOutput = null;
        try {
            versionOutput = runCommand(workingDirectory, env, new String[]{"--version"}, Collections.emptyList()).getRes();
        } catch (IOException | InterruptedException e) {
            log.error("Failed to get CLI version. Reason: " + e.getMessage());
        }

        return versionOutput;
    }

    private CommandResults runCommand(File workingDirectory, Map<String, String> envVars, String[] args, List<String> extraArgs) throws IOException,
            InterruptedException {
        return runCommand(workingDirectory, envVars, args, extraArgs,null, null);
    }

    public CommandResults runCommand(File workingDirectory, Map<String, String> commandEnvVars, String[] args, List<String> extraArgs,List<String> credentials, Log logger)
            throws IOException, InterruptedException {
        List<String> finalArgs = Stream.concat(Arrays.stream(args), extraArgs.stream()).collect(Collectors.toList());
        Map<String, String> combinedEnvVars = new HashMap<>();
        Optional.ofNullable(env).ifPresent(combinedEnvVars::putAll);
        Optional.ofNullable(commandEnvVars).ifPresent(combinedEnvVars::putAll);
        CommandExecutor commandExecutor = new CommandExecutor(Paths.get(path, this.jfrogExec).toString(), combinedEnvVars);
        CommandResults commandResults = commandExecutor.exeCommand(workingDirectory, finalArgs, credentials, logger);
        if (!commandResults.isOk()) {
            throw new IOException(commandResults.getErr() + commandResults.getRes());
        }

        return commandResults;
    }

    public void downloadCliIfNeeded(String destinationPath, String jfrogCliVersion) throws IOException {
        // verify installed cli version
        if (Files.exists(Paths.get(path, jfrogExec))){
            String cliVersion = extractVersionFromCliOutput(runVersion(new File(path)));
            log.debug("Local CLI version is: " + cliVersion);
            if (jfrogCliVersion.equals(cliVersion)) {
                log.info("Local Jfrog CLI version has been verified and is compatible. Proceeding with its usage.");
            } else {
                log.info(String.format("JFrog CLI version %s is installed, but the required version is %s. " +
                        "Initiating download of version %s to the destination: %s.", cliVersion, jfrogCliVersion, jfrogCliVersion, destinationPath));
                downloadCliFromReleases(jfrogCliVersion, destinationPath);
            }
        } else {
            log.info(String.format("JFrog CLI is not installed. Initiating download of version %s to the destination: %s.", jfrogCliVersion, destinationPath));
            downloadCliFromReleases(jfrogCliVersion, destinationPath);
        }
    }

    public void downloadCliFromReleases(String cliVersion, String destinationFolder) throws IOException {
        String[] urlParts = {"jfrog-cli/v2-jf", cliVersion, "jfrog-cli-" + getOSAndArc(), jfrogExec};
        String fileLocationInReleases = String.join("/", urlParts);
        Path basePath = Paths.get(destinationFolder);
        String destinationPath = basePath.resolve(jfrogExec).toString();
        String finalUrl = JFROG_CLI_RELEASES_URL + "/" + fileLocationInReleases;

        // download executable from releases and save it in 'destinationPath'
        try (InputStream in = new URL(finalUrl).openStream()){
            Files.copy(in, basePath.resolve(jfrogExec), StandardCopyOption.REPLACE_EXISTING);

            // setting the file as executable
            File cliExecutable = new File(String.valueOf(basePath.resolve(jfrogExec)));
            if (!cliExecutable.setExecutable(true)) {
                log.error(String.format("Failed to set downloaded CLI as executable. Path: %s", destinationPath));
            } else {
                log.debug(String.format("Downloaded CLI to %s. Permission te execute: %s", destinationPath, cliExecutable.canExecute()));
            }
        } catch (IOException e) {
            throw new IOException(String.format("Failed to download CLI from %s. Reason: %s", fileLocationInReleases, e.getMessage()), e.getCause());
        }
    }

    public CommandResults addCliServerConfig(String xrayUrl, String artifactoryUrl, String cliServerId, String user, String password, String accessToken, File workingDirectory, Map<String, String> envVars) throws Exception {
        List<String> args = new ArrayList<>();
        List<String> credentials = new ArrayList<>();

        args.add("config");
        args.add("add");
        args.add(cliServerId);
        args.add("--interactive=false");
        args.add("--overwrite");
        args.add("--enc-password=false");

        if (accessToken != null && !accessToken.isEmpty()) {
            credentials.add("--access-token=" + accessToken);
        } else {
            args.add("--user=" + user);
            credentials.add("--password=" + password);
        }

        args.add("--xray-url=" + xrayUrl);
        args.add("--artifactory-url=" + artifactoryUrl);

        try {
            return runCommand(workingDirectory, envVars, args.toArray(new String[0]), Collections.emptyList(), credentials ,log);
        } catch (IOException | InterruptedException e) {
            throw new Exception("Failed to configure JFrog CLI server. Reason: " + e.getMessage(), e);
        }
    }

    public CommandResults runCliAudit(File workingDirectory, List<String> scannedDirectories, String serverId, List<String> extraArgs, Map<String, String> envVars) throws Exception {
        AuditConfig config = new AuditConfig.Builder()
                .scannedDirectories(scannedDirectories)
                .serverId(serverId)
                .extraArgs(extraArgs)
                .envVars(envVars)
                .build();
        return runCliAudit(workingDirectory, config);
    }

    public CommandResults runCliAudit(File workingDirectory, AuditConfig config) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("audit");

        if (config.getScannedDirectories() != null && !config.getScannedDirectories().isEmpty()) {
            String workingDirsString = config.getScannedDirectories().size() > 1 ?
                    String.join(", ", config.getScannedDirectories()) :
                    config.getScannedDirectories().get(0);
            args.add("--working-dirs=" + quoteArgumentForUnix(workingDirsString));
        }

        args.add("--server-id=" + config.getServerId());
        args.add("--format=sarif");

        if (config.getExcludedPattern() != null && !config.getExcludedPattern().isEmpty()) {
            String excludedPatterns = String.join(",", config.getExcludedPattern());
            args.add("--exclusions=" + quoteArgumentForUnix(excludedPatterns));
        }

        try {
            return runCommand(workingDirectory, config.getEnvVars(), args.toArray(new String[0]),
                    config.getExtraArgs() != null ? config.getExtraArgs() : Collections.emptyList(), null, log);
        } catch (IOException | InterruptedException e) {
            throw new Exception("Failed to run JF audit. Reason: " + e.getMessage(), e);
        }
    }

    private String extractVersionFromCliOutput(String input) {
        if (input != null) {
            // define a pattern for the version format 'x.x.x'
            String regex = "\\b\\d+\\.\\d+\\.\\d+\\b";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(input);

            if (matcher.find()) {
                return matcher.group();
            }
        }

        return null;
    }

    private void addDefaultEnvVars(Map<String, String> env) {
        if (env != null) {
            env.put("JFROG_CLI_AVOID_NEW_VERSION_WARNING", "true");
        }
    }

    private String quoteArgumentForUnix(String commaSeparatedValues) {
        // macOS/Linux: add quotes around the comma-separated values
        return SystemUtils.IS_OS_WINDOWS ? commaSeparatedValues : "\"" + commaSeparatedValues + "\"";
    }
}
