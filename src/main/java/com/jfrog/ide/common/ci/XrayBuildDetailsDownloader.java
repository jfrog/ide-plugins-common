package com.jfrog.ide.common.ci;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.utils.Utils;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.impl.XrayClientBuilder;
import com.jfrog.xray.client.services.details.DetailsResponse;
import com.jfrog.xray.client.services.summary.Error;
import com.jfrog.xray.client.services.summary.*;
import org.apache.commons.collections4.CollectionUtils;
import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.jfrog.ide.common.ci.Utils.ARTIFACTS_NODE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author yahavi
 **/
public class XrayBuildDetailsDownloader extends ConsumerRunnableBase {
    private final XrayClientBuilder xrayClientBuilder;
    private ProducerConsumerExecutor executor;
    private final ProgressIndicator indicator;
    private final DependencyTree root;
    private final AtomicInteger count;
    private final double total;
    private Log log;

    public XrayBuildDetailsDownloader(DependencyTree root, XrayClientBuilder xrayClientBuilder,
                                      ProgressIndicator indicator, AtomicInteger count, double total, Log log) {
        this.xrayClientBuilder = xrayClientBuilder;
        this.indicator = indicator;
        this.count = count;
        this.total = total;
        this.root = root;
        this.log = log;
    }

    @Override
    public void consumerRun() {
        try (XrayClient xrayClient = xrayClientBuilder.build()) {
            while (!Thread.interrupted()) {
                ProducerConsumerItem item = executor.take();
                if (item == executor.TERMINATE) {
                    // If reached the TERMINATE, return it to the queue and exit.
                    executor.put(item);
                    break;
                }
                DependencyTree buildDependencyTree = (DependencyTree) item;
                GeneralInfo generalInfo = buildDependencyTree.getGeneralInfo();
                try {
                    DetailsResponse response = xrayClient.details().build(generalInfo.getArtifactId(), generalInfo.getVersion());
                    if (!response.getScanCompleted() || isNotEmpty(response.getErrors()) || isEmpty(response.getComponents())) {
                        if (CollectionUtils.isNotEmpty(response.getErrors())) {
                            printError(response);
                        }
                        continue;
                    }
                    populateBuildDependencyTree(buildDependencyTree, response);
                    synchronized (root) {
                        root.add(buildDependencyTree);
                    }
                } catch (IOException exception) {
                    log.error("Couldn't scan build", exception);
                } finally {
                    indicator.setFraction(count.incrementAndGet() / total);
                }
            }
        } catch (InterruptedException ignored) {
        }

    }

    private void printError(DetailsResponse response) {
        response.getErrors().stream()
                .map(err -> (Error) err)
                .forEach(err -> log.error(err.getError() + "/n" + err.getIdentifier()));
    }

    public void populateBuildDependencyTree(DependencyTree buildDependencyTree, DetailsResponse response) {
        Map<String, IssuesAndLicensesPair> artifactIssuesAndLicenses = Maps.newHashMap();
        Map<String, String> sha1ToSha256 = Maps.newHashMap();
        Map<String, Artifact> sha1ToComponent = Maps.newHashMap();
        for (Artifact component : response.getComponents()) {
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

        for (DependencyTree module : buildDependencyTree.getChildren()) {
            for (DependencyTree artifactsOrDependencies : module.getChildren()) {
                boolean isArtifactNode = artifactsOrDependencies.getUserObject().equals(ARTIFACTS_NODE);
                for (DependencyTree child : artifactsOrDependencies.getChildren()) {
                    populateComponents(child, sha1ToComponent, sha1ToSha256, artifactIssuesAndLicenses, isArtifactNode);
                }
            }
        }
    }

    private void populateComponents(DependencyTree buildDependencyTree, Map<String, Artifact> sha1ToComponent,
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
            Artifact artifact = sha1ToComponent.get(nodeSha1);
            if (artifact == null) {
                // Artifact not found in Xray scan
                continue;
            }

            if (artifact.getIssues() != null) {
                buildArtifact.setIssues(artifact.getIssues().stream()
                        .map(Utils::toIssue).collect(Collectors.toSet()));
            }
            if (artifact.getLicenses() != null) {
                buildArtifact.setLicenses(artifact.getLicenses().stream()
                        .map(Utils::toLicense).collect(Collectors.toSet()));
            }

            if (!isArtifact) {
                continue;
            }
            String nodeSha256 = sha1ToSha256.get(nodeSha1);
            IssuesAndLicensesPair issuesAndLicenses = artifactIssuesAndLicenses.get(nodeSha256);
            if (issuesAndLicenses != null) {
                buildArtifact.getIssues()
                        .addAll(issuesAndLicenses.issues.stream().map(Utils::toIssue).collect(Collectors.toList()));
                buildArtifact.getLicenses()
                        .addAll(issuesAndLicenses.licenses.stream().map(Utils::toLicense).collect(Collectors.toList()));
            }
        }
    }

    @Override
    public void setExecutor(ProducerConsumerExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void setLog(Log log) {
        this.log = log;
    }

    private static class IssuesAndLicensesPair {
        private final Set<Issue> issues = Sets.newHashSet();
        private final Set<License> licenses = Sets.newHashSet();
    }
}
