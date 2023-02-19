package com.jfrog.ide.common.go;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.fail;

/**
 * Created by MichaelS
 */
public class GoGetTest {
    private static final Path GO_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "go"));
    private static final Log log = new NullLog();

    @Test
    public void testGoGet() {
        try {
            File tempProject = Files.createTempDirectory("ide-plugins-common-go").toFile();
            tempProject.deleteOnExit();
            FileUtils.copyDirectory(GO_ROOT.resolve("project1").toFile(), tempProject);
            GoGet goGet = new GoGet(null, tempProject.toPath(), null, log);
            goGet.run("github.com/jfrog/jfrog-cli-core", "1.10.0");
        } catch (Exception ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }
}
