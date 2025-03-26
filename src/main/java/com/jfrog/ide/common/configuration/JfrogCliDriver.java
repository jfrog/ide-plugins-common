package com.jfrog.ide.common.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.Version;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jfrog.ide.common.utils.ArtifactoryConnectionUtils.createAnonymousAccessArtifactoryManagerBuilder;
import static com.jfrog.ide.common.utils.Utils.createMapper;
import static com.jfrog.ide.common.utils.Utils.getOSAndArc;

/**
 * @author Tal Arian
 */
public class JfrogCliDriver {
    private static final String JFROG_CLI_RELEASES_URL = "https://releases.jfrog.io/artifactory";
    private static final ObjectMapper jsonReader = createMapper();
    private final CommandExecutor commandExecutor;
    private final Log log;

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
        this.commandExecutor = new CommandExecutor(Paths.get(path, this.jfrogExec).toString(), env);
        this.log = log;
    }

    @SuppressWarnings("unused")
    public boolean isJfrogCliInstalled() {
        return runVersion(null) != null;
    }

    @SuppressWarnings("unused")
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
            throw new IOException("'jfrog config export' command failed. " +
                    "That might be happen if you haven't config any CLI server yet or using the config encryption feature.", e);
        }
    }

    public String runVersion(File workingDirectory) {
        String versionOutput = null;
        try {
            versionOutput = runCommand(workingDirectory, new String[]{"--version"}, Collections.emptyList()).getRes();
        } catch (IOException | InterruptedException e) {
            log.error("Failed to get CLI version. Reason: " + e.getMessage());
        }

        return versionOutput;
    }

    private CommandResults runCommand(File workingDirectory, String[] args, List<String> extraArgs) throws IOException,
            InterruptedException {
        return runCommand(workingDirectory, args, extraArgs, null);
    }

    public CommandResults runCommand(File workingDirectory, String[] args, List<String> extraArgs, Log logger)
            throws IOException, InterruptedException {
        List<String> finalArgs = Stream.concat(Arrays.stream(args), extraArgs.stream()).collect(Collectors.toList());
        CommandResults commandResults = commandExecutor.exeCommand(workingDirectory, finalArgs, null, logger);
        if (!commandResults.isOk()) {
            throw new IOException(commandResults.getErr() + commandResults.getRes());
        }

        return commandResults;
    }

    public void downloadCliIfNeeded(String destinationPath, String jfrogCliVersion) throws IOException {
        Version requestedVersion = new Version(jfrogCliVersion);
        // verify installed cli version
        Version cliVersion = extractVersionFromCliOutput(runVersion(null));
        log.debug("Local CLI version is: " + cliVersion);
        // cli is installed but not the correct version
        if (cliVersion != null && cliVersion.equals(requestedVersion)) {
            log.info("Local Jfrog CLI version has been verified and is compatible. Proceeding with its usage.");
        } else {
            log.info(String.format("JFrog CLI is either not installed or the current version is incompatible. " +
                    "Initiating download of version %s to the destination: %s.", requestedVersion, destinationPath));
            downloadCliFromReleases(requestedVersion, destinationPath);
        }
    }

    public void downloadCliFromReleases(Version cliVersion, String destinationFolder) throws IOException {
        String[] urlParts = {"jfrog-cli/v2-jf", cliVersion.toString(), "jfrog-cli-" + getOSAndArc(), jfrogExec};
        String fileLocationInReleases = String.join("/", urlParts);
        Path basePath = Paths.get(destinationFolder);
        String destinationPath = basePath.resolve(jfrogExec).toString();

        // download executable from releases and save it in 'destinationPath'
        try {
            ServerConfig serverConfig = getServerConfig();
            ArtifactoryManagerBuilder artifactoryManagerBuilder = createAnonymousAccessArtifactoryManagerBuilder(JFROG_CLI_RELEASES_URL, serverConfig.getProxyConfForTargetUrl(JFROG_CLI_RELEASES_URL), log);
            ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build();
            File cliExecutable = artifactoryManager.downloadToFile(fileLocationInReleases, destinationPath);
            // setting the file as executable
            if (!cliExecutable.setExecutable(true)) {
                log.error(String.format("Failed to set downloaded CLI as executable. Path: %s", destinationPath));
            } else {
                log.debug(String.format("Downloaded CLI to %s. Permission te execute: %s", destinationPath, cliExecutable.canExecute()));
            }
        } catch (IOException e) {
            log.error(String.format("Failed to download CLI from %s. Reason: %s", fileLocationInReleases, e.getMessage()), e);
            throw e;
        }
    }

    public void addCliServerConfig(String xrayUrl, String artifactoryUrl, String cliServerId, String user, String password, String accessToken, File workingDirectory) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("config");
        args.add("add");
        args.add(cliServerId);
        args.add("--xray-url=" + xrayUrl);
        args.add("--artifactory-url=" + artifactoryUrl);
        args.add("--interactive=false");
        args.add("--overwrite");

        if (accessToken != null && !accessToken.isEmpty()) {
            args.add("--access-token=" + accessToken);
        } else {
            args.add("--user=" + user);
            args.add("--password=" + password);
            args.add("--enc-password=false");
        }

        try {
            runCommand(workingDirectory, args.toArray(new String[0]), Collections.emptyList(), log);
            log.info("JFrog CLI server has been configured successfully");
        } catch (IOException | InterruptedException e) {
            log.error("Failed to configure JFrog CLI server. Reason: " + e.getMessage(), e);
                throw new Exception("Failed to configure JFrog CLI server. Reason: " + e.getMessage(), e);
        }
    }

    public CommandResults runCliAudit(File workingDirectory, List<String> scannedDirectories, String serverId, List<String> extraArgs) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("audit");
        if (scannedDirectories != null && !scannedDirectories.isEmpty()) {
            String workingDirsString = scannedDirectories.size() > 1 ? String.join(", ", scannedDirectories) : scannedDirectories.get(0);
            args.add("--working-dirs=" + workingDirsString);
        }
        args.add("--server-id=" + serverId);
        args.add("--format=sarif");
        try {
            return runCommand(workingDirectory, args.toArray(new String[0]), extraArgs != null ? extraArgs : Collections.emptyList(), log);
        } catch (IOException | InterruptedException e) {
            throw new Exception("Failed to run JF audit. Reason: " + e.getMessage(), e);
        }
    }

    private Version extractVersionFromCliOutput(String input) {
        if (input != null) {
            // define a pattern for the version format 'x.x.x'
            String regex = "\\b\\d+\\.\\d+\\.\\d+\\b";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(input);

            if (matcher.find()) {
                return new Version(matcher.group());
            }
        }

        return null;
    }
}
