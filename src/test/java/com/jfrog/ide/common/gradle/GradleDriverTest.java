package com.jfrog.ide.common.gradle;

import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author yahavi
 **/
public class GradleDriverTest {

    @Test
    public void testIsGradleInstalled() throws IOException {
        GradleDriver gradleDriver = new GradleDriver("", null);
        gradleDriver.verifyGradleInstalled();
    }
}
