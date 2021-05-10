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
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * @author yahavi
 **/
public class GradleDriver {
    private static final Path GRADLE_DEPS = Paths.get(System.getProperty("user.home"), ".jfrog-ide-plugins", "gradle-dependencies");
    private final CommandExecutor commandExecutor;

    /**
     * Create a Gradle driver. If Gradle wrapper exist, use it. Otherwise use Gradle from Path.
     *
     * @param workingDirectory - The project working directory
     * @param env              - Environment variables
     */
    public GradleDriver(Path workingDirectory, Map<String, String> env) {
        String wrapperExe = SystemUtils.IS_OS_WINDOWS ? "gradlew.bat" : "gradlew";
        Path gradleWrapper = workingDirectory.resolve(wrapperExe).toAbsolutePath();
        if (Files.exists(gradleWrapper)) {
            this.commandExecutor = new CommandExecutor(gradleWrapper.toString(), env);
            return;
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

    /**
     * Run 'generateDependenciesGraphAsJson' on the input project.
     * The task builds dependencies trees to each one of the Gradle projects under the working directory.
     * The dependency trees are stored under <user-home>/.jfrog-ide-plugins/gradle-dependencies/<project-base64-key>
     *
     * @param workingDirectory - The project directory
     * @param logger           - The logger
     * @return list of files containing the dependency trees of the Gradle projects.
     * @throws IOException in case of any I/O error.
     */
    public File[] generateDependenciesGraphAsJson(File workingDirectory, Log logger) throws IOException {
        String encodedPath = Base64.getEncoder().encodeToString(workingDirectory.getName().getBytes(StandardCharsets.UTF_8));

        // Create temp init script file
        Path initScript = Files.createTempFile(null, encodedPath);
        logger.debug("dependencies.gradle init script path: " + initScript);
        try (InputStream gradleInitScript = getClass().getResourceAsStream("/dependencies.gradle")) {
            if (gradleInitScript == null) {
                throw new IOException("Couldn't find dependencies.gradle init script.");
            }

            // Copy init script to the temp file
            Files.copy(gradleInitScript, initScript, StandardCopyOption.REPLACE_EXISTING);

            // Run "gradle generateDependenciesGraphAsJson -I <path-to-init-script>"
            List<String> args = Lists.newArrayList("generateDependenciesGraphAsJson", "-I", initScript.toString());
            runCommand(workingDirectory, args, logger);

            // Return all files under <user-home>/.jfrog-ide-plugins/gradle-dependencies/<encodedPath>
            return GRADLE_DEPS.resolve(encodedPath).toFile().listFiles();
        } catch (IOException | InterruptedException e) {
            throw new IOException("Couldn't build Gradle dependency tree in workspace '" + workingDirectory + "': " + ExceptionUtils.getRootCauseMessage(e), e);
        } finally {
            FileUtils.forceDelete(initScript.toFile());
        }
    }

    public String version(File workingDirectory) throws IOException, InterruptedException {
        return runCommand(workingDirectory, Lists.newArrayList("--version"), null).getRes();
    }

    private CommandResults runCommand(File workingDirectory, List<String> args, Log logger) throws IOException, InterruptedException {
        CommandResults gradleCommandRes = commandExecutor.exeCommand(workingDirectory, args, null, logger);
        if (!gradleCommandRes.isOk()) {
            throw new IOException(gradleCommandRes.getErr() + gradleCommandRes.getRes());
        }
        return gradleCommandRes;
    }
}
