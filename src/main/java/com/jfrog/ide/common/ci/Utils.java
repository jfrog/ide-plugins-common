package com.jfrog.ide.common.ci;

import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

/**
 * @author yahavi
 **/
public class Utils {

    public static final String DEPENDENCIES_NODE = "dependencies";
    public static final String ARTIFACTS_NODE = "artifacts";

    public static DependencyTree createDependenciesNode(String moduleId) {
        DependencyTree artifactsNode = new DependencyTree(DEPENDENCIES_NODE);
        GeneralInfo artifactsGeneralInfo = new GeneralInfo().componentId(moduleId).pkgType("Module dependencies");
        artifactsNode.setGeneralInfo(artifactsGeneralInfo);
        return artifactsNode;
    }

    public static DependencyTree createArtifactsNode(String moduleId) {
        DependencyTree artifactsNode = new DependencyTree(ARTIFACTS_NODE);
        GeneralInfo artifactsGeneralInfo = new GeneralInfo().componentId(moduleId).pkgType("Module artifacts");
        artifactsNode.setGeneralInfo(artifactsGeneralInfo);
        return artifactsNode;
    }
}
