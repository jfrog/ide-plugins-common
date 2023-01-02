package com.jfrog.ide.common.tree;

import org.apache.commons.collections.CollectionUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;

public class FileTreeNode extends DefaultMutableTreeNode implements SubtitledTreeNode {
    protected String fileName;
    protected String filePath;
    protected Severity topSeverity = Severity.Normal;

    public FileTreeNode(String filePath) {
        this.filePath = filePath;
        File f = new File(filePath);
        fileName = f.getName();
    }

    public Severity getTopSeverity() {
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

    public void addDependency(ApplicableIssueNode issue) {
        add(issue);
        if (issue.getSeverity().isHigherThan(topSeverity)) {
            topSeverity = issue.getSeverity();
        }
    }

    public void sortChildren() {
        if (CollectionUtils.isNotEmpty(children)) {
            children.sort((treeNode1, treeNode2) -> ((DependencyNode) treeNode2).getTopSeverity().ordinal() - ((DependencyNode) treeNode1).getTopSeverity().ordinal());
        }
    }
}
