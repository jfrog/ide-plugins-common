package com.jfrog.ide.common.configuration;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    private final JfrogCliDriver jfrogCliDriver = new JfrogCliDriver(testEnv);
    private final String PASSWORD = "ide-plugins-common-test-password";
    private final String USER_NAME = "ide-plugins-common-test-user";
    private final String URL = "https://ide/plugins/common/test/";
    private String testServerId;
    private File tempDir;


    @BeforeMethod
    public void setUp(Object[] testArgs) {
        try {
            configJfrogCli();
            testServerId = createServerId();
            String[] serverConfigCmdArgs = {"config", "add", testServerId, "--user=" + USER_NAME, "--password=" + PASSWORD, "--url=" + URL, "--interactive=false", "--enc-password=false"};
            jfrogCliDriver.runCommand(tempDir, serverConfigCmdArgs, Collections.emptyList(), null);
        } catch (IOException | InterruptedException e) {
            fail(e.getMessage(), e);
        }
    }

    private void configJfrogCli() {
        try {
            tempDir = Files.createTempDirectory("ide-plugins-common-cli-test").toFile();
            tempDir.deleteOnExit();
        } catch (IOException e) {
            fail(e.getMessage(), e);
        }
        testEnv.put("JFROG_CLI_HOME_DIR", tempDir.getAbsolutePath());
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


    @SuppressWarnings("unused")
    @Test()
    private void cliExportTest() {
        try {
            JfrogCliServerConfig serverConfig = jfrogCliDriver.getServerConfig();
            assertEquals(serverConfig.getUsername(), USER_NAME);
            assertEquals(serverConfig.getPassword(), PASSWORD);
            assertEquals(serverConfig.getUrl(), URL);
            assertEquals(serverConfig.getXrayUrl(), URL + "xray/");
        } catch (IOException e) {
            fail(e.getMessage(), e);
        }
    }

}