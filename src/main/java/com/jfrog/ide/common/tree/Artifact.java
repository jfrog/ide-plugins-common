package com.jfrog.ide.common.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.Serializable;
import java.util.List;

/**
 * @author yahavi
 */
// TODO: consider changing the name
public class Artifact extends DefaultMutableTreeNode implements Serializable, SubtitledTreeNode {

    private static final long serialVersionUID = 1L;

    private GeneralInfo generalInfo;
    private List<List<String>> impactPaths;
    private Severity topSeverity = Severity.Normal;

    // Empty constructor for serialization
    public Artifact() {
        generalInfo = new GeneralInfo();
    }

    // TODO: remove if not used
    public Artifact(GeneralInfo generalInfo) {
        this.generalInfo = generalInfo;
    }

    public Artifact(GeneralInfo generalInfo, List<List<String>> impactPaths) {
        this(generalInfo);
        this.impactPaths = impactPaths;
    }

    @SuppressWarnings("unused")
    public GeneralInfo getGeneralInfo() {
        return generalInfo;
    }

    @SuppressWarnings("unused")
    public void setGeneralInfo(GeneralInfo generalInfo) {
        this.generalInfo = generalInfo;
    }

    public Severity getTopSeverity() {
        return topSeverity;
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
        final String prefixSeparator = "://";
        int prefixIndex = generalInfo.getComponentId().indexOf(prefixSeparator);
        if (prefixIndex == -1) {
            return generalInfo.getComponentId();
        }
        return generalInfo.getComponentId().substring(prefixIndex + prefixSeparator.length());
    }

    @Override
    public String getSubtitle() {
        return null;
    }

    @Override
    public String getIcon() {
        return topSeverity.getIconName();
    }
}
