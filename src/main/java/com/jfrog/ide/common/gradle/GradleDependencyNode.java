package com.jfrog.ide.common.gradle;

import java.util.List;

/**
 * @author yahavi
 **/
public class GradleDependencyNode {
    private List<GradleDependencyNode> dependencies;
    private List<String> scopes;
    private String artifactId;
    private String groupId;
    private String version;

    public List<GradleDependencyNode> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<GradleDependencyNode> dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
