package com.jfrog.ide.common.updateversion;

import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.file.Path;

public abstract class ComponentUpdater {

    protected final Path projectDir;
    protected final Log logger;
    protected String componentFullName;

    protected ComponentUpdater(Path projectDir, Log logger) {
        this.projectDir = projectDir;
        this.logger = logger;
    }

    protected void run(String componentName, String componentVersion) throws IOException {
        if (!isDriverInstalled()) {
            throw new IOException("Could not scan npm project dependencies, because driver is not in the PATH.");
        }
        this.componentFullName = componentName + getVersionDelimiter() + componentVersion;
    }

    protected abstract boolean isDriverInstalled();

    protected abstract String getVersionDelimiter();

}
