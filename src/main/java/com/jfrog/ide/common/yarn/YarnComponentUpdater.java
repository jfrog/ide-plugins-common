package com.jfrog.ide.common.yarn;

import com.jfrog.ide.common.updateversion.ComponentUpdater;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class YarnComponentUpdater extends ComponentUpdater {

    public static final String YARN_VERSION_DELIMITER = "@";
    private final YarnDriver yarnDriver;

    public YarnComponentUpdater(Path projectDir, Log logger, Map<String, String> env) {
        super(projectDir, logger);
        this.yarnDriver = new YarnDriver(env);
    }

    /**
     * Upgrade specific yarn component.
     *
     * @param componentName    - The component's name.
     * @param componentVersion - The component's version.
     * @throws IOException in case of I/O error.
     */
    public void run(String componentName, String componentVersion) throws IOException {
        super.run(componentName, componentVersion);
        yarnDriver.upgrade(projectDir.toFile(), componentFullName);
    }

    @Override
    public String getVersionDelimiter() {
        return YARN_VERSION_DELIMITER;
    }

    @Override
    public boolean isDriverInstalled() {
        return yarnDriver.isYarnInstalled();
    }
}
