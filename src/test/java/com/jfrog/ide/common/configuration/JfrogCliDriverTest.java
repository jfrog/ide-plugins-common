package com.jfrog.ide.common.configuration;

import org.apache.commons.lang3.SystemUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private final String URL = "https://ide/plugins/common/test/";
    private String testServerId;
    private File tempDir;


    @SuppressWarnings("unused")
    @Test()
    private void cliExportTest() {
        try {
            JfrogCliServerConfig serverConfig = jfrogCliDriver.getServerConfig(tempDir, Collections.emptyList());
            assertEquals(serverConfig.getUsername(), USER_NAME);
            assertEquals(serverConfig.getPassword(), PASSWORD);
            assertEquals(serverConfig.getUrl(), URL);
            assertEquals(serverConfig.getXrayUrl(), URL + "xray/");
        } catch (IOException e) {
            fail(e.getMessage(), e);
        }
    }

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
            getCli(tempDir);
        } catch (IOException e) {
            fail(e.getMessage(), e);
        }
        testEnv.put("JFROG_CLI_HOME_DIR", tempDir.getAbsolutePath());
        jfrogCliDriver = new JfrogCliDriver(testEnv, tempDir.getAbsolutePath() + File.separator);

    }

    private void getCli(File execDir) throws IOException {
        List<String> args = new ArrayList();
        if (SystemUtils.IS_OS_WINDOWS) {
            args.addAll(0, Arrays.asList("cmd", "/c", "curl -XGET \"https://releases.jfrog.io/artifactory/jfrog-cli/v2/[RELEASE]/jfrog-cli-windows-amd64/jfrog.exe\" -L -k -g", "&& chmod u+x jfrog.exe"));
        } else {
            args = new ArrayList<>() {{
                add("/bin/sh");
                add("-c");
                add("curl -fL https://getcli.jfrog.io | bash -s v2\n");
            }};
        }
        Process process = Runtime.getRuntime().exec(args.toArray(new String[0]), new String[0], execDir);
        process.getOutputStream().close();
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