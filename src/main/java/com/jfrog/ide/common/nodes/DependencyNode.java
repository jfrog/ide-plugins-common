package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.ImpactTreeNode;
import com.jfrog.ide.common.nodes.subentities.License;
import com.jfrog.ide.common.nodes.subentities.Severity;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;

import static com.jfrog.ide.common.utils.Utils.removeComponentIdPrefix;

public class DependencyNode extends SortableChildrenTreeNode implements SubtitledTreeNode, Comparable<DependencyNode> {
    /**
     * Xray component ID, including a package manager prefix (like npm://)
     */
    @JsonProperty()
    private String componentId = "";
    @JsonProperty()
    private boolean indirect;
    @JsonProperty()
    private ImpactTreeNode impactPaths;
    @JsonProperty()
    private List<License> licenses;

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

    @SuppressWarnings("unused")
    public void setIndirect(boolean indirect) {
        this.indirect = indirect;
    }

    @JsonGetter()
    @SuppressWarnings("unused")
    public boolean isIndirect() {
        return indirect;
    }

    @JsonGetter()
    public List<License> getLicenses() {
        return licenses;
    }

    public void addLicense(License license) {
        licenses.add(license);
    }

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

    @JsonGetter()
    @SuppressWarnings("unused")
    public ImpactTreeNode getImpactPaths() {
        return impactPaths;
    }

    @SuppressWarnings("unused")
    public void setImpactPaths(ImpactTreeNode impactPaths) {
        this.impactPaths = impactPaths;
    }

    public void addIssue(IssueNode issue) {
        add(issue);
    }

    @SuppressWarnings("unused")
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
    public int compareTo(DependencyNode other) {
        return other.getSeverity().ordinal() - this.getSeverity().ordinal();
    }

    @Override
    public Object clone() {
        DependencyNode newNode = (DependencyNode) super.clone();
        for (TreeNode child : children) {
            IssueNode issue = (IssueNode) child;
            IssueNode clonedIssue = (IssueNode) issue.clone();
            newNode.addIssue(clonedIssue);
        }
        return newNode;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
