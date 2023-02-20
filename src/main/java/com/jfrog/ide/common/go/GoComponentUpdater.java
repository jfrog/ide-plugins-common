package com.jfrog.ide.common.go;

import com.jfrog.ide.common.updateversion.ComponentUpdater;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.go.GoDriver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Created by Michael Sverdlov
 */

@SuppressWarnings({"unused"})
public class GoComponentUpdater extends ComponentUpdater {
    public static final String GO_VERSION_DELIMITER = "@v";
    private final GoDriver goDriver;

    public GoComponentUpdater(Path projectDir, Log logger, Map<String, String> env, String executablePath) {
        super(projectDir, logger);
        this.goDriver = new GoDriver(executablePath, env, projectDir.toFile(), logger);
    }

    /**
     * Get specific go component.
     *
     * @param componentName    - The component's name.
     * @param componentVersion - The component's version.
     * @throws IOException in case of I/O error.
     */
    public void run(String componentName, String componentVersion) throws IOException {
        super.run(componentName, componentVersion);
        goDriver.get(this.componentFullName, false);
    }

    @Override
    protected boolean isDriverInstalled() {
        return goDriver.isInstalled();
    }

    @Override
    protected String getVersionDelimiter() {
        return GO_VERSION_DELIMITER;
    }
}
