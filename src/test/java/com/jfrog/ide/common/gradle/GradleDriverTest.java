package com.jfrog.ide.common.gradle;

import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertTrue;

/**
 * @author yahavi
 **/
public class GradleDriverTest {
    private static final Path GRADLE_WRAPPER_ROOT = Paths.get(".").toAbsolutePath().normalize()
            .resolve(Paths.get("src", "test", "resources", "gradle", "wrapper"));

    @Test
    public void testIsGradleInstalled() {
        GradleDriver gradleDriver = new GradleDriver(GRADLE_WRAPPER_ROOT, null);
        assertTrue(gradleDriver.isGradleInstalled());
    }
}
