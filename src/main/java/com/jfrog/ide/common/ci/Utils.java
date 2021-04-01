package com.jfrog.ide.common.ci;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.text.ParseException;
import java.util.List;
import java.util.Properties;

/**
 * @author yahavi
 **/
public class Utils {

    // The CI may populate 'JFROG_BUILD_RESULTS' in the build info. 'buildInfo.env.JFROG_BUILD_RESULTS' is the key in the build info properties.
    public static final String BUILD_STATUS_PROP = BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + "JFROG_BUILD_RESULTS";

    public static final String BUILD_RET_ERR_FMT = "Couldn't retrieve build information for build '%s/%s'.";
    private static final String NO_VCS_FMT = "Build '%s/%s' does not contain the branch VCS information.";
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

    public static BuildGeneralInfo createBuildGeneralInfo(Build build, Log logger) throws ParseException {
        List<Vcs> vcsList = build.getVcs();
        if (CollectionUtils.isEmpty(vcsList)) {
            logger.debug(String.format(NO_VCS_FMT, build.getName(), build.getNumber()));
            vcsList = Lists.newArrayList(new Vcs());
        }

        Properties buildProperties = build.getProperties();
        return (BuildGeneralInfo) new BuildGeneralInfo()
                .started(build.getStarted())
                .status(buildProperties != null ? buildProperties.getProperty(BUILD_STATUS_PROP, "") : "")
                .vcs(vcsList.get(0))
                .componentId(build.getName() + ":" + build.getNumber())
                .path(build.getUrl());
    }

    /**
     * Create AQL query to download the last 100 build artifacts from Artifactory matched to the input buildsPattern.
     *
     * @param buildsPattern - The build wildcard pattern to filter in Artifactory
     * @return the AQL query.
     */
    public static String createAqlForBuildArtifacts(String buildsPattern) {
        return String.format("items.find({" +
                "\"repo\":\"artifactory-build-info\"," +
                "\"path\":{\"$match\":\"%s\"}}" +
                ").include(\"name\",\"repo\",\"path\",\"created\").sort({\"$desc\":[\"created\"]}).limit(100)", buildsPattern);
    }
}
