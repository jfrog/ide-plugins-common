package com.jfrog.ide.common.go;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Scope;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static com.jfrog.ide.common.go.GoTreeBuilder.MIN_GO_VERSION;
import static com.jfrog.ide.common.go.GoTreeBuilder.parseGoVersion;
import static org.testng.Assert.*;

/**
 * Created by Bar Belity on 16/02/2020.
 */
public class GoTreeBuilderTest {
    private static final Path GO_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "go"));
    private static final Scope NONE_SCOPE = new Scope();
    private static final Log log = new NullLog();

    /**
     * The project is with dependencies, but without go.sum
     */
    @Test
    public void testCreateDependencyTree1() {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/jfrog/jfrog-cli-core:1.9.0", 11);
            put("github.com/jfrog/jfrog-client-go:0.26.1", 9);
        }};

        try {
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, GO_ROOT.resolve("project1"), null, log);
            DependencyTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * The project is with dependencies and go.sum, but with checksum mismatch on github.com/dsnet/compress
     */
    @Test
    public void testCreateDependencyTree2() {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/jfrog/gocmd:0.1.12", 2);
        }};
        try {
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, GO_ROOT.resolve("project2"), null, log);
            DependencyTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * The project is with dependencies and go.sum, but contains a relative path in go.mod
     * The submodule is a subdirectory of the project directory.
     */
    @Test
    public void testCreateDependencyTree3() {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/test/subproject:0.0.0-00010101000000-000000000000", 1);
        }};
        try {
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, GO_ROOT.resolve("project3"), null, log);
            DependencyTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * The project is with dependencies and go.sum, but contains a relative path in go.mod.
     * The submodule is a sibling of the project directory.
     */
    @Test
    public void testCreateDependencyTree4() {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/test/subproject:0.0.0-00010101000000-000000000000", 1);
        }};
        try {
            GoTreeBuilder treeBuilder = new GoTreeBuilder(null, GO_ROOT.resolve("project4"), null, log);
            DependencyTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    private void validateDependencyTreeResults(Map<String, Integer> expected, DependencyTree actual) {
        Vector<DependencyTree> children = actual.getChildren();
        assertEquals(children.size(), expected.size());
        for (DependencyTree current : children) {
            assertTrue(expected.containsKey(current.toString()));
            assertEquals(current.getChildren().size(), expected.get(current.toString()).intValue());
            assertTrue(current.getScopes().contains(NONE_SCOPE));
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
        assertEquals(expectedVersion, parseGoVersion(commandResults, new NullLog()).toString());
    }
}
