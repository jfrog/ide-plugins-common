package com.jfrog.ide.common.scan;

import com.google.common.collect.Sets;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.tree.Artifact;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;

/**
 * Base class for the scan managers.
 *
 * @author yahavi
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@Getter
@Setter
public abstract class ScanManagerBase {

    private ServerConfig serverConfig;
    private ComponentPrefix prefix;
    private ScanLogic scanLogic;
    private String projectName;
    private Log log;

    /**
     * Construct a scan manager for IDE project.
     *
     * @param projectName  - The project name.
     * @param log          - The logger.
     * @param serverConfig - Xray server config.
     * @param prefix       - Components prefix for xray scan, e.g. gav:// or npm://.
     */
    public ScanManagerBase(String projectName, Log log, ServerConfig serverConfig, ComponentPrefix prefix) {
        this.serverConfig = serverConfig;
        this.projectName = projectName;
        this.prefix = prefix;
        this.log = log;
    }

    /**
     * Recursively, add all dependencies list licenses to the licenses set.
     *
     * @param node - The root DependencyTree node.
     */
    protected Set<License> collectAllLicenses(DependencyTree node) {
        Set<License> allLicenses = Sets.newHashSet();
        Enumeration<?> enumeration = node.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DependencyTree child = (DependencyTree) enumeration.nextElement();
            allLicenses.addAll(child.getLicenses());
        }
        return allLicenses;
    }

    /**
     * Recursively, add all dependencies list scopes to the scopes set.
     *
     * @param node - The root DependencyTree node.
     */
    protected Set<Scope> collectAllScopes(DependencyTree node) {
        Set<Scope> allScopes = Sets.newHashSet();
        Enumeration<?> enumeration = node.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DependencyTree child = (DependencyTree) enumeration.nextElement();
            allScopes.addAll(child.getScopes());
        }
        return allScopes;
    }

    // TODO: update comment
    /**
     * Scan and cache components.
     *
     * @param indicator - Progress bar.
     */
    public Map<String, Artifact> scanArtifacts(ProgressIndicator indicator, DependencyTree dependencyTree) throws IOException, InterruptedException {
        log.debug("Start scan for '" + projectName + "'.");
        Map<String, Artifact> results = scanLogic.scanArtifacts(dependencyTree, serverConfig, indicator, prefix, this::checkCanceled);
        // TODO: consider removing this if
        if (results != null) {
            log.debug("Scan for '" + projectName + "' finished successfully.");
        } else {
            log.debug("Wasn't able to scan '" + projectName + "'.");
        }
        return results;
    }


    // TODO: update comment
//    /**
//     * Add Xray scan results from cache to the dependency tree.
//     *
//     * @param node - The dependency tree.
//     */
//    private void populateDescriptorNode(BasicTreeNode fileNode, DependencyTree node) {
//        if (!node.getIssues().isEmpty() || !node.getViolatedLicenses().isEmpty()) {
//            BasicTreeNode depNode = new DependencyTreeNode(node);
//            for (Issue issue : node.getIssues()) {
//                BasicTreeNode issueNode = new IssueTreeNode(issue);
//                // TODO: not done
//            }
//            fileNode.add(depNode);
//        }
//        if (node.isLeaf()) {
//            return;
//        }
//        for (DependencyTree child : node.getChildren()) {
//            populateDependencyTreeNode(child);
//            populateDescriptorNode(fileNode, child);
//        }
//    }

    // TODO: remove? or save the scan results in the new format, and then filter.
//    public DependencyTree getScanResults() {
//        return scanLogic.getScanResults();
//    }

    // TODO: remove
//    public void setScanResults(DependencyTree results) {
//        scanLogic.setScanResults(results);
//    }

    /**
     * @throws CancellationException if the user clicked on the cancel button.
     */
    protected abstract void checkCanceled() throws CancellationException;
}
