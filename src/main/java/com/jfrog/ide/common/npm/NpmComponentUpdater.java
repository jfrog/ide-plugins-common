package com.jfrog.ide.common.npm;

import com.jfrog.ide.common.updateversion.ComponentUpdater;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.npm.NpmDriver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Build npm dependency tree before the Xray scan.
 *
 * @author yahavi
 */
@SuppressWarnings({"unused"})
public class NpmComponentUpdater extends ComponentUpdater {

    public static final String NPM_VERSION_DELIMITER = "@";
    private final NpmDriver npmDriver;

    public NpmComponentUpdater(Path projectDir, Log logger, Map<String, String> env) {
        super(projectDir, logger);
        this.npmDriver = new NpmDriver(env);
    }

    /**
     * Install specific npm component.
     *
     * @param componentName    - The component's name.
     * @param componentVersion - The component's version.
     * @throws IOException in case of I/O error.
     */
    public void run(String componentName, String componentVersion) throws IOException {
        super.run(componentName, componentVersion);
        npmDriver.install(projectDir.toFile(), Collections.singletonList(this.componentFullName), this.logger);
    }

    @Override
    public String getVersionDelimiter() {
        return NPM_VERSION_DELIMITER;
    }

    @Override
    public boolean isDriverInstalled() {
        return npmDriver.isNpmInstalled();
    }
}
