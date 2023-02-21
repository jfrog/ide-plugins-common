package com.jfrog.ide.common.go;

import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by MichaelS
 */
public class GoComponentUpdaterTest {
    private static final Path GO_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "go"));
    private static final Log log = new NullLog();

    @Test
    public void testGoComponentUpdater() throws IOException {
        File tempProject = Files.createTempDirectory("ide-plugins-common-go").toFile();
        tempProject.deleteOnExit();
        FileUtils.copyDirectory(GO_ROOT.resolve("project1").toFile(), tempProject);
        GoComponentUpdater goComponentUpdater = new GoComponentUpdater(tempProject.toPath(), log, null, "");
        goComponentUpdater.run("github.com/jfrog/jfrog-cli-core", "1.10.0");
    }
}
