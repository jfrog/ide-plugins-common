package com.jfrog.ide.common.npm;

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
public class NpmComponentUpdaterTest {
    private static final Path GO_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "npm"));
    private static final Log log = new NullLog();

    @Test
    public void testNpmComponentUpdater() {
        try {
            File tempProject = Files.createTempDirectory("ide-plugins-common-go").toFile();
            tempProject.deleteOnExit();
            FileUtils.copyDirectory(GO_ROOT.resolve("dependency").toFile(), tempProject);
            NpmComponentUpdater npmComponentUpdater = new NpmComponentUpdater(tempProject.toPath(), log, null);
            npmComponentUpdater.run("progress", "2.0.2");
        } catch (Exception ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }
}
