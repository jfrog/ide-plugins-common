package com.jfrog.ide.common.components;

import com.jfrog.ide.common.components.subentities.License;
import com.jfrog.ide.common.components.subentities.Severity;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.TreeNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.jfrog.ide.common.utils.Utils.removeComponentIdPrefix;

/**
 * @author yahavi
 */
public class DependencyNode extends ComparableSeverityTreeNode implements Serializable, SubtitledTreeNode {
    private static final long serialVersionUID = 1L;

    private String componentId = "";
    private boolean indirect;
    private ImpactTreeNode impactPaths;
    private final List<License> licenses;

    public DependencyNode() {
        licenses = new ArrayList<>();
    }

    @SuppressWarnings("unused")
    public String getComponentId() {
        return componentId;
    }

    @SuppressWarnings("unused")
    public DependencyNode componentId(String componentId) {
        this.componentId = componentId;
        return this;
    }

    public void setIndirect(boolean indirect) {
        this.indirect = indirect;
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
            Severity childSeverity = ((IssueNode) child).getSeverity();
            if (childSeverity.isHigherThan(severity)) {
                severity = childSeverity;
            }
        }
        return severity;
    }

    @SuppressWarnings("unused")
    public ImpactTreeNode getImpactPaths() {
        return impactPaths;
    }

    @SuppressWarnings("unused")
    public void setImpactPaths(ImpactTreeNode impactPaths) {
        this.impactPaths = impactPaths;
    }

    public void addVulnerabilityOrViolation(IssueNode vulnerabilityOrViolation) {
        add(vulnerabilityOrViolation);
    }

    public void sortChildren() {
        children.sort((treeNode1, treeNode2) -> ((IssueNode) treeNode2).getSeverity().ordinal() - ((IssueNode) treeNode1).getSeverity().ordinal());
    }

    public String getArtifactId() {
        String compIdWithoutPrefix = getComponentIdWithoutPrefix();
        int colonMatches = StringUtils.countMatches(compIdWithoutPrefix, ":");
        if (colonMatches < 1 || colonMatches > 2) {
            return "";
        }
        int indexOfColon = compIdWithoutPrefix.indexOf(":");
        if (colonMatches == 1) {
            return compIdWithoutPrefix.substring(0, indexOfColon);
        }
        return compIdWithoutPrefix.substring(indexOfColon + 1, compIdWithoutPrefix.lastIndexOf(":"));
    }

    public String getVersion() {
        String compIdWithoutPrefix = getComponentIdWithoutPrefix();
        int colonMatches = StringUtils.countMatches(compIdWithoutPrefix, ":");
        if (colonMatches < 1 || colonMatches > 2) {
            return "";
        }
        return compIdWithoutPrefix.substring(compIdWithoutPrefix.lastIndexOf(":") + 1);
    }

    public String getComponentIdWithoutPrefix() {
        return removeComponentIdPrefix(this.componentId);
    }

    @Override
    public String getTitle() {
        return getComponentIdWithoutPrefix();
    }

    @Override
    public String getSubtitle() {
        return indirect ? "(indirect)" : null;
    }

    @Override
    public String getIcon() {
        return getSeverity().getIconName();
    }

    @Override
    public Object clone() {
        DependencyNode newNode = (DependencyNode) super.clone();
        for (TreeNode child : children) {
            IssueNode issue = (IssueNode) child;
            IssueNode clonedIssue = (IssueNode) issue.clone();
            newNode.addVulnerabilityOrViolation(clonedIssue);
        }
        return newNode;
    }

    @Override
    public String toString() {
        return getTitle();
    }

}
