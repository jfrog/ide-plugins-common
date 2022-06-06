package com.jfrog.ide.common.gradle;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static java.lang.System.lineSeparator;

/**
 * @author yahavi
 **/
public class GradleDriver {
    private final CommandExecutor commandExecutor;

    /**
     * Create a Gradle driver. If the path to Gradle executable exists, use it. Otherwise, use Gradle from system path.
     *
     * @param gradleExe - The Gradle executable
     * @param env       - Environment variables
     */
    public GradleDriver(String gradleExe, Map<String, String> env) {
        this.commandExecutor = new CommandExecutor(StringUtils.defaultIfBlank(gradleExe, "gradle"), env);
    }

    /**
     * Run `gradle --version` command. If an error occurred - print it.
     *
     * @throws IOException if any error occurred.
     */
    public void verifyGradleInstalled() throws IOException {
        try {
            version(null);
        } catch (IOException | InterruptedException e) {
            throw new IOException("Could not scan Gradle project dependencies, " +
                    "because Gradle project was not configured properly or Gradle is not in the system path.", e);
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
    public List<File> generateDependenciesGraphAsJson(File workingDirectory, Log logger) throws IOException {
        String encodedPath = Base64.getEncoder().encodeToString(workingDirectory.getName().getBytes(StandardCharsets.UTF_8));

        // Create temp init script file
        Path initScript = Files.createTempFile("init-script", encodedPath);
        logger.debug("dependencies.gradle init script path: " + initScript);
        try (InputStream gradleInitScript = getClass().getResourceAsStream("/gradle-dep-tree.gradle")) {
            if (gradleInitScript == null) {
                throw new IOException("Couldn't find dependencies.gradle init script.");
            }

            // Copy init script to the temp file
            Files.copy(gradleInitScript, initScript, StandardCopyOption.REPLACE_EXISTING);

            // Run "gradle generateDepTrees -q -I <path-to-init-script>"
            List<String> args = Lists.newArrayList("generateDepTrees", "-q", "-I", initScript.toString());
            CommandResults results = runCommand(workingDirectory, args, logger);
            List<File> files = new ArrayList<>();
            for (String line : results.getRes().split(lineSeparator())) {
                line = StringUtils.trimToNull(line);
                if (line == null) {
                    continue;
                }
                files.add(new File(line));
            }

            return files;
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
