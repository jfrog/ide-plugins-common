package com.jfrog.ide.common.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yahavi
 */
public class Artifact extends DefaultMutableTreeNode implements Serializable, SubtitledTreeNode {

    private static final long serialVersionUID = 1L;

    private GeneralInfo generalInfo;
    private ImpactTreeNode impactPaths;
    private Severity topSeverity = Severity.Normal;
    private List<License> licenses;

    // Empty constructor for serialization
    public Artifact() {
        generalInfo = new GeneralInfo();
    }

    public Artifact(GeneralInfo generalInfo) {
        this.generalInfo = generalInfo;
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
        if (licenses == null) {
            licenses = new ArrayList<>();
        }
        licenses.add(license);
    }

    public Severity getTopSeverity() {
        return topSeverity;
    }

    public ImpactTreeNode getImpactPaths() {
        return impactPaths;
    }

    public void setImpactPaths(ImpactTreeNode impactPaths) {
        this.impactPaths = impactPaths;
    }

    public void addVulnerabilityOrViolation(VulnerabilityOrViolation vulnerabilityOrViolation) {
        add(vulnerabilityOrViolation);
        if (vulnerabilityOrViolation.getSeverity().isHigherThan(topSeverity)) {
            topSeverity = vulnerabilityOrViolation.getSeverity();
        }
    }

    public void sortChildren() {
        children.sort((treeNode1, treeNode2) -> ((VulnerabilityOrViolation) treeNode2).getSeverity().ordinal() - ((VulnerabilityOrViolation) treeNode1).getSeverity().ordinal());
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
        return topSeverity.getIconName();
    }

    @Override
    public Object clone() {
        Artifact newNode = (Artifact) super.clone();
        for (TreeNode child : children) {
            VulnerabilityOrViolation issue = (VulnerabilityOrViolation) child;
            VulnerabilityOrViolation clonedIssue = (VulnerabilityOrViolation) issue.clone();
            newNode.addVulnerabilityOrViolation(clonedIssue);
        }
        return newNode;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
