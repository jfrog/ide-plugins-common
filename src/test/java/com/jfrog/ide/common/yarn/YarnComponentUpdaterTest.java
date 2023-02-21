package com.jfrog.ide.common.yarn;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by MichaelS
 */
public class YarnComponentUpdaterTest {
    private static final Path GO_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "yarn"));

    @Test
    public void testYarnComponentUpdater() throws IOException {
        File tempProject = Files.createTempDirectory("ide-plugins-common-go").toFile();
        tempProject.deleteOnExit();
        FileUtils.copyDirectory(GO_ROOT.resolve("dependency").toFile(), tempProject);
        YarnComponentUpdater yarnComponentUpdater = new YarnComponentUpdater(tempProject.toPath(), null, null);
        yarnComponentUpdater.run("@types/node", "14.14.11");
    }
}
