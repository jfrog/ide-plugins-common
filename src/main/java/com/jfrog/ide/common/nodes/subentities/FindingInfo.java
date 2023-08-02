package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FindingInfo {
    @JsonProperty()
    private String lineSnippet;
    @JsonProperty()
    private int rowStart;
    @JsonProperty()
    private int colStart;
    @JsonProperty()
    private int rowEnd;
    @JsonProperty()
    private int colEnd;
    @JsonProperty()
    private String filePath;

    public FindingInfo() {
    }

    public FindingInfo(String filePath, int rowStart, int colStart, int rowEnd, int colEnd, String lineSnippet) {
        this.filePath = filePath;
        this.rowStart = rowStart;
        this.colStart = colStart;
        this.rowEnd = rowEnd;
        this.colEnd = colEnd;
        this.lineSnippet = lineSnippet;
    }

    public String getLineSnippet() {
        return lineSnippet;
    }

    public int getRowStart() {
        return rowStart;
    }

    public int getColStart() {
        return colStart;
    }

    public int getRowEnd() {
        return rowEnd;
    }

    public int getColEnd() {
        return colEnd;
    }

    public String getFilePath() {
        return filePath;
    }
}
