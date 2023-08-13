package com.jfrog.ide.common.nodes.subentities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Objects;

@Getter
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FindingInfo that = (FindingInfo) o;
        return Objects.equals(lineSnippet, that.lineSnippet) && rowStart == that.rowStart && colStart == that.colStart && rowEnd == that.rowEnd && colEnd == that.colEnd && Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineSnippet, rowStart, colStart, rowEnd, colEnd, filePath);
    }
}
