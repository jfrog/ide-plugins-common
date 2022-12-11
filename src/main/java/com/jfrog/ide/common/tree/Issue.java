package com.jfrog.ide.common.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * @author yahavi
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Issue extends IssueOrLicense implements Comparable<Issue> {

    private Severity severity = Severity.Normal;
    private List<String> fixedVersions;
    private List<String> references;
    private String component = "";
    private String ignoreRuleUrl;
    private Cve cve;
    private String summary;
    private String issueId;

    public Issue() {
    }

    @SuppressWarnings("unused")
    public Issue(String issueId, Severity severity, String summary, List<String> fixedVersions, Cve cve,
                 List<String> references, String ignoreRuleUrl) {
        this.issueId = issueId;
        this.severity = severity;
        this.summary = summary;
        this.fixedVersions = fixedVersions;
        this.cve = cve;
        this.references = references;
        this.ignoreRuleUrl = ignoreRuleUrl;
    }

    public String getIssueId() {
        return this.issueId;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public String getComponent() {
        return this.component;
    }

    public void setComponent(String component) {
        this.component = component;
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

    @SuppressWarnings("unused")
    public List<String> getReferences() {
        return references;
    }

    @SuppressWarnings("unused")
    public String getIgnoreRuleUrl() {
        return ignoreRuleUrl;
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
    public int compareTo(@Nonnull Issue otherIssue) {
        return Integer.compare(hashCode(), Objects.hashCode(otherIssue));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Issue)) {
            return false;
        }
        return StringUtils.equals(((Issue) other).getIssueId(), getIssueId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueId);
    }

    @Override
    public String getTitle() {
        String title = cve != null && cve.getCveId() != null && !cve.getCveId().isEmpty() ? cve.getCveId() : issueId;
        return title + "";
    }

    @Override
    public String getSubtitle() {
        // TODO: return null or something else
        return null;
    }
}
