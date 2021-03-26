package com.jfrog.ide.common.persistency;

import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yahavi
 **/
public class Utils {

    public static DependencyTree artifactToDependencyTree(Artifact artifact) {
        return null;
    }

    public static Artifact dependencyNodeToArtifact(DependencyTree node) {
        return dependencyNodeToArtifact(node, false);
    }

    public static Artifact dependencyNodeToArtifact(DependencyTree node, boolean isArtifact) {
        Set<String> children = node.getChildren().stream()
                .map(DependencyTree::getGeneralInfo)
                .map(GeneralInfo::getComponentId)
                .collect(Collectors.toSet());
        return new Artifact(node.getGeneralInfo().artifact(isArtifact), node.getIssues(), node.getLicenses(), node.getScopes(), children);
    }
}
