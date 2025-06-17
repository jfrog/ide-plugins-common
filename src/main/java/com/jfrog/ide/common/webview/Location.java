package com.jfrog.ide.common.webview;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class Location implements Serializable {
    private String file;
    private String fileName;
    private String snippet;
    private int startRow;
    private int startColumn;
    private int endRow;
    private int endColumn;

    public Location() {
        this.file = "";
        this.fileName = "";
        this.snippet = "";
        this.startRow = 0;
        this.startColumn = 0;
        this.endRow = 0;
        this.endColumn = 0;
    }

    public Location(String file, String fileName, int startRow, int startColumn, int endRow, int endColumn, String snippet) {
        this.file = file;
        this.fileName = fileName;
        this.snippet = snippet;
        this.startRow = startRow;
        this.startColumn = startColumn;
        this.endRow = endRow;
        this.endColumn = endColumn;
    }

    public Location(Location other) {
        this(other.file, other.fileName, other.startRow, other.startColumn, other.endRow, other.endColumn, other.snippet);
    }
}
