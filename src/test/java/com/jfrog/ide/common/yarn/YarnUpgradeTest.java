package com.jfrog.ide.common.yarn;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.fail;

/**
 * Created by MichaelS
 */
public class YarnUpgradeTest {
    private static final Path GO_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "yarn"));

    @Test
    public void testYarnUpgrade() {
        try {
            File tempProject = Files.createTempDirectory("ide-plugins-common-go").toFile();
            tempProject.deleteOnExit();
            FileUtils.copyDirectory(GO_ROOT.resolve("dependency").toFile(), tempProject);
            YarnUpgrade yarnUpgrade = new YarnUpgrade(tempProject.toPath(), null);
            yarnUpgrade.run("@types/node", "14.14.11");
        } catch (Exception ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }
}
