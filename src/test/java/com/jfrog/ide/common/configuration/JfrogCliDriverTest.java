package com.jfrog.ide.common.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Test correctness of executing JFrog CLI commands
 *
 * @author tala
 */
public class JfrogCliDriverTest {

    private final JfrogCliDriver jfrogCliDriver = new JfrogCliDriver(null);
    private final String serverIdBase = "ide-plugins-common-test-server-";
    private final String userName = "ide-plugins-common-test-user";
    private final String password = "ide-plugins-common-test-password";
    private final String url = "https://ide/plugins/common/test/";


    private static final SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
    private String testServerId;

    @BeforeMethod
    public void setUp(Object[] testArgs) {
        try {
            testServerId = createServerId();
            String[] serverConfigCmdArgs = {"config", "add", testServerId, "--user=" + userName, "--password=" + password, "--url=" + url, "--interactive=false", "--enc-password=false"};
            jfrogCliDriver.runCommand(Paths.get(".").toAbsolutePath().normalize().toFile(), serverConfigCmdArgs, Collections.emptyList(), null);
        } catch (IOException | InterruptedException e) {
            fail(e.getMessage(), e);
        }
    }

    private String createServerId() {
        return serverIdBase + timeStampFormat.format(System.currentTimeMillis());
    }

    @AfterMethod
    public void cleanUp() {
        try {
            String[] serverConfigCmdArgs = {"config", "remove", testServerId, "--quiet"};
            jfrogCliDriver.runCommand(Paths.get(".").toAbsolutePath().normalize().toFile(), serverConfigCmdArgs, Collections.emptyList(), null);
        } catch (IOException | InterruptedException e) {
            fail(e.getMessage(), e);
        }
    }


    @SuppressWarnings("unused")
    @Test()
    private void cliExportTest() {
        try {
            List<String> args = new ArrayList<>();
            args.add(testServerId);
            JsonNode serverConfig = jfrogCliDriver.getServerConfig(Paths.get(".").toAbsolutePath().normalize().toFile(), args);
            assertEquals(serverConfig.get("user").textValue(), userName);
            assertEquals(serverConfig.get("password").textValue(), password);
            assertEquals(serverConfig.get("url").textValue(), url);
        } catch (
                IOException e) {
            fail(e.getMessage(), e);
        }
    }

}