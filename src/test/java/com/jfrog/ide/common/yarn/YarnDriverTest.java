package com.jfrog.ide.common.yarn;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.*;

/**
 * Test the 'why' functionality in YarnDriver.
 */
public class YarnDriverTest {
    private static final Path YARN_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "yarn"));

    enum Project {

        EMPTY("empty"),
        DEPENDENCY("dependency");

        private final Path path;

        Project(String path) {
            this.path = YARN_ROOT.resolve(path);
        }
    }

    private final YarnDriver yarnDriver = new YarnDriver(null);
    private File tempProject;

    @BeforeMethod
    public void setUp(Object[] testArgs) {
        try {
            tempProject = Files.createTempDirectory("ide-plugins-common-yarn").toFile();
            tempProject.deleteOnExit();
            FileUtils.copyDirectory(((Project) testArgs[0]).path.toFile(), tempProject);
        } catch (IOException e) {
            fail(e.getMessage(), e);
        }
    }

    @AfterMethod
    public void tearDown() {
        FileUtils.deleteQuietly(tempProject);
    }

    @DataProvider
    private Object[][] yarnWhyDependencyProvider() {
        return new Object[][]{
                {Project.DEPENDENCY, "progress"},
                {Project.DEPENDENCY, "has-flag"}};
    }

    @Test(dataProvider = "yarnWhyDependencyProvider")
    public void yarnWhyDependencyTest(Project project, String componentName) {
        try {
            JsonNode[] whyResults = yarnDriver.why(tempProject, componentName);
            assertNotNull(whyResults);
            assertTrue(whyResults.length > 0);

            for (JsonNode result : whyResults) {
                assertNotNull(result);
                assertFalse(result.has("problems"), "Unexpected problems in yarn why command result:\n" + result.get("problems"));
            }
        } catch (IOException e) {
            fail("Exception during yarn why command: " + e.getMessage(), e);
        }
    }

    @DataProvider
    private Object[][] yarnWhyEmptyProvider() {
        return new Object[][]{{Project.EMPTY, "component-name"} // Should return an error
        };
    }

    @Test(dataProvider = "yarnWhyEmptyProvider")
    public void yarnWhyEmptyTest(Project project, String componentName) {
        try {
            JsonNode[] whyResults = yarnDriver.why(tempProject, componentName);
            assertNotNull(whyResults);
            assertTrue(whyResults.length > 0);

            assertTrue(whyResults[0].has("problems"), "Yarn why command result should contain problems:\n" + whyResults[0].toString());

        } catch (IOException e) {
            fail("Exception during yarn why command: " + e.getMessage(), e);
        }
    }
}
