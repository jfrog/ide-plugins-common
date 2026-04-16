package com.jfrog.ide.common.utils;

import java.nio.file.Path;

/**
 * Utility methods for WSL (Windows Subsystem for Linux) path handling.
 * On Windows, WSL filesystems are exposed as UNC paths under the wsl.localhost or wsl$ hosts.
 */
public class WslUtils {
    // UNC-style prefixes used by Windows to expose WSL filesystems
    private static final String WSL_LOCALHOST_PREFIX = "\\\\wsl.localhost\\";
    private static final String WSL_DOLLAR_PREFIX = "\\\\wsl$\\";

    private WslUtils() {
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        return s.length() >= prefix.length() && s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Normalizes Windows extended-length UNC prefixes so WSL detection sees {@code \\wsl$\...} / {@code \\wsl.localhost\...}.
     * Example: {@code \\?\UNC\wsl$\Ubuntu\home\...} becomes {@code \\wsl$\Ubuntu\home\...}.
     */
    static String normalizePathStringForWsl(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String p = path;
        if (startsWithIgnoreCase(p, "\\\\?\\UNC\\")) {
            return "\\\\" + p.substring("\\\\?\\UNC\\".length());
        }
        if (startsWithIgnoreCase(p, "\\\\?\\")) {
            return p.substring("\\\\?\\".length());
        }
        return p;
    }

    /**
     * Returns true if the given path string refers to a WSL filesystem
     * (i.e. it is a UNC path rooted at the wsl.localhost or wsl$ host).
     */
    public static boolean isWslPath(String path) {
        if (path == null) {
            return false;
        }
        String normalized = normalizePathStringForWsl(path);
        return startsWithIgnoreCase(normalized, WSL_LOCALHOST_PREFIX) || startsWithIgnoreCase(normalized, WSL_DOLLAR_PREFIX);
    }

    /**
     * Returns true if the given {@link Path} refers to a WSL filesystem.
     */
    public static boolean isWslPath(Path path) {
        return path != null && isWslPath(path.toString());
    }

    /**
     * Converts a Windows-style WSL UNC path to the equivalent Linux path inside WSL.
     * The distro name (first UNC component after the host) is stripped, and
     * backslashes are replaced with forward slashes.
     *
     * @param wslWindowsPath the Windows-style WSL path
     * @return the Linux path, or the original string unchanged if it is not a WSL path
     */
    public static String toLinuxPath(String wslWindowsPath) {
        if (wslWindowsPath == null) {
            return null;
        }
        String p = normalizePathStringForWsl(wslWindowsPath);
        String withoutPrefix;
        if (startsWithIgnoreCase(p, WSL_LOCALHOST_PREFIX)) {
            withoutPrefix = p.substring(WSL_LOCALHOST_PREFIX.length());
        } else if (startsWithIgnoreCase(p, WSL_DOLLAR_PREFIX)) {
            withoutPrefix = p.substring(WSL_DOLLAR_PREFIX.length());
        } else {
            return wslWindowsPath;
        }
        // withoutPrefix is now: <distro>\<rest-of-path>
        // Strip the distro name (first path component) to obtain the Linux path.
        int firstBackslash = withoutPrefix.indexOf('\\');
        if (firstBackslash == -1) {
            return "/"; // Path pointed at the distro root
        }
        return withoutPrefix.substring(firstBackslash).replace('\\', '/');
    }
}
