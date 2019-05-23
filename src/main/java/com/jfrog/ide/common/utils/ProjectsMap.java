package com.jfrog.ide.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public class ProjectsMap extends TreeMap<ProjectsMap.ProjectKey, DependenciesTree> {
    private static final long serialVersionUID = 1L;

    /**
     * Put a project in the map.
     *
     * @param projectName      - The project name.
     * @param dependenciesTree - The dependencies tree.
     */
    public void put(String projectName, DependenciesTree dependenciesTree) {
        super.put(createKey(projectName, dependenciesTree.getGeneralInfo()), dependenciesTree);
    }

    @SuppressWarnings("WeakerAccess")
    public static ProjectKey createKey(String projectName, GeneralInfo generalInfo) {
        return new ProjectKey(projectName, generalInfo.getPath());
    }

    /**
     * Comparable key. The keys are sorted by name and path.
     */
    public static class ProjectKey implements Comparable<ProjectKey> {
        private String projectName;
        private String projectPath;

        ProjectKey(String projectName, String projectPath) {
            this.projectName = projectName;
            this.projectPath = projectPath;
        }

        @Override
        public int compareTo(@Nonnull ProjectKey other) {
            return Comparator
                    .comparing((ProjectsMap.ProjectKey key) -> StringUtils.compare(key.projectName, other.projectName))
                    .thenComparing(key -> StringUtils.compare(key.projectPath, other.projectPath)).compare(this, other);
        }

    }
}
