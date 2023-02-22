package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.apache.commons.collections.CollectionUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;
import java.util.UUID;
import java.util.Vector;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uuid")
public class SortableChildrenTreeNode extends DefaultMutableTreeNode {
    // For deserialization
    @SuppressWarnings("unused")
    private String uuid = UUID.randomUUID().toString();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sortChildren() {
        if (CollectionUtils.isNotEmpty(children)) {
            children.sort(Comparator.comparing(treeNode -> ((Comparable) treeNode)));
            children.stream()
                    .filter(treeNode -> treeNode instanceof SortableChildrenTreeNode)
                    .forEach(treeNode -> ((SortableChildrenTreeNode) treeNode).sortChildren());
        }
    }

    @JsonSetter("children")
    private void setChildren(Vector<DefaultMutableTreeNode> children) {
        if (children == null) {
            return;
        }
        for (DefaultMutableTreeNode child : children) {
            add(child);
        }
    }
}
