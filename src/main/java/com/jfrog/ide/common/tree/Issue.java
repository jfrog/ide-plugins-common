package com.jfrog.ide.common.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author yahavi
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Issue extends VulnerabilityOrViolation {

    private Severity severity = Severity.Normal;
    private List<String> fixedVersions;
    private List<String> infectedVersions;
    private List<String> references;
    private Cve cve;
    private String summary;
    private String issueId;
    private String lastUpdated;
    private List<String> watchNames;
    private ResearchInfo researchInfo;

    public Issue() {
    }

    @SuppressWarnings("unused")
    public Issue(String issueId, Severity severity, String summary, List<String> fixedVersions, List<String> infectedVersions,
                 Cve cve, String lastUpdated, List<String> watchNames, List<String> references, ResearchInfo researchInfo) {
        this.issueId = issueId;
        this.severity = severity;
        this.summary = summary;
        this.fixedVersions = fixedVersions;
        this.infectedVersions = infectedVersions;
        this.cve = cve;
        this.lastUpdated = lastUpdated;
        this.watchNames = watchNames;
        this.references = references;
        this.researchInfo = researchInfo;
    }

    public String getIssueId() {
        return this.issueId;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    @SuppressWarnings("unused")
    public String getSummary() {
        return summary;
    }

    @SuppressWarnings("unused")
    public List<String> getFixedVersions() {
        return fixedVersions;
    }

    @SuppressWarnings("unused")
    public void setFixedVersions(List<String> fixedVersions) {
        this.fixedVersions = fixedVersions;
    }

    public List<String> getInfectedVersions() {
        return infectedVersions;
    }

    @SuppressWarnings("unused")
    public List<String> getReferences() {
        return references;
    }

    public Cve getCve() {
        return cve;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public List<String> getWatchNames() {
        return watchNames;
    }

    @SuppressWarnings("unused")
    public ResearchInfo getResearchInfo() {
        return researchInfo;
    }

    @JsonIgnore
    @SuppressWarnings("WeakerAccess")
    public boolean isTopSeverity() {
        return getSeverity() == Severity.Critical;
    }

    @JsonIgnore
    public boolean isHigherSeverityThan(Issue o) {
        return getSeverity().isHigherThan(o.getSeverity());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Issue)) {
            return false;
        }
        Issue otherIssue = (Issue) other;
        return StringUtils.equals(otherIssue.getIssueId(), getIssueId()) &&
                StringUtils.equals(otherIssue.getCveIdOrEmpty(), getCveIdOrEmpty());
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueId, cve);
    }

    @Override
    public String getTitle() {
        return StringUtils.firstNonBlank(getCveIdOrEmpty(), issueId);
    }

    @Override
    public String getSubtitle() {
        return null;
    }

    @Override
    public String toString() {
        return getTitle();
    }

    private String getCveIdOrEmpty() {
        if (cve == null) {
            return "";
        }
        return cve.getCveId();
    }
}
