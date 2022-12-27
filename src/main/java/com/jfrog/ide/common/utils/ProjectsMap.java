package com.jfrog.ide.common.utils;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * Maps project's key to project's dependency tree. The project's key is a unique key based on the name and the path.
 * Used in IDEs tree browsers to distinct between different projects.
 *
 * @author yahavi
 */
@SuppressWarnings("unused")
public class ProjectsMap extends TreeMap<ProjectsMap.ProjectKey, DependencyTree> {
    private static final long serialVersionUID = 1L;

    /**
     * Put a project in the map.
     *
     * @param projectName    - The project name.
     * @param dependencyTree - The dependency tree.
     */
    public void put(String projectName, DependencyTree dependencyTree) {
        super.put(createKey(projectName, dependencyTree.getGeneralInfo()), dependencyTree);
    }

    @SuppressWarnings("WeakerAccess")
    public static ProjectKey createKey(String projectName, GeneralInfo generalInfo) {
        return createKey(projectName, generalInfo.getPath());
    }

    public static ProjectKey createKey(String projectName, String path) {
        return new ProjectKey(projectName, path);
    }

    /**
     * Comparable key. The keys are sorted by name and path.
     */
    public static class ProjectKey implements Comparable<ProjectKey> {
        String projectName;
        String projectPath;

        ProjectKey(String projectName, String projectPath) {
            this.projectName = StringUtils.defaultIfBlank(projectName, "");
            this.projectPath = StringUtils.defaultIfBlank(projectPath, "");
        }

        @Override
        public int compareTo(@NonNull ProjectKey other) {
            return Comparator.comparing(ProjectKey::getProjectName)
                    .thenComparing(ProjectKey::getProjectPath)
                    .compare(this, other);
        }

        private String getProjectName() {
            return projectName;
        }

        private String getProjectPath() {
            return projectPath;
        }
    }
}
