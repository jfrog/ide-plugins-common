package com.jfrog.ide.common.ci;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.jfrog.xray.client.services.details.DetailsResponse;
import com.jfrog.xray.client.services.summary.General;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.*;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static com.jfrog.ide.common.ci.Utils.*;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Represents the dependency tree of a CI build.
 *
 * @author yahavi
 **/
public class BuildDependencyTree extends DependencyTree {
    public BuildDependencyTree() {
        super();
    }

    public BuildDependencyTree(Object userObject) {
        super(userObject);
        setMetadata(true);
    }

    /**
     * Create the build dependency tree from the provided build info.
     *
     * @param build - The build info
     * @throws ParseException in case of parse exception in the build info started time.
     */
    public void createBuildDependencyTree(Build build, Log logger) throws ParseException {
        setUserObject(build.getName() + "/" + build.getNumber());
        setScopes(Sets.newHashSet(new Scope()));
        setGeneralInfo(createBuildGeneralInfo(build, logger));
        if (CollectionUtils.isNotEmpty(build.getModules())) {
            populateModulesDependencyTree(this, build, logger);
        }
    }

    private void populateModulesDependencyTree(DependencyTree buildDependencyTree, Build build, Log logger) {
        for (Module module : build.getModules()) {
            GeneralInfo moduleGeneralInfo = new GeneralInfo()
                    .componentId(module.getId())
                    .pkgType(module.getType());
            DependencyTree moduleNode = new DependencyTree(module.getId());
            moduleNode.setMetadata(true);
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
                populateDependencies(dependenciesNode, module, logger);
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

    private void populateDependencies(DependencyTree dependenciesNode, Module module, Log logger) {
        Multimap<String, Dependency> parentToChildren = HashMultimap.create();
        for (Dependency dependency : module.getDependencies()) {
            String[][] requestedBy = dependency.getRequestedBy();
            // If there is no "requestedBy" field or the module is the parent of the dependency,
            // the direct parent is the dependencies node.
            if (isEmpty(requestedBy) || isEmpty(requestedBy[0])) {
                parentToChildren.put(dependenciesNode.toString(), dependency);
                continue;
            }

            for (String[] parent : requestedBy) {
                String directParent;
                // If there is an empty "requestedBy" field or the module is the parent of the dependency -
                // the direct parent is the dependencies node.
                // Otherwise - the direct parent is the first dependency in the requestedBy.
                if (isBlank(requestedBy[0][0]) || StringUtils.equals(requestedBy[0][0], module.getId())) {
                    directParent = dependenciesNode.toString();
                } else {
                    directParent = parent[0];
                }
                parentToChildren.put(directParent, dependency);
            }
        }
        populateTransitiveDependencies(dependenciesNode, parentToChildren, logger);
    }

    private void populateTransitiveDependencies(DependencyTree node, Multimap<String, Dependency> parentToChildren, Log logger) {
        for (Dependency childDependency : parentToChildren.get(node.toString())) {
            GeneralInfo generalInfo = new GeneralInfo()
                    .componentId(childDependency.getId())
                    .pkgType(childDependency.getType())
                    .sha1(childDependency.getSha1());
            DependencyTree child = new DependencyTree(childDependency.getId());
            child.setGeneralInfo(generalInfo);
            Collection<String> scopes = CollectionUtils.emptyIfNull(childDependency.getScopes());
            child.setScopes(scopes.stream().map(Scope::new).collect(Collectors.toSet()));
            child.setLicenses(Sets.newHashSet(new License()));
            node.add(child);
            if (node.hasLoop(logger)) {
                return;
            }
            populateTransitiveDependencies(child, parentToChildren, logger);
        }
    }

    /**
     * Populate build dependency tree with issues and licenses.
     *
     * @param response - The response from 'details/build' Xray REST API
     */
    public void populateBuildDependencyTree(DetailsResponse response) {
        if (response == null) {
            // If no response from Xray, the dependency tree components status is unknown.
            // Populate all nodes with dummy unknown level issues to show the unknown icon in tree nodes.
            populateTreeWithUnknownIssues();
            return;
        }
        // Component to issues and licenses mapping
        Map<String, IssuesAndLicensesPair> componentIssuesAndLicenses = Maps.newHashMap();
        // Sha1 to Sha256 mapping
        Map<String, String> sha1ToSha256 = Maps.newHashMap();
        // Component to Xray Artifact mapping
        Map<String, com.jfrog.xray.client.services.summary.Artifact> sha1ToComponent = Maps.newHashMap();

        // Populate the above mappings. We will use the information to populate the dependency tree efficiently.
        for (com.jfrog.xray.client.services.summary.Artifact component : ListUtils.emptyIfNull(response.getComponents())) {
            General general = component.getGeneral();
            sha1ToComponent.put(general.getSha1(), component);
            sha1ToSha256.put(general.getSha1(), general.getSha256());

            if (CollectionUtils.isNotEmpty(general.getParentSha256())) {
                for (String parentSha256 : general.getParentSha256()) {
                    IssuesAndLicensesPair issuesAndLicenses = componentIssuesAndLicenses.get(parentSha256);
                    if (issuesAndLicenses == null) {
                        issuesAndLicenses = new IssuesAndLicensesPair();
                        componentIssuesAndLicenses.put(parentSha256, issuesAndLicenses);
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

        // Populate the build modules
        for (DependencyTree module : getChildren()) {
            for (DependencyTree artifactsOrDependencies : module.getChildren()) {
                boolean isArtifactNode = artifactsOrDependencies.getUserObject().equals(ARTIFACTS_NODE);
                for (DependencyTree child : artifactsOrDependencies.getChildren()) {
                    // Populate dependencies and artifacts
                    populateComponents(child, sha1ToComponent, sha1ToSha256, componentIssuesAndLicenses, isArtifactNode);
                }
            }
        }
    }

    /**
     * Populate issues and artifacts with issues and licenses.
     *
     * @param buildDependencyTree       - The build dependency tree to populate
     * @param sha1ToComponent           - Sha1 to component mapping
     * @param sha1ToSha256              - Sha1 to sha256 mapping
     * @param artifactIssuesAndLicenses - Artifact to issues and licenses mapping
     * @param isArtifact                - True if the components are artifacts. False if the components are dependencies
     */
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
            if (CollectionUtils.isNotEmpty(artifact.getLicenses())) {
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

    private void populateTreeWithUnknownIssues() {
        Enumeration<?> bfs = depthFirstEnumeration();
        while (bfs.hasMoreElements()) {
            DependencyTree node = (DependencyTree) bfs.nextElement();
            node.setIssues(Sets.newHashSet(new org.jfrog.build.extractor.scan.Issue("", Severity.Unknown, "",
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "")));
        }
    }

    private static class IssuesAndLicensesPair {
        private final Set<com.jfrog.xray.client.services.summary.Issue> issues = Sets.newHashSet();
        private final Set<com.jfrog.xray.client.services.summary.License> licenses = Sets.newHashSet();
    }
}
