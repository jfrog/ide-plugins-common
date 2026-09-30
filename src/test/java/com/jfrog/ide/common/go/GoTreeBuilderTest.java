package com.jfrog.ide.common.go;

import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.Version;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.go.GoDriver;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
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
     * The projects replace a dependency with a relative path that contains a space, shell characters or a percent sign.
     */
    @Test(dataProvider = "replacePathProjectsProvider")
    public void testCreateDependencyTreeReplacePath(String projectName) throws IOException {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/test/subproject:0.0.0-00010101000000-000000000000", 1);
        }};
        Path projectDir = GO_ROOT.resolve(projectName);
        GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
        DepTree dt = treeBuilder.buildTree();
        validateDependencyTreeResults(expected, dt);
    }

    /**
     * The project replaces a dependency with an absolute path.
     */
    @Test
    public void testCreateDependencyTreeAbsoluteReplacePath() throws IOException {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/test/subproject:0.0.0-00010101000000-000000000000", 1);
        }};
        Path projectDir = Files.createTempDirectory("projectAbsoluteReplacePath");
        try {
            Files.copy(GO_ROOT.resolve("project4").resolve("main.go"), projectDir.resolve("main.go"));
            Files.copy(GO_ROOT.resolve("project4").resolve("go.sum"), projectDir.resolve("go.sum"));
            String replacementPath = GO_ROOT.resolve("subproject").toString().replace('\\', '/');
            Files.writeString(projectDir.resolve("go.mod"), "module projectAbsoluteReplacePath\n\n" +
                    "require github.com/test/subproject v0.0.0-00010101000000-000000000000\n\n" +
                    "replace github.com/test/subproject => \"" + replacementPath + "\"\n\n" +
                    "go 1.13\n");
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
            DepTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt);
        } finally {
            FileUtils.deleteDirectory(projectDir.toFile());
        }
    }

    /**
     * A replaced module name that tries to run a command, through "cmd /c" on Windows and "/bin/sh -c" elsewhere.
     */
    @Test
    public void testCreateDependencyTreeDoesNotRunReplacedModuleName() throws IOException {
        Path projectDir = Files.createTempDirectory("projectInjectedModuleName");
        Path marker = projectDir.resolve("injected");
        try {
            String markerInGoMod = marker.toString().replace("\\", "\\\\");
            String replacedModule = "ex\\\"&type nul>" + markerInGoMod + "&\\\"$(touch " + markerInGoMod + ").com/x";
            Files.writeString(projectDir.resolve("go.mod"), "module projectInjectedModuleName\n\n" +
                    "replace \"" + replacedModule + "\" => \"../subproject\"\n\n" +
                    "go 1.13\n");
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
            try {
                treeBuilder.buildTree();
            } catch (IOException ignored) {
            }
            assertFalse(Files.exists(marker));
        } finally {
            FileUtils.deleteDirectory(projectDir.toFile());
        }
    }

    @DataProvider
    private Object[][] replacePathProjectsProvider() {
        return new Object[][]{
                {"projectReplaceWithSpace"},
                {"projectReplaceWithShellChars"},
                {"projectReplaceWithPercent"},
        };
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
        Path projectDir = GO_ROOT.resolve("project6");
        GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
        assertThrows(IOException.class, treeBuilder::buildTree);
    }

    @Test
    public void testCreateDependencyTreeEmbedProject() throws IOException {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/jfrog/jfrog-cli-core:1.9.0", 10);
            put("github.com/jfrog/jfrog-client-go:0.26.1", 8);
        }};
        Path projectDir = GO_ROOT.resolve("embedProject");
        GoTreeBuilder treeBuilder = new GoTreeBuilder(null, projectDir, projectDir.resolve("go.mod").toString(), null, log);
        DepTree depTree = treeBuilder.buildTree();
        validateDependencyTreeResults(expected, depTree);
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
