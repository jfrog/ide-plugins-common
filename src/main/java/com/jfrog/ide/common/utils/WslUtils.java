package com.jfrog.ide.common.utils;

import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for running CLI tools in a WSL (Windows Subsystem for Linux) environment.
 * <p>
 * When IntelliJ runs on Windows and a project is located on a WSL filesystem
 * (e.g. {@code \\wsl.localhost\Ubuntu-22.04\home\user\project}), the Windows process
 * environment does not contain paths to CLI tools installed inside WSL.
 * <p>
 * This class solves the problem by creating lightweight {@code .cmd} shim files that
 * delegate to the WSL distro via {@code wsl.exe}, and prepending their directory to the
 * PATH in the environment map used by CLI drivers.
 */
public class WslUtils {

    static final String WSL_UNC_PREFIX = "\\\\wsl.localhost\\";
    static final String WSL_LEGACY_PREFIX = "\\\\wsl$\\";

    // Utility class
    private WslUtils() {
    }

    /**
     * Returns {@code true} if the given path lives on a WSL filesystem.
     */
    public static boolean isWslPath(String path) {
        if (path == null) return false;
        return path.startsWith(WSL_UNC_PREFIX) || path.startsWith(WSL_LEGACY_PREFIX);
    }

    /**
     * Extracts the WSL distro name from a UNC path.
     * E.g. {@code \\wsl.localhost\Ubuntu-22.04\home\...} → {@code Ubuntu-22.04}.
     */
    static String getWslDistro(String path) {
        String prefix = path.startsWith(WSL_UNC_PREFIX) ? WSL_UNC_PREFIX : WSL_LEGACY_PREFIX;
        String remainder = path.substring(prefix.length());
        int sep = remainder.indexOf('\\');
        return sep >= 0 ? remainder.substring(0, sep) : remainder;
    }

    /**
     * Returns an environment map augmented with {@code .cmd} shims for the given commands so
     * they delegate to the WSL distro that hosts the project. This allows Windows JVM processes
     * to invoke CLI tools that are only installed inside WSL.
     * <p>
     * Returns the original map unchanged when:
     * <ul>
     *   <li>the JVM is not running on Windows, or</li>
     *   <li>{@code projectDir} is {@code null}, or</li>
     *   <li>{@code projectDir} is not a WSL path.</li>
     * </ul>
     * Silently falls back to the original map on any I/O failure during shim creation.
     *
     * @param env        base environment map (e.g. from {@code EnvironmentUtil.getEnvironmentMap()})
     * @param projectDir the project directory; used to detect WSL and determine the distro name
     * @param commands   CLI command names for which to create shims (e.g. {@code "npm"}, {@code "go"})
     * @return augmented environment map, or the original map if augmentation is not needed/possible
     */
    public static Map<String, String> augmentForWsl(Map<String, String> env, Path projectDir, String... commands) {
        if (projectDir == null || !SystemUtils.IS_OS_WINDOWS) {
            return env;
        }
        String pathStr = projectDir.toString();
        if (!isWslPath(pathStr)) {
            return env;
        }

        String distro = getWslDistro(pathStr);
        try {
            Path shimDir = Files.createTempDirectory("jfrog-wsl-shim-");
            shimDir.toFile().deleteOnExit();
            for (String cmd : commands) {
                Path shim = shimDir.resolve(cmd + ".cmd");
                // CRLF line endings are required for Windows batch files
                String content = "@echo off\r\nwsl -d " + distro + " " + cmd + " %*\r\n";
                Files.write(shim, content.getBytes(StandardCharsets.US_ASCII));
                shim.toFile().deleteOnExit();
            }
            Map<String, String> augmented = new HashMap<>(env);
            String originalPath = augmented.getOrDefault("PATH", "");
            augmented.put("PATH", shimDir + File.pathSeparator + originalPath);
            return augmented;
        } catch (IOException e) {
            return env;
        }
    }
}
