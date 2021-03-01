package com.jfrog.ide.common.vcs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.jfrog.ide.common.log.ProgressIndicator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * @author yahavi
 **/
public class BuildArtifactsDownloader extends ProducerRunnableBase {
    public static final String BUILD_INFO_REPO = "/artifactory-build-info/";

    private final Queue<AqlSearchResult.SearchEntry> buildArtifacts;
    private final ArtifactoryDependenciesClient client;
    private final ProgressIndicator indicator;
    private final AtomicInteger count;
    private final double total;

    public BuildArtifactsDownloader(Queue<AqlSearchResult.SearchEntry> buildArtifacts, ArtifactoryDependenciesClient client,
                                    ProgressIndicator indicator, AtomicInteger count, double total) {
        this.buildArtifacts = buildArtifacts;
        this.indicator = indicator;
        this.client = client;
        this.count = count;
        this.total = total;
    }

    @Override
    public void producerRun() throws InterruptedException {
        ObjectMapper mapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false).setSerializationInclusion(NON_NULL);
        try {
            while (!buildArtifacts.isEmpty()) {
                if (Thread.interrupted()) {
                    break;
                }
                AqlSearchResult.SearchEntry searchEntry = buildArtifacts.remove();
                HttpEntity entity = null;
                String downloadUrl = client.getArtifactoryUrl() + BUILD_INFO_REPO + searchEntry.getPath() + "/" + searchEntry.getName();
                try (CloseableHttpResponse response = client.downloadArtifact(downloadUrl)) {
                    entity = response.getEntity();
                    Build build = mapper.readValue(response.getEntity().getContent(), Build.class);
                    List<Vcs> vcsList = build.getVcs();
                    if (CollectionUtils.isEmpty(vcsList)) {
                        throw new IOException("Build '" + build.getName() + "/" + build.getNumber() + "' does not contain the branch VCS information");
                    }
                    GeneralInfo buildGeneralInfo = new GeneralInfo()
                            .name(build.getName())
                            .version(build.getNumber())
                            .path(build.getUrl());
                    DependencyTree DependencyTree = new DependencyTree(vcsList.get(0).getRevision());
                    DependencyTree.setGeneralInfo(buildGeneralInfo);
                    if (CollectionUtils.isNotEmpty(build.getModules())) {
                        populateBuildModules(DependencyTree, build);
                    }

                    executor.put(DependencyTree);
                    indicator.setFraction(count.incrementAndGet() / total);
                } finally {
                    EntityUtils.consumeQuietly(entity);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't retrieve build information", e);
        }
    }

    private void populateBuildModules(DependencyTree DependencyTree, Build build) {
        for (Module module : build.getModules()) {
            GeneralInfo moduleGeneralInfo = new GeneralInfo()
                    .componentId(module.getId())
                    .pkgType(module.getType());
            DependencyTree moduleNode = new DependencyTree(module.getId());
            moduleNode.setGeneralInfo(moduleGeneralInfo);
            if (CollectionUtils.isNotEmpty(module.getDependencies())) {
                populateModuleDependencies(moduleNode, module);
            }
            DependencyTree.add(moduleNode);
        }
    }

    private void populateModuleDependencies(DependencyTree moduleNode, Module module) {
        for (Dependency dependency : module.getDependencies()) {
            GeneralInfo dependencyGeneralInfo = new GeneralInfo()
                    .componentId(dependency.getId())
                    .pkgType(dependency.getType());
            DependencyTree dependencyTree = new DependencyTree(dependency.getId());
            dependencyTree.setGeneralInfo(dependencyGeneralInfo);
            Set<String> scopes = dependency.getScopes();
            if (scopes != null) {
                dependencyTree.setScopes(scopes.stream().map(Scope::new).collect(Collectors.toSet()));
            } else {
                dependencyTree.setScopes(Sets.newHashSet(new Scope()));
            }
            dependencyTree.setLicenses(Sets.newHashSet(new License()));
            moduleNode.add(dependencyTree);
        }
    }
}
