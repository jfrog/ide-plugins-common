package com.jfrog.ide.common.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.Serializable;
import java.util.List;

/**
 * @author yahavi
 */
// TODO: consider changing the name
public class Artifact extends DefaultMutableTreeNode implements Serializable, SubtitledTreeNode {

    private static final long serialVersionUID = 1L;

    private GeneralInfo generalInfo;
    private ImpactTreeNode impactPaths;
    private Severity topSeverity = Severity.Normal;
    private String licenseName;

    // Empty constructor for serialization
    public Artifact() {
        generalInfo = new GeneralInfo();
    }

    // TODO: remove if not used
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

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
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

    public void addIssueOrLicense(IssueOrLicense issueOrLicense) {
        add(issueOrLicense);
        if (issueOrLicense.getSeverity().isHigherThan(topSeverity)) {
            topSeverity = issueOrLicense.getSeverity();
        }
    }

    public void sortChildren() {
        children.sort((treeNode1, treeNode2) -> ((IssueOrLicense) treeNode2).getSeverity().ordinal() - ((IssueOrLicense) treeNode1).getSeverity().ordinal());
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
        Artifact newNode = (Artifact)super.clone();
        for (TreeNode child : children) {
            IssueOrLicense issue = (IssueOrLicense) child;
            IssueOrLicense clonedIssue = (IssueOrLicense) issue.clone();
            newNode.addIssueOrLicense(clonedIssue);
        }
        return newNode;
    }
}
