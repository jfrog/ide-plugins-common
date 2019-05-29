package com.jfrog.ide.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.swing.filechooser.FileSystemView;
import java.nio.file.FileSystems;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * @author yahavi
 */
public class ProjectsMapTest {

    @Test
    @SuppressWarnings("OverwrittenKey")
    public void testPut() {
        ProjectsMap projectsMap = new ProjectsMap();

        DependenciesTree first = createDependenciesNode("a", "e");

        projectsMap.put("a", first);

        // Completely Duplicate keys - Expect 1
        projectsMap.put("e", createDependenciesNode("e", "e"));
        projectsMap.put("e", createDependenciesNode("e", "e"));

        // Keys with same project name - Expect 3 ordered by path
        projectsMap.put("d", createDependenciesNode("d", "e"));
        projectsMap.put("d", createDependenciesNode("d", "g"));
        projectsMap.put("d", createDependenciesNode("d", "f"));

        DependenciesTree[] values = projectsMap.values().toArray(new DependenciesTree[]{});
        assertEquals(values.length, 5);

        assertEquals(values[0], first);
        assertEquals(values[1].getUserObject(), createDependenciesNode("d", "e").getUserObject());
        assertEquals(values[2].getUserObject(), createDependenciesNode("d", "f").getUserObject());
        assertEquals(values[3].getUserObject(), createDependenciesNode("d", "g").getUserObject());
        assertEquals(values[4].getUserObject(), createDependenciesNode("e", "e").getUserObject());

    }

    @Test(dataProvider = "keysProvider")
    public void testCompareProjects(String name, String path) {
        ProjectsMap.ProjectKey projectKey = new ProjectsMap.ProjectKey(name, path);
        String wellName = name == null ? "" : name;
        String wellPath = path == null ? "" : path;
        assertEquals(projectKey.projectName, wellName);
        assertEquals(projectKey.projectPath, wellPath);
        assertEquals(0, projectKey.compareTo(new ProjectsMap.ProjectKey(wellName, wellPath)));
        if (StringUtils.isNotBlank(name)) {
            assertNotEquals(0, projectKey.compareTo(new ProjectsMap.ProjectKey("", path)));
        }
        if (StringUtils.isNotBlank(path)) {
            assertNotEquals(0, projectKey.compareTo(new ProjectsMap.ProjectKey(name, "")));
        }
    }

    @DataProvider
    private Object[][] keysProvider() {
        return new Object[][]{
                {"thor", "asgard"},
                {"loki_1", ""},
                {"", "midgard_1"},
                {"", ""},
                {"loki_1", null},
                {null, "midgard_1"},
                {null, null},
                {"odin", FileSystems.getDefault().getPath(".").toAbsolutePath().toString()},
                {"odin", FileSystemView.getFileSystemView().getRoots()[0].toString()}
        };
    }

    private DependenciesTree createDependenciesNode(String name, String path) {
        DependenciesTree dependenciesTree = new DependenciesTree(name);
        dependenciesTree.setGeneralInfo(new GeneralInfo().path(path));
        return dependenciesTree;
    }
}