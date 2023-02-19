package com.jfrog.ide.common.npm;

import com.google.common.collect.Lists;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.npm.NpmDriver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Build npm dependency tree before the Xray scan.
 *
 * @author yahavi
 */
@SuppressWarnings({"unused"})
public class NpmInstall {

    public static final String NPM_VERSION_DELIMITER = "@";
    private final NpmDriver npmDriver;
    private final Path projectDir;

    public NpmInstall(Path projectDir, Map<String, String> env) {
        this.projectDir = projectDir;
        this.npmDriver = new NpmDriver(env);
    }

    /**
     * Install specific npm component.
     *
     * @param componentName    - The component's name.
     * @param componentVersion - The component's version.
     * @param logger           - The logger
     * @throws IOException in case of I/O error.
     */
    public void run(String componentName, String componentVersion, Log logger) throws IOException {
        if (!npmDriver.isNpmInstalled()) {
            throw new IOException("Could not scan npm project dependencies, because npm CLI is not in the PATH.");
        }
        String componentFullName = componentName + NPM_VERSION_DELIMITER + componentVersion;
        npmDriver.install(projectDir.toFile(), Lists.newArrayList("install", componentFullName), logger);
    }
}
