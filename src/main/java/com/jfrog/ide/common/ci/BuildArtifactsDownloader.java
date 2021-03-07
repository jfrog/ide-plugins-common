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
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.jfrog.ide.common.utils.Utils.createMapper;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

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
        ObjectMapper mapper = createMapper();
        try {
            while (!buildArtifacts.isEmpty()) {
                if (Thread.interrupted()) {
                    break;
                }
                AqlSearchResult.SearchEntry searchEntry = buildArtifacts.remove();
//                URI uri = URI.create(client.getArtifactoryUrl()).resolve(BUILD_INFO_REPO).resolve(searchEntry.getPath()).resolve(searchEntry.getName());
                String downloadUrl = client.getArtifactoryUrl() + BUILD_INFO_REPO + searchEntry.getPath() + "/" + searchEntry.getName();
                HttpEntity entity = null;
                try (CloseableHttpResponse response = client.downloadArtifact(downloadUrl)) {
                    entity = response.getEntity();
                    Build build = mapper.readValue(entity.getContent(), Build.class);
                    DependencyTree buildDependencyTree = createBuildDependencyTree(build);
                    if (buildDependencyTree != null) {
                        executor.put(buildDependencyTree);
                    }
                } finally {
                    EntityUtils.consumeQuietly(entity);
                }
                indicator.setFraction(count.incrementAndGet() / total);

            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't retrieve build information", e);
        }
    }

    DependencyTree createBuildDependencyTree(Build build) {
        List<Vcs> vcsList = build.getVcs();
        if (CollectionUtils.isEmpty(vcsList)) {
            log.warn("Build '" + build.getName() + "/" + build.getNumber() + "' does not contain the branch VCS information");
            return null;
        }

        GeneralInfo buildGeneralInfo = new GeneralInfo()
                .name(build.getName())
                .version(build.getNumber())
                .path(build.getUrl());
//        DependencyTree dependencyTree = new DependencyTree(build.getVcs().get(0).getMessage()); // TODO - restore
        DependencyTree dependencyTree = new DependencyTree(build.getName() + "/" + build.getNumber());
        dependencyTree.setScopes(Sets.newHashSet(new Scope()));
        dependencyTree.setGeneralInfo(buildGeneralInfo);
        if (CollectionUtils.isNotEmpty(build.getModules())) {
            populateModulesDependencyTree(dependencyTree, build);
        }
        return dependencyTree;
    }

    private void populateModulesDependencyTree(DependencyTree DependencyTree, Build build) {
        for (Module module : build.getModules()) {
            GeneralInfo moduleGeneralInfo = new GeneralInfo()
                    .componentId(module.getId())
                    .pkgType(module.getType());
            DependencyTree moduleNode = new DependencyTree(module.getId());
            moduleNode.setGeneralInfo(moduleGeneralInfo);
            if (CollectionUtils.isNotEmpty(module.getDependencies())) {
                populateDirectDependencies(moduleNode, module);
            }
            DependencyTree.add(moduleNode);
        }
    }

    private void populateDirectDependencies(DependencyTree moduleNode, Module module) {
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
            moduleNode.add(populateTransitiveDependencies(directDependency, parentToChildren));
        }
    }

    private DependencyTree populateTransitiveDependencies(Dependency dependency, Multimap<String, Dependency> parentToChildren) {
        GeneralInfo dependencyGeneralInfo = new GeneralInfo().componentId(dependency.getId()).pkgType(dependency.getType());
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
