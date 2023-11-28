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
 * Test functionalities of YarnDriver.
 */
public class YarnDriverTest {
    private static final Path YARN_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "yarn"));

    public enum Project {
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
                {Project.DEPENDENCY, "progress", true},
                {Project.DEPENDENCY, "has-flag", true},
                {Project.EMPTY, "component-name", false}
        };
    }

    @Test(dataProvider = "yarnWhyDependencyProvider")
    public void yarnWhyDependencyTest(@SuppressWarnings("unused") Project project, String componentName, boolean resultsExist) throws IOException {
        JsonNode[] whyResults = yarnDriver.why(tempProject, componentName);
        assertNotNull(whyResults);
        assertEquals(whyResults.length > 0, resultsExist);
    }
}
