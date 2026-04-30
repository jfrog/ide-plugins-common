package com.jfrog.ide.common.npm;

import com.jfrog.ide.common.updateversion.ComponentUpdater;
import org.jfrog.build.extractor.WslUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.npm.NpmDriver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final CommandExecutor wslExecutor;
    private final boolean isWsl;

    public NpmComponentUpdater(Path projectDir, Log logger, Map<String, String> env) {
        super(projectDir, logger);
        this.isWsl = WslUtils.isWslPath(projectDir);
        if (isWsl) {
            this.npmDriver = null;
            this.wslExecutor = new CommandExecutor("wsl.exe", env);
        } else {
            this.npmDriver = new NpmDriver(env);
            this.wslExecutor = null;
        }
    }

    /**
     * Prefix arguments for {@code wsl.exe} so npm runs in the project directory inside WSL
     * (aligned with {@link NpmTreeBuilder}).
     */
    private List<String> wslNpmInvocationPrefix() {
        String linuxPath = WslUtils.toLinuxPath(projectDir.toString());
        List<String> args = new ArrayList<>();
        args.add("--cd");
        args.add(linuxPath);
        args.add("--exec");
        args.add("npm");
        return args;
    }

    /**
     * Install specific npm component.
     *
     * @param componentName    - The component's name.
     * @param componentVersion - The component's version.
     * @throws IOException in case of I/O error.
     */
    @Override
    public void run(String componentName, String componentVersion) throws IOException {
        super.run(componentName, componentVersion);
        if (isWsl) {
            List<String> args = new ArrayList<>(wslNpmInvocationPrefix());
            args.add("install");
            args.add(this.componentFullName);
            try {
                CommandResults res = wslExecutor.exeCommand(null, args, null, logger);
                if (!res.isOk()) {
                    throw new IOException(StringUtils.defaultString(res.getErr()) + StringUtils.defaultString(res.getRes()));
                }
            } catch (IOException | InterruptedException e) {
                throw new IOException("npm install failed via WSL", e);
            }
        } else {
            npmDriver.install(projectDir.toFile(), Collections.singletonList(this.componentFullName), this.logger);
        }
    }

    @Override
    public String getVersionDelimiter() {
        return NPM_VERSION_DELIMITER;
    }

    @Override
    public boolean isDriverInstalled() {
        if (isWsl) {
            try {
                List<String> args = new ArrayList<>(wslNpmInvocationPrefix());
                args.add("--version");
                CommandResults results = wslExecutor.exeCommand(null, args, null, null);
                return results.isOk() && !StringUtils.isBlank(results.getRes());
            } catch (Exception e) {
                return false;
            }
        }
        return npmDriver.isNpmInstalled();
    }

    @Override
    public String buildTool() {
        return "npm";
    }
}
