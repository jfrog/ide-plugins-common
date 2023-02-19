package com.jfrog.ide.common.go;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.go.GoDriver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Created by Michael Sverdlov
 */

@SuppressWarnings({"unused"})
public class GoGet {
    public static final String GO_GET_CMD_FORMAT = "get %s";
    public static final String GO_VERSION_DELIMITER = "@v";
    private final Map<String, String> env;
    private final String executablePath;
    private final Path projectDir;
    private final Log logger;

    public GoGet(String executablePath, Path projectDir, Map<String, String> env, Log logger) {
        this.executablePath = executablePath;
        this.projectDir = projectDir;
        this.logger = logger;
        this.env = env;
    }

    /**
     * Get specific go component.
     *
     * @param componentName    - The component's name.
     * @param componentVersion - The component's version.
     * @throws IOException in case of I/O error.
     */
    public void run(String componentName, String componentVersion) throws IOException {
        GoDriver goDriver = new GoDriver(executablePath, env, projectDir.toFile(), logger);
        if (!goDriver.isInstalled()) {
            throw new IOException("Could not scan go project dependencies, because go CLI is not in the PATH.");
        }
        String componentFullName = componentName + GO_VERSION_DELIMITER + componentVersion;
        goDriver.runCmd("get " + componentFullName, true);
    }
}
