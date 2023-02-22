package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.jfrog.ide.common.nodes.subentities.Severity;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.UUID;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uuid")
public abstract class IssueNode extends DefaultMutableTreeNode implements SubtitledTreeNode, Comparable<IssueNode> {
    // For deserialization
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

    @Override
    public int compareTo(IssueNode o) {
        return o.getSeverity().ordinal() - this.getSeverity().ordinal();
    }
}
