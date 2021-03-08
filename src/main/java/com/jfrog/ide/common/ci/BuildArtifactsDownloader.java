package com.jfrog.ide.common.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.jfrog.ide.common.log.ProgressIndicator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.*;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.jfrog.ide.common.utils.Utils.createMapper;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

/**
 * @author yahavi
 **/
public class BuildArtifactsDownloader extends ProducerRunnableBase {
    public static final String BUILD_STATUS_PROP = BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + "JFROG_BUILD_RESULTS";
    public static final String BUILD_INFO_REPO = "/artifactory-build-info/";

    private final ArtifactoryDependenciesClientBuilder clientBuilder;
    private final Queue<AqlSearchResult.SearchEntry> buildArtifacts;
    private final ProgressIndicator indicator;
    private final AtomicInteger count;
    private final double total;

    public BuildArtifactsDownloader(Queue<AqlSearchResult.SearchEntry> buildArtifacts,
                                    ArtifactoryDependenciesClientBuilder clientBuilder,
                                    ProgressIndicator indicator, AtomicInteger count, double total) {
        this.buildArtifacts = buildArtifacts;
        this.clientBuilder = clientBuilder;
        this.indicator = indicator;
        this.count = count;
        this.total = total;
    }

    @Override
    public void producerRun() throws InterruptedException {
        ObjectMapper mapper = createMapper();

        try (ArtifactoryDependenciesClient client = clientBuilder.build()) {
            String baseRepoUrl = client.getArtifactoryUrl() + BUILD_INFO_REPO;
            while (!buildArtifacts.isEmpty()) {
                if (Thread.interrupted()) {
                    break;
                }
                AqlSearchResult.SearchEntry searchEntry = buildArtifacts.remove();
                String downloadUrl = baseRepoUrl + searchEntry.getPath() + "/" + searchEntry.getName();
                HttpEntity entity = null;
                try (CloseableHttpResponse response = client.downloadArtifact(downloadUrl)) {
                    entity = response.getEntity();
                    Build build = mapper.readValue(entity.getContent(), Build.class);
                    DependencyTree buildDependencyTree = createBuildDependencyTree(build);
                    if (buildDependencyTree != null) {
                        executor.put(buildDependencyTree);
                    }
                } catch (ParseException | IOException e) {
                    log.error("Couldn't retrieve build information", e);
                } finally {
                    EntityUtils.consumeQuietly(entity);
                }
                indicator.setFraction(count.incrementAndGet() / total);
            }
        }
    }

    DependencyTree createBuildDependencyTree(Build build) throws ParseException {
        List<Vcs> vcsList = build.getVcs();
        if (CollectionUtils.isEmpty(vcsList)) {
            log.warn("Build '" + build.getName() + "/" + build.getNumber() + "' does not contain the branch VCS information");
            return null;
        }

        Properties buildProperties = build.getProperties();
        GeneralInfo buildGeneralInfo = new BuildGeneralInfo()
                .started(build.getStarted())
                .status(buildProperties != null ? buildProperties.getProperty(BUILD_STATUS_PROP) : "")
                .vcs(vcsList.get(0))
                .name(build.getName())
                .version(build.getNumber())
                .path(build.getUrl());
        DependencyTree dependencyTree = new DependencyTree(build.getName() + "/" + build.getNumber());
        dependencyTree.setScopes(Sets.newHashSet(new Scope()));
        dependencyTree.setGeneralInfo(buildGeneralInfo);
        if (CollectionUtils.isNotEmpty(build.getModules())) {
            populateModulesDependencyTree(dependencyTree, build);
        }
        return dependencyTree;
    }

    private void populateModulesDependencyTree(DependencyTree buildDependencyTree, Build build) {
        for (Module module : build.getModules()) {
            GeneralInfo moduleGeneralInfo = new GeneralInfo()
                    .componentId(module.getId())
                    .pkgType(module.getType());
            DependencyTree moduleNode = new DependencyTree(module.getId());
            moduleNode.setGeneralInfo(moduleGeneralInfo);

            // Populate artifacts
            DependencyTree artifactsNode = new DependencyTree(CiManagerBase.ARTIFACTS_NODE);
            GeneralInfo artifactsGeneralInfo = new GeneralInfo().componentId(module.getId()).pkgType("Module artifacts");
            artifactsNode.setGeneralInfo(artifactsGeneralInfo);
            moduleNode.add(artifactsNode);
            if (CollectionUtils.isNotEmpty(module.getArtifacts())) {
                populateArtifacts(artifactsNode, module);
            }

            // Populate dependencies
            DependencyTree dependenciesNode = new DependencyTree(CiManagerBase.DEPENDENCIES_NODE);
            GeneralInfo dependenciesGeneralInfo = new GeneralInfo().componentId(module.getId()).pkgType("Module dependencies");
            dependenciesNode.setGeneralInfo(dependenciesGeneralInfo);
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

}
