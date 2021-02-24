package com.jfrog.ide.common.go;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Scope;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

/**
 * Created by Bar Belity on 16/02/2020.
 */
public class GoTreeBuilderTest {
    private static final Path GO_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "go"));
    private static final Scope NONE_SCOPE = new Scope();
    private static final Log log = new NullLog();

    @Test
    public void testCreateDependencyTree1() {
        Map<String, Integer> expected = new HashMap<String, Integer>() {
            {
                put("github.com/jfrog/gocmd:0.1.12", 5);
                put("github.com/jfrog/gofrog:1.0.5", 1);
                put("github.com/magiconair/properties:1.8.0", 0);
                put("github.com/mattn/go-shellwords:1.0.3", 0);
                put("github.com/mholt/archiver:2.1.0+incompatible", 0);
            }
        };

        try {
            Path projectDir = createProjectDir("project1", GO_ROOT.resolve("project1").toFile());
            GoTreeBuilder treeBuilder = new GoTreeBuilder(projectDir, null, log);
            DependencyTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt, true);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    @Test
    public void testCreateDependencyTree2() {
        Map<String, Integer> expected = new HashMap<String, Integer>() {{
            put("github.com/jfrog/gocmd:0.1.12", 5);
        }};
        try {
            Path projectDir = createProjectDir("project2", GO_ROOT.resolve("project2").toFile());
            GoTreeBuilder treeBuilder = new GoTreeBuilder(projectDir, null, log);
            DependencyTree dt = treeBuilder.buildTree();
            validateDependencyTreeResults(expected, dt, true);
        } catch (IOException ex) {
            fail(ExceptionUtils.getStackTrace(ex));
        }
    }

    private Map<String, List<String>> getAllDependenciesForTest() {
        return new HashMap<String, List<String>>() {
            {
                put("my/pkg/name1", Arrays.asList(
                        "github.com/jfrog/directDep1@v0.1",
                        "github.com/jfrog/directDep2@v0.2",
                        "github.com/jfrog/directDep3@v0.3"));
                put("github.com/jfrog/directDep1@v0.1", Arrays.asList(
                        "github.com/jfrog/indirectDep1-1@v1.1",
                        "github.com/jfrog/indirectDep2-1@v1.2"));
                put("github.com/jfrog/directDep2@v0.2", Collections.singletonList(
                        "github.com/jfrog/indirectDep1-2@v2.1"));
                put("github.com/jfrog/indirectDep1-1@v1.1", Collections.singletonList(
                        "github.com/jfrog/indirectIndirectDep1-1-1@v1.1.1"));
            }
        };
    }

    @Test
    public void testPopulateAllDependenciesMap() {
        // Output of 'go mod graph'.
        String[] dependenciesGraph = new String[]{
                "my/pkg/name1 github.com/jfrog/directDep1@v0.1",
                "my/pkg/name1 github.com/jfrog/directDep2@v0.2",
                "my/pkg/name1 github.com/jfrog/directDep3@v0.3",
                "github.com/jfrog/directDep1@v0.1 github.com/jfrog/indirectDep1-1@v1.1",
                "github.com/jfrog/directDep1@v0.1 github.com/jfrog/indirectDep2-1@v1.2",
                "github.com/jfrog/directDep2@v0.2 github.com/jfrog/indirectDep1-2@v2.1",
                "github.com/jfrog/indirectDep1-1@v1.1 github.com/jfrog/indirectIndirectDep1-1-1@v1.1.1"
        };
        // Run method.
        Map<String, List<String>> expectedAllDependencies = getAllDependenciesForTest();
        Map<String, List<String>> actualAllDependencies = new HashMap<>();
        GoTreeBuilder.populateAllDependenciesMap(dependenciesGraph, actualAllDependencies);
        // Validate result.
        assertEquals(expectedAllDependencies.size(), actualAllDependencies.size());
        boolean notEqual =
                expectedAllDependencies.entrySet()
                        .stream()
                        .anyMatch(e ->
                                !actualAllDependencies.containsKey(e.getKey()) ||
                                        (actualAllDependencies.get(e.getKey())).size() != (e.getValue()).size()
                        );
        if (notEqual) {
            String expected = expectedAllDependencies.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining("\n"));
            String actual = actualAllDependencies.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining("\n"));
            fail(String.format("Expected result mismatches actual result.\nExpected:\n%s\nActual:\n%s", expected, actual));
        }
    }

    @Test
    public void testPopulateDependencyTree() {
        Map<String, Integer> expected = new HashMap<String, Integer>() {
            {
                put("github.com/jfrog/directDep1:0.1", 2);
                put("github.com/jfrog/directDep2:0.2", 1);
                put("github.com/jfrog/directDep3:0.3", 0);
            }
        };
        Map<String, List<String>> allDependencies = getAllDependenciesForTest();
        DependencyTree rootNode = new DependencyTree();
        GoTreeBuilder.populateDependencyTree(rootNode, "my/pkg/name1", allDependencies);
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

    public static Path createProjectDir(String targetDir, File projectOrigin) throws IOException {
        File projectDir = Files.createTempDirectory(targetDir).toFile();
        FileUtils.copyDirectory(projectOrigin, projectDir);
        return projectDir.toPath();
    }
}
