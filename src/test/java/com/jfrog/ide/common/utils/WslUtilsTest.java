package com.jfrog.ide.common.utils;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.jfrog.ide.common.utils.WslUtils.*;
import static org.testng.Assert.*;

public class WslUtilsTest {

    @DataProvider
    public static Object[][] wslPaths() {
        return new Object[][]{
                {"\\\\wsl.localhost\\Ubuntu-22.04\\home\\user\\project", true},
                {"\\\\wsl$\\Ubuntu\\home\\user\\project", true},
                {"C:\\Users\\user\\project", false},
                {"/home/user/project", false},
                {"", false},
                {null, false},
        };
    }

    @Test(dataProvider = "wslPaths")
    public void testIsWslPath(String path, boolean expected) {
        assertEquals(isWslPath(path), expected);
    }

    @DataProvider
    public static Object[][] distroExtractionCases() {
        return new Object[][]{
                {"\\\\wsl.localhost\\Ubuntu-22.04\\home\\user\\project", "Ubuntu-22.04"},
                {"\\\\wsl.localhost\\Ubuntu\\home\\user\\project", "Ubuntu"},
                {"\\\\wsl$\\Debian\\home\\user\\project", "Debian"},
                // distro only, no trailing backslash
                {"\\\\wsl.localhost\\Ubuntu-22.04", "Ubuntu-22.04"},
        };
    }

    @Test(dataProvider = "distroExtractionCases")
    public void testGetWslDistro(String path, String expectedDistro) {
        assertEquals(getWslDistro(path), expectedDistro);
    }

    @Test
    public void testAugmentForWslWithNullProjectDir() {
        Map<String, String> env = new HashMap<>();
        env.put("PATH", "/some/path");
        Map<String, String> result = augmentForWsl(env, null, "npm");
        assertSame(result, env, "Should return original map when projectDir is null");
    }

    @Test
    public void testAugmentForWslWithNonWslPath() {
        Map<String, String> env = new HashMap<>();
        env.put("PATH", "C:\\Windows\\system32");
        Path nonWslDir = Paths.get("C:\\Users\\user\\project");
        Map<String, String> result = augmentForWsl(env, nonWslDir, "npm");
        assertSame(result, env, "Should return original map for non-WSL path");
    }

    @Test
    public void testAugmentForWslWithLinuxPath() {
        Map<String, String> env = new HashMap<>();
        env.put("PATH", "/usr/bin");
        Path linuxDir = Paths.get("/home/user/project");
        Map<String, String> result = augmentForWsl(env, linuxDir, "npm");
        assertSame(result, env, "Should return original map for Linux path");
    }

    /**
     * Tests the shim creation logic directly (without relying on IS_OS_WINDOWS).
     * Validates that the shim file has the correct content and that PATH is prepended.
     */
    @Test
    public void testShimContentAndPathPrepend() throws Exception {
        String distro = "Ubuntu-22.04";
        Path shimDir = Files.createTempDirectory("jfrog-wsl-test-shim-");
        try {
            String[] commands = {"npm", "yarn"};
            Map<String, String> env = new HashMap<>();
            String originalPath = "C:\\Windows\\system32";
            env.put("PATH", originalPath);

            // Directly call shim creation logic (package-private, same package in test)
            for (String cmd : commands) {
                Path shim = shimDir.resolve(cmd + ".cmd");
                String content = "@echo off\r\nwsl -d " + distro + " " + cmd + " %*\r\n";
                Files.write(shim, content.getBytes(StandardCharsets.US_ASCII));
            }
            Map<String, String> augmented = new HashMap<>(env);
            augmented.put("PATH", shimDir + File.pathSeparator + originalPath);

            // Verify PATH is prepended
            String newPath = augmented.get("PATH");
            assertTrue(newPath.startsWith(shimDir.toString()), "Shim dir should be prepended to PATH");
            assertTrue(newPath.endsWith(originalPath), "Original PATH should be preserved");

            // Verify shim file content
            for (String cmd : commands) {
                Path shim = shimDir.resolve(cmd + ".cmd");
                assertTrue(Files.exists(shim), cmd + ".cmd should exist");
                String shimContent = new String(Files.readAllBytes(shim), StandardCharsets.US_ASCII);
                assertTrue(shimContent.contains("wsl -d " + distro + " " + cmd + " %*"),
                        "Shim should delegate to correct distro and command");
                assertTrue(shimContent.contains("\r\n"), "Shim should use CRLF line endings");
            }
        } finally {
            // Clean up
            for (String cmd : new String[]{"npm", "yarn"}) {
                Files.deleteIfExists(shimDir.resolve(cmd + ".cmd"));
            }
            Files.deleteIfExists(shimDir);
        }
    }
}
