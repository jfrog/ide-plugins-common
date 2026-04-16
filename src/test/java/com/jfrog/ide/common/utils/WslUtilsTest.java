package com.jfrog.ide.common.utils;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class WslUtilsTest {

    @DataProvider
    public Object[][] wslPathDetection() {
        return new Object[][]{
                {null, false},
                {"C:\\dev\\project", false},
                {"\\\\wsl$\\Ubuntu\\home\\user\\repo", true},
                {"\\\\WSL$\\Ubuntu\\home\\user\\repo", true},
                {"\\\\wsl.localhost\\Ubuntu\\home\\user\\repo", true},
                {"\\\\WSL.LOCALHOST\\Ubuntu\\home\\user\\repo", true},
        };
    }

    @Test(dataProvider = "wslPathDetection")
    public void testIsWslPathString(String path, boolean expected) {
        assertEquals(WslUtils.isWslPath(path), expected);
    }

    @Test(dataProvider = "wslPathDetection")
    public void testIsWslPathPathObject(String pathString, boolean expected) {
        if (pathString == null) {
            assertFalse(WslUtils.isWslPath((Path) null));
        } else {
            assertEquals(WslUtils.isWslPath(Path.of(pathString)), expected);
        }
    }

    @DataProvider
    public Object[][] toLinuxPathCases() {
        return new Object[][]{
                {null, null},
                {"C:\\dev", "C:\\dev"},
                {"\\\\wsl$\\Ubuntu\\home\\user\\app", "/home/user/app"},
                {"\\\\WSL$\\Ubuntu\\home\\user\\app", "/home/user/app"},
                {"\\\\wsl.localhost\\Ubuntu\\home\\user\\app", "/home/user/app"},
                {"\\\\wsl$\\Ubuntu", "/"},
        };
    }

    @Test(dataProvider = "toLinuxPathCases")
    public void testToLinuxPath(String input, String expected) {
        assertEquals(WslUtils.toLinuxPath(input), expected);
    }
}
