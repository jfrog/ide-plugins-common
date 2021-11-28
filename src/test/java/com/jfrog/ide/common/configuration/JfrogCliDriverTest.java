package com.jfrog.ide.common.configuration;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.SystemUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Test correctness of executing JFrog CLI commands
 *
 * @author tala
 */
public class JfrogCliDriverTest {

    private final SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
    private final Map<String, String> testEnv = new HashMap<>();
    private JfrogCliDriver jfrogCliDriver;
    private final String PASSWORD = "ide-plugins-common-test-password";
    private final String USER_NAME = "ide-plugins-common-test-user";
    private final String SERVER_URL = "https://ide/plugins/common/test/";
    private String testServerId;
    private File tempDir;

    @SuppressWarnings("unused")
    @Test()
    private void cliExportTest() {
        try {
            JfrogCliServerConfig serverConfig = jfrogCliDriver.getServerConfig(tempDir, Collections.emptyList());
            assertEquals(serverConfig.getUsername(), USER_NAME);
            assertEquals(serverConfig.getPassword(), PASSWORD);
            assertEquals(serverConfig.getUrl(), SERVER_URL);
            assertEquals(serverConfig.getXrayUrl(), SERVER_URL + "xray/");
        } catch (IOException e) {
            fail(e.getMessage(), e);
        }
    }

    @BeforeMethod
    public void setUp(Object[] testArgs) {
        try {
            configJfrogCli();
            testServerId = createServerId();
            String[] serverConfigCmdArgs = {"config", "add", testServerId, "--user=" + USER_NAME, "--password=" + PASSWORD, "--url=" + SERVER_URL, "--interactive=false", "--enc-password=false"};
            jfrogCliDriver.runCommand(tempDir, serverConfigCmdArgs, Collections.emptyList(), null);
        } catch (IOException | InterruptedException e) {
            fail(e.getMessage(), e);
        }
    }

    private void configJfrogCli() {
        try {
            tempDir = Files.createTempDirectory("ide-plugins-common-cli-test").toFile();
            tempDir.deleteOnExit();
            getCli(tempDir);
        } catch (IOException | InterruptedException e) {
            fail(e.getMessage(), e);
        }
        testEnv.put("JFROG_CLI_HOME_DIR", tempDir.getAbsolutePath());
        jfrogCliDriver = new JfrogCliDriver(testEnv, tempDir.getAbsolutePath() + File.separator);

    }

    private void getCli(File execDir) throws IOException, InterruptedException {
        List<String> args;
        if (SystemUtils.IS_OS_WINDOWS) {
            try (InputStream in = new URL("https://releases.jfrog.io/artifactory/jfrog-cli/v2-jf/[RELEASE]/jfrog-cli-windows-amd64/jf.exe").openStream()) {
                Files.copy(in, Paths.get(tempDir.getAbsolutePath() + "\\jf.exe"), StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }

        args = new ArrayList<>() {{
            add("/bin/sh");
            add("-c");
            add("curl -fL https://getcli.jfrog.io/v2-jf | sh");
        }};

        Process process = Runtime.getRuntime().exec(args.toArray(new String[0]), new String[0], execDir);
        process.getOutputStream().close();
        process.waitFor();
    }


    private String createServerId() {
        return "ide-plugins-common-test-server-" + timeStampFormat.format(System.currentTimeMillis());
    }

    @AfterMethod
    public void cleanUp() {
        try {
            String[] serverConfigCmdArgs = {"config", "remove", testServerId, "--quiet"};
            jfrogCliDriver.runCommand(tempDir, serverConfigCmdArgs, Collections.emptyList(), null);
        } catch (IOException | InterruptedException e) {
            fail(e.getMessage(), e);
        }
    }

}