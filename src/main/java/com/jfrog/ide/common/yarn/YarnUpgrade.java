package com.jfrog.ide.common.yarn;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Build yarn dependency tree before the Xray scan.
 *
 * @author tala
 */
public class YarnUpgrade {

    public static final String YARN_VERSION_DELIMITER = "@";
    private final YarnDriver yarnDriver;
    private final Path projectDir;

    public YarnUpgrade(Path projectDir, Map<String, String> env) {
        this.projectDir = projectDir;
        this.yarnDriver = new YarnDriver(env);
    }

    public void run(String componentName, String componentVersion) throws IOException {
        if (!yarnDriver.isYarnInstalled()) {
            throw new IOException("Could not scan yarn project dependencies, because yarn CLI is not in the PATH.");
        }
        String componentFullName = componentName + YARN_VERSION_DELIMITER + componentVersion;
        yarnDriver.upgrade(projectDir.toFile(), componentFullName);
    }
}
