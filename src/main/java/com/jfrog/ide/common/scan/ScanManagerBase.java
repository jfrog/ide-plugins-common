package com.jfrog.ide.common.scan;

import com.google.common.collect.Sets;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.util.Enumeration;
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
     * Populate a DependencyTree node with issues, licenses and general info from the scan cache.
     *
     * @param node - The root node.
     */
    protected void populateDependencyTreeNode(DependencyTree node) {
        Artifact scanArtifact = getArtifactSummary(node.toString());
        if (scanArtifact != null) {
            node.setIssues(Sets.newHashSet(scanArtifact.getIssues()));
            node.setLicenses(Sets.newHashSet(scanArtifact.getLicenses()));
            if (node.getGeneralInfo() == null) {
                node.setGeneralInfo(scanArtifact.getGeneralInfo());
            }
        }
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

    /**
     * @param componentId artifact component ID.
     * @return {@link Artifact} according to the component ID.
     */
    protected Artifact getArtifactSummary(String componentId) {
        return scanLogic.getArtifactSummary(componentId);
    }

    /**
     * Scan and cache components.
     *
     * @param indicator - Progress bar.
     * @param quickScan - Quick or full scan.
     */
    public void scanAndCacheArtifacts(ProgressIndicator indicator, boolean quickScan) throws IOException, InterruptedException {
        log.debug("Start scan for '" + projectName + "'.");
        if (scanLogic.scanAndCacheArtifacts(serverConfig, indicator, quickScan, prefix, this::checkCanceled)) {
            log.debug("Scan for '" + projectName + "' finished successfully.");
        } else {
            log.debug("Wasn't able to scan '" + projectName + "'.");
        }
    }


    /**
     * Add Xray scan results from cache to the dependency tree.
     *
     * @param node - The dependency tree.
     */
    protected void addXrayInfoToTree(DependencyTree node) {
        if (node == null || node.isLeaf()) {
            return;
        }
        for (DependencyTree child : node.getChildren()) {
            populateDependencyTreeNode(child);
            addXrayInfoToTree(child);
        }
    }

    public DependencyTree getScanResults() {
        return scanLogic.getScanResults();
    }

    public void setScanResults(DependencyTree results) {
        scanLogic.setScanResults(results);
    }

    /**
     * @throws CancellationException if the user clicked on the cancel button.
     */
    protected abstract void checkCanceled() throws CancellationException;
}
