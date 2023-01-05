package com.jfrog.ide.common.tree;

import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.TreeNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yahavi
 */
public class DependencyNode extends ComparableSeverityTreeNode implements Serializable, SubtitledTreeNode {

    private static final long serialVersionUID = 1L;

    private GeneralInfo generalInfo;
    private ImpactTreeNode impactPaths;
    private final List<License> licenses;

    // Empty constructor for serialization
    public DependencyNode() {
        generalInfo = new GeneralInfo();
        licenses = new ArrayList<>();
    }

    public DependencyNode(GeneralInfo generalInfo) {
        this.generalInfo = generalInfo;
        licenses = new ArrayList<>();
    }

    @SuppressWarnings("unused")
    public GeneralInfo getGeneralInfo() {
        return generalInfo;
    }

    @SuppressWarnings("unused")
    public void setGeneralInfo(GeneralInfo generalInfo) {
        this.generalInfo = generalInfo;
    }

    public List<License> getLicenses() {
        return licenses;
    }

    public void addLicense(License license) {
        licenses.add(license);
    }

    @Override
    public Severity getSeverity() {
        Severity severity = Severity.Normal;
        for (TreeNode child : children) {
            Severity childSeverity = ((VulnerabilityOrViolationNode) child).getSeverity();
            if (childSeverity.isHigherThan(severity)) {
                severity = childSeverity;
            }
        }
        return severity;
    }

    public ImpactTreeNode getImpactPaths() {
        return impactPaths;
    }

    public String getImpactPathsString() {
        StringBuilder impactPathsBuilder = new StringBuilder();
        impactPaths.getChildren().stream().map(path -> StringUtils.substringAfter(path.name, "://")).forEach(impactPathsBuilder::append);
        return impactPathsBuilder.toString();
    }

    public void setImpactPaths(ImpactTreeNode impactPaths) {
        this.impactPaths = impactPaths;
    }

    public void addVulnerabilityOrViolation(VulnerabilityOrViolationNode vulnerabilityOrViolation) {
        add(vulnerabilityOrViolation);
    }

    public void sortChildren() {
        children.sort((treeNode1, treeNode2) -> ((VulnerabilityOrViolationNode) treeNode2).getSeverity().ordinal() - ((VulnerabilityOrViolationNode) treeNode1).getSeverity().ordinal());
    }

    @Override
    public String getTitle() {
        return generalInfo.getComponentIdWithoutPrefix();
    }

    @Override
    public String getSubtitle() {
        return null;
    }

    @Override
    public String getIcon() {
        return getSeverity().getIconName();
    }

    @Override
    public Object clone() {
        DependencyNode newNode = (DependencyNode) super.clone();
        for (TreeNode child : children) {
            VulnerabilityOrViolationNode issue = (VulnerabilityOrViolationNode) child;
            VulnerabilityOrViolationNode clonedIssue = (VulnerabilityOrViolationNode) issue.clone();
            newNode.addVulnerabilityOrViolation(clonedIssue);
        }
        return newNode;
    }

    @Override
    public String toString() {
        return getTitle();
    }

}
