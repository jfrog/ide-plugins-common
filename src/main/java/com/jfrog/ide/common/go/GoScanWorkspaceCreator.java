package com.jfrog.ide.common.go;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.extractor.WslUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.jfrog.ide.common.utils.Utils.createMapper;

/**
 * This FileVisitor copies all go.mod and *.go files from the input source directory to the input target directory.
 * For all go.mod files, replaces relative paths to absolute.
 * That functionality is needed, so that we can calculate the go dependencies tree on a copy of the original code project,
 * rather than on the original one, in order to avoid changing it.
 *
 * @author yahavi
 **/
public class GoScanWorkspaceCreator implements FileVisitor<Path> {
    private static final ObjectMapper jsonReader = createMapper();
    private static final String GO_MOD_VARIABLE = "JFROG_GO_MOD";
    private static final String REPLACE_FLAG_VARIABLE_PREFIX = "JFROG_GO_REPLACE_";
    private final String executablePath;
    private final Map<String, String> env;
    private final Path sourceDir;
    private final Path targetDir;
    private final Log logger;
    private final boolean runGoThroughWsl;
    private static final String[] EXCLUDED_DIRS = new String[]{".git", ".idea", ".vscode"};

    public GoScanWorkspaceCreator(String executablePath, Path sourceDir, Path targetDir,
                                  Map<String, String> env, Log logger, boolean runGoThroughWsl) {
        this.executablePath = executablePath;
        this.env = env;
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
        this.logger = logger;
        this.runGoThroughWsl = runGoThroughWsl;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        // Skip excluded directories.
        if (StringUtils.equalsAny(dir.getFileName().toString(), EXCLUDED_DIRS)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        // Skip subdirectories with go.mod files.
        // These directories are different Go projects and their go files should not be in the root project.
        if (!sourceDir.equals(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                if (files.anyMatch(file -> file.getFileName().toString().equals("go.mod"))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
        }

        Path resolve = targetDir.resolve(sourceDir.relativize(dir));
        if (Files.notExists(resolve)) {
            Files.createDirectories(resolve);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String fileName = file.getFileName().toString();

        // Go files should be copied to allow running `go list -f "{{with .Module}}{{.Path}} {{.Version}}{{end}}" all`
        // and to get the list package that are actually in use by the Go project.
        if (fileName.endsWith(".go")) {
            Files.copy(file, targetDir.resolve(sourceDir.relativize(file)));
            return FileVisitResult.CONTINUE;
        }
        // Copy the root go.mod file and replace relative path in "replace" to absolute paths.
        if (fileName.equals("go.mod")) {
            Path targetGoMod = targetDir.resolve(sourceDir.relativize(file));
            Files.copy(file, targetGoMod);
            makeReplacePathsAbsolute(targetGoMod);
            return FileVisitResult.CONTINUE;
        }
        // Files other than go.mod and *.go files are not necessary to build the dependency tree of used Go packages.
        // Therefore, we just create an empty file with the same name so go:embed files won't cause a missing file error.
        if (!fileName.equals("go.sum")) {
            Files.createFile(targetDir.resolve(sourceDir.relativize(file)));
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Rewrites every relative local path in the go.mod's replace directives to an absolute path under the source
     * directory, so the copied project still finds the modules it replaces.
     */
    private void makeReplacePathsAbsolute(Path goMod) throws IOException {
        Map<String, String> variables = new HashMap<>();
        variables.put(GO_MOD_VARIABLE, toWslPathWhenNeeded(goMod.toAbsolutePath()));
        String goModJson = runGo(List.of("mod", "edit", "-json", referenceVariable(GO_MOD_VARIABLE)), variables, false).getRes();

        // Build a -replace flag for each directive that replaces a module with a local path, not another module version.
        List<String> replaceFlagReferences = new ArrayList<>();
        for (JsonNode replaceDirective : jsonReader.readTree(goModJson).path("Replace")) {
            JsonNode replacement = replaceDirective.get("New");
            if (replacement.has("Version")) {
                continue;
            }
            String replaceFlag;
            try {
                replaceFlag = "-replace=" + formatReplacedModule(replaceDirective.get("Old")) + "=" + makeAbsolute(replacement.get("Path").asText());
            } catch (InvalidPathException e) {
                logger.warn("Skipping a go.mod replace directive with an invalid path: " + e.getMessage());
                continue;
            }
            if (SystemUtils.IS_OS_WINDOWS && breaksCmdQuoting(replaceFlag)) {
                logger.warn("Skipping a go.mod replace directive that can't be passed to go on Windows: " + replaceFlag);
                continue;
            }
            String replaceFlagVariable = REPLACE_FLAG_VARIABLE_PREFIX + replaceFlagReferences.size();
            variables.put(replaceFlagVariable, replaceFlag);
            replaceFlagReferences.add(referenceVariable(replaceFlagVariable));
        }
        if (replaceFlagReferences.isEmpty()) {
            return;
        }

        List<String> editArgs = new ArrayList<>(List.of("mod", "edit"));
        editArgs.addAll(replaceFlagReferences);
        editArgs.add(referenceVariable(GO_MOD_VARIABLE));
        runGo(editArgs, variables, true);
    }

    /**
     * Runs go with the given variables added to its environment. Values taken from the scanned project are passed
     * this way and referenced from the args, because GoDriver runs commands through a shell that would interpret them.
     */
    private CommandResults runGo(List<String> args, Map<String, String> variables, boolean verbose) throws IOException {
        Map<String, String> goEnv = env == null ? new HashMap<>() : new HashMap<>(env);
        goEnv.putAll(variables);
        return new GoDriver(executablePath, goEnv, targetDir.toFile(), logger, runGoThroughWsl).runCmd(args, verbose);
    }

    /**
     * When Go runs inside WSL, converts a Windows path to the Linux path Go sees there. Otherwise, returns it unchanged.
     */
    private String toWslPathWhenNeeded(Path path) {
        return runGoThroughWsl ? WslUtils.toWslLinuxCdPath(path.toFile()) : path.toString();
    }

    /**
     * Returns the absolute path, in the original project, of a replace path from the go.mod, converted for WSL when
     * needed. An absolute replace path is returned as is.
     */
    private String makeAbsolute(String replacementPath) {
        return toWslPathWhenNeeded(sourceDir.toAbsolutePath().resolve(replacementPath).normalize());
    }

    /**
     * Returns whether a value would end the quoting around its reference in "cmd /c", which cannot escape a double
     * quote inside a quoted argument.
     */
    private static boolean breaksCmdQuoting(String value) {
        return value.chars().anyMatch(character -> character == '"' || character < ' ');
    }

    /**
     * Returns a reference to an environment variable, which the shell GoDriver runs commands through ("/bin/sh -c" or
     * "cmd /c") replaces with the variable's value as one argument. Neither shell expands variables inside that value.
     */
    private static String referenceVariable(String variable) {
        return SystemUtils.IS_OS_WINDOWS ? "\"%" + variable + "%\"" : "\"$" + variable + "\"";
    }

    /**
     * Returns the replaced module as the -replace flag expects it: its path, followed by "@version" when the
     * replace directive applies to one version only.
     */
    private static String formatReplacedModule(JsonNode module) {
        String path = module.get("Path").asText();
        return module.has("Version") ? path + "@" + module.get("Version").asText() : path;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        if (exc != null) {
            logger.warn("An error occurred during preparing Go workspace " + ExceptionUtils.getRootCauseMessage(exc));
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        if (exc != null) {
            logger.warn("An error occurred during preparing Go workspace " + ExceptionUtils.getRootCauseMessage(exc));
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }
}
