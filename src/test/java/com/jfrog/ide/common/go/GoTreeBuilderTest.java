package com.jfrog.ide.common.go;

import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.Version;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.go.GoDriver;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.jfrog.ide.common.go.GoTreeBuilder.MIN_GO_VERSION;
import static com.jfrog.ide.common.go.GoTreeBuilder.parseGoVersion;
import static org.testng.Assert.*;

/**
 * Created by Bar Belity on 16/02/2020.
 */
public class GoTreeBuilderTest {
    private static final Path GO_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "go"));
    private static final Log log = new NullLog();
    private static final GoDriver goDriver = new GoDriver(null, null, null, log);

    /**
     * The project is with dependencies, but without a "go.sum"
     */
    @Test
    public void testCreateDependencyTree1() {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/jfrog/jfrog-cli-core:1.9.0", 11);
            put("github.com/jfrog/jfrog-client-go:0.26.1", 9);
        }};

        try {
            Path projectDir = GO_ROOT.resolve("project1");
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
            DepTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * The project is with dependencies and with "go.sum", but with checksum mismatch on github.com/dsnet/compress
     */
    @Test
    public void testCreateDependencyTree2() {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/jfrog/gocmd:0.1.12", 2);
        }};
        try {
            Path projectDir = GO_ROOT.resolve("project2");
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
            DepTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * The project is with dependencies and with "go.sum", but contains a relative path in the "go.mod".
     * The submodule is a subdirectory of the project directory.
     */
    @Test
    public void testCreateDependencyTree3() {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/test/subproject:0.0.0-00010101000000-000000000000", 1);
        }};
        try {
            Path projectDir = GO_ROOT.resolve("project3");
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
            DepTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * The project is with dependencies and with "go.sum", but contains a relative path in go.mod.
     * The submodule is a sibling of the project directory.
     */
    @Test
    public void testCreateDependencyTree4() {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/test/subproject:0.0.0-00010101000000-000000000000", 1);
        }};
        try {
            Path projectDir = GO_ROOT.resolve("project4");
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
            DepTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * The project has no dependencies.
     */
    @Test
    public void testCreateDependencyTree5() {
        Map<String, Integer> expected = new HashMap<>();
        try {
            Path projectDir = GO_ROOT.resolve("project5");
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
            DepTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * The project has no source code (it doesn't contain any packages).
     */
    @Test
    public void testCreateDependencyTree6() {
        try {
            Path projectDir = GO_ROOT.resolve("project6");
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
            treeBuilder.buildTree();
            fail("Expected an IOException being thrown");
        } catch (IOException e) {
            // This exception is expected being thrown
        } catch (Throwable e) {
            fail(ExceptionUtils.getStackTrace(e));
        }
    }

    @Test
    public void testCreateDependencyTreeEmbedProject() throws IOException {
        Path projectDir = GO_ROOT.resolve("embedProject");
        GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
        treeBuilder.buildTree();
    }

    private void validateDependencyTreeResults(Map<String, Integer> expected, DepTree actual) throws IOException {
        addExpectedVersionNode(expected);
        Set<String> children = actual.getRootNode().getChildren();
        assertEquals(children.size(), expected.size());
        for (String childId : children) {
            DepTreeNode childNode = actual.nodes().get(childId);
            assertNotNull(childNode);
            assertEquals(childNode.getChildren().size(), expected.get(childId).intValue());
        }
    }

    @DataProvider
    private Object[][] goVersionProvider() {
        return new Object[][]{
                // Below 1.16
                {"go version go1.9 darwin/amd64", "1.9"},
                {"go version go1.15 darwin/amd64", "1.15"},
                {"go version go1.15.4 darwin/amd64", "1.15.4"},

                // Error values
                {"go version 1.15 darwin/amd64", MIN_GO_VERSION.toString()},
                {"go version 1.17 darwin/amd64", MIN_GO_VERSION.toString()},
                {"1.17", MIN_GO_VERSION.toString()},
                {"1.15", MIN_GO_VERSION.toString()},
        };
    }

    @Test(dataProvider = "goVersionProvider")
    public void testParseGoVersion(String versionOutput, String expectedVersion) {
        CommandResults commandResults = new CommandResults();
        commandResults.setRes(versionOutput);
        assertEquals(expectedVersion, parseGoVersion(commandResults, log).toString());
    }

    private void addExpectedVersionNode(Map<String, Integer> expected) throws IOException {
        CommandResults versionRes = goDriver.version(false);
        Version goVersion = parseGoVersion(versionRes, log);
        expected.put("github.com/golang/go:" + goVersion, 0);
    }
}
