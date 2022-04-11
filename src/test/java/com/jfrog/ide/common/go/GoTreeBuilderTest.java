package com.jfrog.ide.common.go;

import com.google.common.collect.Sets;
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
import java.util.*;

import static com.jfrog.ide.common.go.GoTreeBuilder.isBelow16;
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
            validateDependencyTreeResults(expected, dt, true);
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
            validateDependencyTreeResults(expected, dt, true);
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
            validateDependencyTreeResults(expected, dt, true);
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
            validateDependencyTreeResults(expected, dt, true);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    private Map<String, List<String>> getAllDependenciesForTest() {
        return new HashMap<>() {{
            put("my/pkg/name1", Arrays.asList(
                    "github.com/jfrog/directDep1@v0.1",
                    "github.com/jfrog/directDep2@v0.2",
                    "github.com/jfrog/directDep3@v0.3"));
            put("github.com/jfrog/directDep1@v0.1", List.of(
                    "github.com/jfrog/indirectDep1-1@v1.1",
                    "github.com/jfrog/indirectDep2-1@v1.3"));
            put("github.com/jfrog/directDep2@v0.2", Collections.singletonList(
                    "github.com/jfrog/indirectDep1-2@v2.1"));
            put("github.com/jfrog/indirectDep1-1@v1.1", Collections.singletonList(
                    "github.com/jfrog/indirectIndirectDep1-1-1@v1.1.1"));
        }};
    }

    @Test
    public void testPopulateAllDependenciesMap() {
        // Output of 'go mod graph'.
        String[] dependenciesGraph = new String[]{
                "my/pkg/name1 github.com/jfrog/directDep1@v0.1",
                "my/pkg/name1 github.com/jfrog/directDep2@v0.2",
                "my/pkg/name1 github.com/jfrog/directDep3@v0.3",
                "github.com/jfrog/directDep1@v0.1 github.com/jfrog/indirectDep1-1@v1.1",
                // github.com/jfrog/indirectDep2-1@v1.2 does not appear in the used modules set:
                "github.com/jfrog/directDep1@v0.1 github.com/jfrog/indirectDep2-1@v1.2",
                "github.com/jfrog/directDep1@v0.1 github.com/jfrog/indirectDep2-1@v1.3",
                "github.com/jfrog/directDep2@v0.2 github.com/jfrog/indirectDep1-2@v2.1",
                "github.com/jfrog/indirectDep1-1@v1.1 github.com/jfrog/indirectIndirectDep1-1-1@v1.1.1"
        };
        Set<String> usedModules = Sets.newHashSet("github.com/jfrog/directDep1@v0.1",
                "github.com/jfrog/directDep2@v0.2",
                "github.com/jfrog/directDep3@v0.3",
                "github.com/jfrog/indirectDep1-1@v1.1",
                "github.com/jfrog/indirectDep2-1@v1.3",
                "github.com/jfrog/indirectDep1-2@v2.1",
                "github.com/jfrog/indirectIndirectDep1-1-1@v1.1.1");
        // Run method.
        Map<String, List<String>> expectedAllDependencies = getAllDependenciesForTest();
        Map<String, List<String>> actualAllDependencies = new HashMap<>();
        GoTreeBuilder.populateAllDependenciesMap(dependenciesGraph, actualAllDependencies, usedModules);
        // Validate result.
        assertEquals(expectedAllDependencies, actualAllDependencies);
    }

    @Test
    public void testPopulateDependencyTree() {
        Map<String, Integer> expected = new HashMap<>() {{
            put("github.com/jfrog/directDep1:0.1", 2);
            put("github.com/jfrog/directDep2:0.2", 1);
            put("github.com/jfrog/directDep3:0.3", 0);
        }};
        Map<String, List<String>> allDependencies = getAllDependenciesForTest();
        DependencyTree rootNode = new DependencyTree();
        GoTreeBuilder treeBuilder = new GoTreeBuilder(null, Paths.get(""), null, log);
        treeBuilder.populateDependencyTree(rootNode, "my/pkg/name1", allDependencies);
        validateDependencyTreeResults(expected, rootNode, false);
    }

    private void validateDependencyTreeResults(Map<String, Integer> expected, DependencyTree actual, boolean checkScope) {
        Vector<DependencyTree> children = actual.getChildren();
        assertEquals(children.size(), expected.size());
        for (DependencyTree current : children) {
            assertTrue(expected.containsKey(current.toString()));
            assertEquals(current.getChildren().size(), expected.get(current.toString()).intValue());
            if (checkScope) {
                assertTrue(current.getScopes().contains(NONE_SCOPE));
            }
        }
    }

    @DataProvider
    private Object[][] goVersionProvider() {
        return new Object[][]{
                // Below 1.16
                {"go version go1.9 darwin/amd64", true},
                {"go version go1.15 darwin/amd64", true},
                {"go version go1.15.4 darwin/amd64", true},

                // Above 1.16
                {"go version go1.16 darwin/amd64", false},
                {"go version go1.16.1 darwin/amd64", false},
                {"go version go1.17.7 darwin/amd64", false},

                // Error values
                {"go version 1.15 darwin/amd64", false},
                {"go version 1.17 darwin/amd64", false},
                {"1.17", false},
                {"1.15", false},
        };
    }

    @Test(dataProvider = "goVersionProvider")
    public void testIsBelow16(String versionOutput, boolean expectedBelow) {
        CommandResults commandResults = new CommandResults();
        commandResults.setRes(versionOutput);
        assertEquals(expectedBelow, isBelow16(commandResults, new NullLog()));
    }
}
