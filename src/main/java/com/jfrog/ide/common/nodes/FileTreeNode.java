package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.Severity;
import lombok.Getter;

import java.io.File;
import java.util.Objects;

public class FileTreeNode extends SortableChildrenTreeNode implements SubtitledTreeNode, Comparable<FileTreeNode> {
    @JsonProperty()
    protected String fileName = "";
    @Getter
    @JsonProperty()
    protected String filePath = "";
    @JsonProperty()
    protected Severity topSeverity = Severity.Normal;

    // Empty constructor for deserialization
    protected FileTreeNode() {
    }

    public FileTreeNode(String filePath) {
        this.filePath = filePath;
        File f = new File(filePath);
        fileName = f.getName();
    }

    public Severity getSeverity() {
        return topSeverity;
    }

    @Override
    public String getTitle() {
        return fileName;
    }

    @Override
    public String getSubtitle() {
        return filePath;
    }

    @Override
    public String toString() {
        return fileName;
    }

    @Override
    public String getIcon() {
        return topSeverity.getIconName();
    }

    public void addIssue(IssueNode issue) {
        add(issue);
        if (issue.getSeverity().isHigherThan(topSeverity)) {
            topSeverity = issue.getSeverity();
        }
    }

    @Override
    public int compareTo(FileTreeNode other) {
        return other.getSeverity().ordinal() - this.getSeverity().ordinal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileTreeNode that = (FileTreeNode) o;
        return Objects.equals(fileName, that.fileName) && Objects.equals(filePath, that.filePath) && topSeverity == that.topSeverity && Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, filePath, topSeverity, children);
    }
}
