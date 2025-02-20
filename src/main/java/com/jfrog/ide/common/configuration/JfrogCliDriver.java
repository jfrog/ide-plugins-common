package com.jfrog.ide.common.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jfrog.ide.common.utils.ArtifactoryConnectionUtils.createAnonymousAccessArtifactoryManagerBuilder;
import static com.jfrog.ide.common.utils.Utils.createMapper;

/**
 * @author Tal Arian
 */
public class JfrogCliDriver {
    private static final String JFROG_CLI_RELEASES_URL = "https://releases.jfrog.io/artifactory";
    private static final String JFROG_CLI_VERSION = "2.73.3"; // TODO: TBD
    private static final ObjectMapper jsonReader = createMapper();
    private final CommandExecutor commandExecutor;
    private final String osAndArch;
    private final Log log;
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
        this.log = log;
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
        String versionOutput = null;
        try{
            versionOutput = runCommand(workingDirectory, new String[]{"--version"}, Collections.emptyList()).getRes();
        } catch (IOException e) {
            log.error("Failed to get CLI version. Reason: " + e.getMessage());
        }
        return versionOutput;
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

    public void downloadCliIfNeeded(String destinationPath) {
        if(isJfrogCliInstalled()){
            // verify installed cli version
            try {
                String cliVersion = extractVersion(version(null));
                log.debug("Local CLI version is: " + cliVersion);

                if (cliVersion != null && cliVersion.equals(JFROG_CLI_VERSION)) {
                    log.info("Local 'jf.exe' file version has been verified and is compatible. Proceeding with its usage.");
                } else {
                    log.info(String.format("Local 'jf.exe' file version is not compatible. Downloading v%s to destination: %s", JFROG_CLI_VERSION, destinationPath));
                    downloadCliFromReleases(JFROG_CLI_VERSION, destinationPath);
                }
            } catch (InterruptedException | IOException e) {
                // TODO: should we fail in case of error or download a new cli exe ?
                log.error("Failed to verify CLI version. Downloading v"+ JFROG_CLI_VERSION);
                downloadCliFromReleases(JFROG_CLI_VERSION, destinationPath);
            }
        } else {
            // download cli
            downloadCliFromReleases(JFROG_CLI_VERSION, destinationPath);
        }
    }

    public void downloadCliFromReleases(String cliVersion, String destinationPath) {
        String[] urlParts = {"jfrog-cli/v2-jf", cliVersion, "jfrog-cli-" + osAndArch, jfrogExec};
        String fileLocationInReleases = String.join("/", urlParts);

        // download executable from releases and save it in 'destinationPath'
        try{
            ServerConfig serverConfig = getServerConfig();
            ArtifactoryManagerBuilder artifactoryManagerBuilder = createAnonymousAccessArtifactoryManagerBuilder(JFROG_CLI_RELEASES_URL, serverConfig.getProxyConfForTargetUrl(JFROG_CLI_RELEASES_URL), log);
            ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build();
            File cliExecutable = artifactoryManager.downloadToFile(fileLocationInReleases, destinationPath);
            log.debug(String.format("Downloaded CLI to %s. Permission te execute: %s", destinationPath, cliExecutable.canExecute()));
        } catch (IOException e) {
            log.error(String.format("Failed to download CLI from %s. Reason: %s", fileLocationInReleases, e.getMessage()), e);
        }
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

    private String extractVersion(String input) {
        // define a pattern for the version format 'x.x.x'
        String regex = "\\b\\d+\\.\\d+\\.\\d+\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        JfrogCliDriver driver = new JfrogCliDriver(null, new NullLog());
        String destinationPath = "C:\\Users\\Keren Reshef\\Downloads\\jfrog-cli.exe";
        driver.downloadCliIfNeeded(destinationPath);
    }
}
