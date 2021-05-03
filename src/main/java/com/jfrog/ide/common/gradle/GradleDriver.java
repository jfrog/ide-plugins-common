package com.jfrog.ide.common.gradle;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * @author yahavi
 **/
public class GradleDriver {
    private final CommandExecutor commandExecutor;

    public GradleDriver(Path workingDirectory, Map<String, String> env) {
        if (SystemUtils.IS_OS_WINDOWS) {
            if (Files.exists(workingDirectory.resolve("gradlew.bat"))) {
                this.commandExecutor = new CommandExecutor("gradlew.bat", env);
                return;
            }
        } else {
            if (Files.exists(workingDirectory.resolve("gradlew.wrapper"))) {
                this.commandExecutor = new CommandExecutor("gradlew.wrapper", env);
                return;
            }
        }
        this.commandExecutor = new CommandExecutor("gradle", env);
    }

    @SuppressWarnings("unused")
    public boolean isGradleInstalled() {
        try {
            version(null);
            return true;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public File[] generateDependenciesGraphAsJson(File workingDirectory, Log logger) throws IOException {
        List<String> args = new ArrayList<>();
        args.add("generateDependenciesGraphAsJson");
        args.add("-I");
        Path tempDirectory = Files.createTempDirectory(null);
        try (InputStream gradleInitScript = getClass().getResourceAsStream("/dependencies.gradle")) {
            if (gradleInitScript == null) {
                throw new IOException("Couldn't find dependencies.gradle init script.");
            }
            Path tmpGradleInit = tempDirectory.resolve("dependencies.gradle");
            Files.copy(gradleInitScript, tmpGradleInit);
            args.add(tmpGradleInit.toString());
            runCommand(workingDirectory, args, logger);
            String encodedPath = Base64.getEncoder().encodeToString(workingDirectory.getName().getBytes(StandardCharsets.UTF_8));
            Path path = Paths.get(System.getProperty("user.home"), ".jfrog-ide-plugins", "gradle-dependencies", encodedPath);
            return path.toFile().listFiles();
        } catch (IOException | InterruptedException e) {
            throw new IOException("Couldn't build Gradle dependency tree in workspace '" + workingDirectory + "': " + ExceptionUtils.getRootCauseMessage(e), e);
        } finally {
            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
    }

    public String version(File workingDirectory) throws IOException, InterruptedException {
        return runCommand(workingDirectory, Lists.newArrayList("--version"), null).getRes();
    }

    private CommandResults runCommand(File workingDirectory, List<String> args, Log logger) throws IOException, InterruptedException {
        CommandResults npmCommandRes = commandExecutor.exeCommand(workingDirectory, args, null, logger);

        if (!npmCommandRes.isOk()) {
            throw new IOException(npmCommandRes.getErr() + npmCommandRes.getRes());
        }

        return npmCommandRes;
    }
}
