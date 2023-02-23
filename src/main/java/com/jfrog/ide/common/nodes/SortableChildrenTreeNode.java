package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.*;
import org.apache.commons.collections.CollectionUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Comparator;
import java.util.UUID;
import java.util.Vector;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uuid")
public class SortableChildrenTreeNode extends DefaultMutableTreeNode {
    // For deserialization
    @SuppressWarnings("unused")
    @JsonProperty()
    private final String uuid = UUID.randomUUID().toString();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sortChildren() {
        if (CollectionUtils.isNotEmpty(children)) {
            children.sort(Comparator.comparing(treeNode -> ((Comparable) treeNode)));
            children.stream()
                    .filter(treeNode -> treeNode instanceof SortableChildrenTreeNode)
                    .forEach(treeNode -> ((SortableChildrenTreeNode) treeNode).sortChildren());
        }
    }

    @JsonGetter()
    public Vector<TreeNode> getChildren() {
        return children;
    }

    @JsonGetter()
    public boolean getAllowsChildren() {
        return super.getAllowsChildren();
    }

    @JsonSetter()
    public void setChildren(Vector<DefaultMutableTreeNode> children) {
        if (children == null) {
            return;
        }
        for (DefaultMutableTreeNode child : children) {
            add(child);
        }
    }
}
