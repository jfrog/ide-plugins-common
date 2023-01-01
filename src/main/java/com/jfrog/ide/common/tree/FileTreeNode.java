package com.jfrog.ide.common.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;

public abstract class FileTreeNode extends DefaultMutableTreeNode implements SubtitledTreeNode {
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
}
