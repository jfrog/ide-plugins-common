package com.jfrog.ide.common.ci;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.jfrog.xray.client.services.details.DetailsResponse;
import com.jfrog.xray.client.services.summary.General;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.*;
import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static com.jfrog.ide.common.ci.Utils.*;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author yahavi
 **/
public class CiDependencyTree extends DependencyTree implements ProducerConsumerItem {
    public static final String BUILD_STATUS_PROP = BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + "JFROG_BUILD_RESULTS";

    private final Build build;

    public CiDependencyTree(Build build) {
        this.build = build;
    }

    public void createBuildDependencyTree() throws ParseException, IOException {
        List<Vcs> vcsList = build.getVcs();
        if (CollectionUtils.isEmpty(vcsList)) {
            throw new IOException("Build '" + build.getName() + "/" + build.getNumber() + "' does not contain the branch VCS information");
        }

        Properties buildProperties = build.getProperties();
        GeneralInfo buildGeneralInfo = new BuildGeneralInfo()
                .started(build.getStarted())
                .status(buildProperties != null ? buildProperties.getProperty(BUILD_STATUS_PROP) : "")
                .vcs(vcsList.get(0))
                .componentId(build.getName() + ":" + build.getNumber())
                .path(build.getUrl());
        setUserObject(build.getName() + "/" + build.getNumber());
        setScopes(Sets.newHashSet(new Scope()));
        setGeneralInfo(buildGeneralInfo);
        if (CollectionUtils.isNotEmpty(build.getModules())) {
            populateModulesDependencyTree(this, build);
        }
    }

    private void populateModulesDependencyTree(DependencyTree buildDependencyTree, Build build) {
        for (Module module : build.getModules()) {
            GeneralInfo moduleGeneralInfo = new GeneralInfo()
                    .componentId(module.getId())
                    .pkgType(module.getType());
            DependencyTree moduleNode = new DependencyTree(module.getId());
            moduleNode.setGeneralInfo(moduleGeneralInfo);

            // Populate artifacts
            DependencyTree artifactsNode = createArtifactsNode(module.getId());
            moduleNode.add(artifactsNode);
            if (CollectionUtils.isNotEmpty(module.getArtifacts())) {
                populateArtifacts(artifactsNode, module);
            }

            // Populate dependencies
            DependencyTree dependenciesNode = createDependenciesNode(module.getId());
            moduleNode.add(dependenciesNode);
            if (CollectionUtils.isNotEmpty(module.getDependencies())) {
                populateDependencies(dependenciesNode, module);
            }

            buildDependencyTree.add(moduleNode);
        }
    }

    private void populateArtifacts(DependencyTree artifactsNode, Module module) {
        for (Artifact artifact : module.getArtifacts()) {
            GeneralInfo artifactGeneralInfo = new GeneralInfo()
                    .componentId(artifact.getName())
                    .pkgType(artifact.getType())
                    .sha1(artifact.getSha1());
            DependencyTree artifactNode = new DependencyTree(artifact.getName());
            artifactNode.setGeneralInfo(artifactGeneralInfo);
            artifactNode.setScopes(Sets.newHashSet(new Scope()));
            artifactNode.setLicenses(Sets.newHashSet(new License()));
            artifactsNode.add(artifactNode);
        }
    }

    private void populateDependencies(DependencyTree dependenciesNode, Module module) {
        Set<Dependency> directDependencies = Sets.newHashSet();
        Multimap<String, Dependency> parentToChildren = HashMultimap.create();
        for (Dependency dependency : module.getDependencies()) {
            String[][] requestedBy = dependency.getRequestedBy();
            if (isEmpty(requestedBy) || isEmpty(requestedBy[0])) {
                directDependencies.add(dependency);
                continue;
            }

            for (String[] parent : requestedBy) {
                String directParent = parent[0];
                if (StringUtils.isBlank(requestedBy[0][0]) || StringUtils.equals(requestedBy[0][0], module.getId())) {
                    directDependencies.add(dependency);
                } else {
                    parentToChildren.put(directParent, dependency);
                }
            }
        }

        for (Dependency directDependency : directDependencies) {
            dependenciesNode.add(populateTransitiveDependencies(directDependency, parentToChildren));
        }
    }

