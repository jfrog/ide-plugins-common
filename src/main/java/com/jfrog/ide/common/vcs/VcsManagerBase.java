package com.jfrog.ide.common.vcs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author yahavi
 **/
public class VcsManagerBase {
    protected DependencyTree root = new DependencyTree();

    public void refreshBuilds(String buildsPattern, ArtifactoryDependenciesClient client, String projectPath, Log logger, ProgressIndicator indicator) {
        try {
            AqlSearchResult results = client.searchArtifactsByAql(createAql(buildsPattern));
            ObjectMapper mapper = createMapper();

            Multimap<String, Build> branchToBuild = HashMultimap.create();
            double total = results.getResults().size();

            AtomicInteger count = new AtomicInteger();
            for (AqlSearchResult.SearchEntry searchEntry : results.getResults()) {
                HttpEntity entity = null;
                try (CloseableHttpResponse response = client.downloadArtifact(client.getArtifactoryUrl() + "/artifactory-build-info/" + searchEntry.getPath() + "/" + searchEntry.getName())) {
                    entity = response.getEntity();
                    Build build = mapper.readValue(response.getEntity().getContent(), Build.class);
                    List<Vcs> vcsList = build.getVcs();
                    if (CollectionUtils.isNotEmpty(vcsList)) {
                        Vcs vcs = vcsList.get(0);
                        branchToBuild.put(vcs.getRevision(), build);
                    }
                    indicator.setFraction(count.incrementAndGet() / total);
                } finally {
                    EntityUtils.consumeQuietly(entity);
                }
            }

            GeneralInfo generalInfo = new GeneralInfo().path(projectPath);
            root = new DependencyTree();
            root.setGeneralInfo(generalInfo);
            branchToBuild.forEach((branch, build) -> {
                GeneralInfo buildGeneralInfo = new GeneralInfo()
                        .name(build.getName())
                        .version(build.getNumber())
                        .path(build.getUrl());
                DependencyTree DependencyTree = new DependencyTree(build.getName() + ":" + build.getNumber());
                DependencyTree.setGeneralInfo(buildGeneralInfo);
                populateDependencyTree(DependencyTree, build);
                root.add(DependencyTree);
            });

        } catch (IOException e) {
            logger.error("Couldn't retrieve build information", e);
        }
    }

    /**
     * Recursively, add all dependencies list licenses to the licenses set.
     */
    public Set<License> getAllLicenses() {
        Set<License> allLicences = Sets.newHashSet();
        root.collectAllScopesAndLicenses(Sets.newHashSet(), allLicences);
        return allLicences;
    }

    /**
     * Recursively, add all dependencies list scopes to the scopes set.
     */
    public Set<Scope> getAllScopes() {
        Set<Scope> allScopes = Sets.newHashSet();
        root.collectAllScopesAndLicenses(allScopes, Sets.newHashSet());
        return allScopes;
    }

    private void populateDependencyTree(DependencyTree DependencyTree, Build build) {
        for (Module module : build.getModules()) {
            GeneralInfo moduleGeneralInfo = new GeneralInfo()
                    .componentId(module.getId())
                    .pkgType(module.getType());
            DependencyTree moduleNode = new DependencyTree(module.getId());
            moduleNode.setGeneralInfo(moduleGeneralInfo);
            populateModuleDependencies(moduleNode, module);
            DependencyTree.add(moduleNode);
        }
    }

    private void populateModuleDependencies(DependencyTree moduleNode, Module module) {
        for (Dependency dependency : module.getDependencies()) {
            GeneralInfo dependencyGeneralInfo = new GeneralInfo()
                    .componentId(dependency.getId())
                    .pkgType(dependency.getType());
            DependencyTree DependencyTree = new DependencyTree(dependency.getId());
            DependencyTree.setGeneralInfo(dependencyGeneralInfo);
            DependencyTree.setScopes(dependency.getScopes().stream().map(Scope::new).collect(Collectors.toSet()));
            DependencyTree.setLicenses(Sets.newHashSet(new License()));

            moduleNode.add(DependencyTree);
        }
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    private String createAql(String buildsPattern) {
        return String.format("items.find({" +
                "\"repo\":\"artifactory-build-info\"," +
                "\"path\":{\"$match\":\"%s\"}}" +
                ").include(\"name\",\"repo\",\"path\",\"created\").sort({\"$asc\":[\"created\"]}).limit(10)", buildsPattern);
    }

    private long getLastModified(String date) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        Date parse = simpleDateFormat.parse(date);
        return parse.getTime();
    }

}
