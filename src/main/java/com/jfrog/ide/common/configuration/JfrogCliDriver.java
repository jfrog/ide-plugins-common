package com.jfrog.ide.common.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.log.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.versioning.ComparableVersion;

import static com.jfrog.ide.common.utils.Utils.createMapper;

/**
 * @author Tal Arian
 */
public class JfrogCliDriver {
    private static final String JFROG_CLI_RELEASES_URL = "https://releases.jfrog.io/artifactory/jfrog-cli/v2-jf/";
    private static final String MINIMUM_JFROG_CLI_VERSION = "2.69.0"; // TODO: TBD
    private static final String MAXIMUM_JFROG_CLI_VERSION = "2.73.3"; // TODO: TBD
    public static final String DEFAULT_CLI_DESTINATION_PATH = ""; // TODO: determine where we would like to download and save the cli
    private static final ObjectMapper jsonReader = createMapper();
    private final CommandExecutor commandExecutor;
    private Log log;
    private String osAndArch;
    private String jfrogExec = "jf";

    @SuppressWarnings("unused")
    public JfrogCliDriver(Map<String, String> env, Log log) throws IOException {
        this(env, "", log);
    }

    public JfrogCliDriver(Map<String, String> env, String path, Log log) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            this.jfrogExec += ".exe";
        }
        this.commandExecutor = new CommandExecutor(Paths.get(path, this.jfrogExec).toString(), env);
        this.osAndArch = getOSAndArc();
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

    public void downloadCliIfNeeded() {
        downloadCliIfNeeded(DEFAULT_CLI_DESTINATION_PATH);
    }

    public void downloadCliIfNeeded(String destinationPath) {
        Path jfrogExeFilePath = Paths.get(destinationPath);

        if(isJfrogCliInstalled()){
            // verify installed cli version
            CommandExecutor commandExecutor = new CommandExecutor(jfrogExeFilePath.toString(), null);
            List<String> versionCommand = Arrays.asList("--version");

            try {
                CommandResults versionCommandOutput = commandExecutor.exeCommand(null, versionCommand, null, log);
                String cliVersion = extractVersion(versionCommandOutput.getRes());

                if (validateCLIVersion(cliVersion)) {
                    log.debug("Local CLI version is: " + cliVersion);
                    log.info("Local 'jf.exe' file version has been verified and is compatible. Proceeding with its usage.");
                } else {
                    log.info("Local 'jf.exe' file version is not compatible. Downloading v" + MAXIMUM_JFROG_CLI_VERSION);
                    downloadCliFromReleases(MAXIMUM_JFROG_CLI_VERSION, destinationPath);
                }
            } catch (InterruptedException | IOException e) {
                // TODO: should we fail in case of error or download a new cli exe ?
                log.error("Failed to verify CLI version. Downloading v"+ MAXIMUM_JFROG_CLI_VERSION);
                downloadCliFromReleases(MAXIMUM_JFROG_CLI_VERSION, destinationPath);
            }
        } else {
            // download cli
            downloadCliFromReleases(MAXIMUM_JFROG_CLI_VERSION, destinationPath);
        }
    }

    public void downloadCliFromReleases(String cliVersion, String destinationPath) {
        String fullDownloadPath = JFROG_CLI_RELEASES_URL + cliVersion + "/jfrog-cli-" + this.osAndArch + this.jfrogExec;

        // TODO: download executable from 'fullCLIPath' and save it in 'destinationPath'
    }

    private String getOSAndArc() throws IOException {
        String arch = SystemUtils.OS_ARCH;
        // Windows
        if (SystemUtils.IS_OS_WINDOWS) {
            return "windows-amd64";
        }
        // Mac
        if (SystemUtils.IS_OS_MAC) {
            if (StringUtils.equalsAny(arch, "aarch64", "arm64")) {
                return "mac-arm64";
            } else {
                return "mac-amd64";
            }
        }
        // Linux
        if (SystemUtils.IS_OS_LINUX) {
            switch (arch) {
                case "i386":
                case "i486":
                case "i586":
                case "i686":
                case "i786":
                case "x86":
                    return "linux-386";
                case "amd64":
                case "x86_64":
                case "x64":
                    return "linux-amd64";
                case "arm":
                case "armv7l":
                    return "linux-arm";
                case "aarch64":
                    return "linux-arm64";
                case "ppc64":
                case "ppc64le":
                    return "linux-" + arch;
            }
        }
        throw new IOException(String.format("Unsupported OS: %s-%s", SystemUtils.OS_NAME, arch));
    }

    private Boolean validateCLIVersion(String cliVersion) {
        ComparableVersion currentCLIVersion = new ComparableVersion(cliVersion);
        ComparableVersion maxCLIVersion = new ComparableVersion(MAXIMUM_JFROG_CLI_VERSION);
        ComparableVersion minCLIVersion = new ComparableVersion(MINIMUM_JFROG_CLI_VERSION);

        return currentCLIVersion.compareTo(minCLIVersion) >=0 && currentCLIVersion.compareTo(maxCLIVersion) <= 0;
    }

    private String extractVersion(String input) {
        String regex = "\\d+(\\.\\d+)*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