    private DependencyTree populateTransitiveDependencies(Dependency dependency, Multimap<String, Dependency> parentToChildren) {
        GeneralInfo dependencyGeneralInfo = new GeneralInfo()
                .componentId(dependency.getId())
                .pkgType(dependency.getType())
                .sha1(dependency.getSha1());
        DependencyTree dependencyTree = new DependencyTree(dependency.getId());
        dependencyTree.setGeneralInfo(dependencyGeneralInfo);
        Collection<String> scopes = CollectionUtils.emptyIfNull(dependency.getScopes());
        dependencyTree.setScopes(scopes.stream().map(Scope::new).collect(Collectors.toSet()));
        dependencyTree.setLicenses(Sets.newHashSet(new License()));

        for (Dependency child : parentToChildren.get(dependency.getId())) {
            dependencyTree.add(populateTransitiveDependencies(child, parentToChildren));
        }
        return dependencyTree;
    }

    public void populateBuildDependencyTree(DetailsResponse response) {
        Map<String, IssuesAndLicensesPair> artifactIssuesAndLicenses = Maps.newHashMap();
        Map<String, String> sha1ToSha256 = Maps.newHashMap();
        Map<String, com.jfrog.xray.client.services.summary.Artifact> sha1ToComponent = Maps.newHashMap();
        for (com.jfrog.xray.client.services.summary.Artifact component : response.getComponents()) {
            General general = component.getGeneral();
            sha1ToComponent.put(general.getSha1(), component);
            sha1ToSha256.put(general.getSha1(), general.getSha256());

            if (CollectionUtils.isNotEmpty(general.getParentSha256())) {
                for (String parentSha256 : general.getParentSha256()) {
                    IssuesAndLicensesPair issuesAndLicenses = artifactIssuesAndLicenses.get(parentSha256);
                    if (issuesAndLicenses == null) {
                        issuesAndLicenses = new IssuesAndLicensesPair();
                        artifactIssuesAndLicenses.put(parentSha256, issuesAndLicenses);
                    }
                    if (component.getIssues() != null) {
                        issuesAndLicenses.issues.addAll(component.getIssues());
                    }
                    if (component.getLicenses() != null) {
                        issuesAndLicenses.licenses.addAll(component.getLicenses());
                    }
                }
            }
        }

        for (DependencyTree module : getChildren()) {
            for (DependencyTree artifactsOrDependencies : module.getChildren()) {
                boolean isArtifactNode = artifactsOrDependencies.getUserObject().equals(ARTIFACTS_NODE);
                for (DependencyTree child : artifactsOrDependencies.getChildren()) {
                    populateComponents(child, sha1ToComponent, sha1ToSha256, artifactIssuesAndLicenses, isArtifactNode);
                }
            }
        }
    }

    private void populateComponents(DependencyTree buildDependencyTree, Map<String, com.jfrog.xray.client.services.summary.Artifact> sha1ToComponent,
                                    Map<String, String> sha1ToSha256, Map<String, IssuesAndLicensesPair> artifactIssuesAndLicenses,
                                    boolean isArtifact) {
        Enumeration<?> bfs = buildDependencyTree.depthFirstEnumeration();
        while (bfs.hasMoreElements()) {
            DependencyTree buildArtifact = (DependencyTree) bfs.nextElement();
            String nodeSha1 = buildArtifact.getGeneralInfo().getSha1();
            if (isBlank(nodeSha1)) {
                // Sha1 does not exist in node
                continue;
            }
            com.jfrog.xray.client.services.summary.Artifact artifact = sha1ToComponent.get(nodeSha1);
            if (artifact == null) {
                // Artifact not found in Xray scan
                continue;
            }

            if (artifact.getIssues() != null) {
                buildArtifact.setIssues(artifact.getIssues().stream()
                        .map(com.jfrog.ide.common.utils.Utils::toIssue).collect(Collectors.toSet()));
            }
            if (artifact.getLicenses() != null) {
                buildArtifact.setLicenses(artifact.getLicenses().stream()
                        .map(com.jfrog.ide.common.utils.Utils::toLicense).collect(Collectors.toSet()));
            }

            if (!isArtifact) {
                continue;
            }
            String nodeSha256 = sha1ToSha256.get(nodeSha1);
            IssuesAndLicensesPair issuesAndLicenses = artifactIssuesAndLicenses.get(nodeSha256);
            if (issuesAndLicenses != null) {
                buildArtifact.getIssues()
                        .addAll(issuesAndLicenses.issues.stream().map(com.jfrog.ide.common.utils.Utils::toIssue).collect(Collectors.toList()));
                buildArtifact.getLicenses()
                        .addAll(issuesAndLicenses.licenses.stream().map(com.jfrog.ide.common.utils.Utils::toLicense).collect(Collectors.toList()));
            }
        }
    }

    private static class IssuesAndLicensesPair {
        private final Set<com.jfrog.xray.client.services.summary.Issue> issues = Sets.newHashSet();
        private final Set<com.jfrog.xray.client.services.summary.License> licenses = Sets.newHashSet();
    }
}
