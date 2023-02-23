package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.*;
import com.jfrog.ide.common.nodes.subentities.Severity;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.UUID;
import java.util.Vector;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uuid")
public abstract class IssueNode extends DefaultMutableTreeNode implements SubtitledTreeNode, Comparable<IssueNode> {
    // For deserialization
    @JsonProperty()
    @SuppressWarnings("unused")
    private final String uuid = UUID.randomUUID().toString();

    public abstract Severity getSeverity();

    public String getIcon() {
        return getSeverity().getIconName();
    }

    public DependencyNode getParentArtifact() {
        TreeNode parent = getParent();
        if (!(parent instanceof DependencyNode)) {
            return null;
        }
        return (DependencyNode) parent;
    }

    @JsonGetter("children")
    public Vector<TreeNode> getChildren() {
        return children;
    }

    @JsonGetter("allowsChildren")
    public boolean getAllowsChildren() {
        return super.getAllowsChildren();
    }

    @JsonSetter("children")
    public void setChildren(Vector<DefaultMutableTreeNode> children) {
        if (children == null) {
            return;
        }
        for (DefaultMutableTreeNode child : children) {
            add(child);
        }
    }

    @Override
    public int compareTo(IssueNode o) {
        return o.getSeverity().ordinal() - this.getSeverity().ordinal();
    }
}
