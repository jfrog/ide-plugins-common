package com.jfrog.ide.common.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.jfrog.ide.common.tree.Severity.getNotApplicableSeverity;

/**
 * @author yahavi
 */
public class IssueNode extends VulnerabilityOrViolationNode {

    private String ignoreRuleUrl;
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
    private List<ApplicableIssueNode> applicableIssues;

    @SuppressWarnings("unused")
    public IssueNode() {
    }

    @SuppressWarnings("unused")
    public IssueNode(String issueId, Severity severity, String summary, List<String> fixedVersions, List<String> infectedVersions,
                     Cve cve, String lastUpdated, List<String> watchNames, List<String> references, ResearchInfo researchInfo, String ignoreRuleUrl) {
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
        this.ignoreRuleUrl = ignoreRuleUrl;
    }

    public String getIssueId() {
        return this.issueId;
    }

    public Severity getSeverity() {
        return isApplicable() != null && !isApplicable() ? getNotApplicableSeverity(severity): severity;
    }

    public Severity getSeverity(boolean masked) {
        if (!masked) {
            return this.severity;
        }
        return getSeverity();
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
    @SuppressWarnings("unused")
    public boolean isTopSeverity() {
        return getSeverity() == Severity.Critical;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IssueNode)) {
            return false;
        }
        IssueNode otherIssue = (IssueNode) other;
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

    public List<ApplicableIssueNode> getApplicableIssues() {
        return applicableIssues;
    }

    public void setApplicableIssues(List<ApplicableIssueNode> applicableIssues) {
        this.applicableIssues = applicableIssues;
    }

    public void AddApplicableIssues(ApplicableIssueNode issue) {
        this.applicableIssues = this.applicableIssues == null ? new ArrayList<>() : this.applicableIssues;
        this.applicableIssues.add(issue);
    }

    /**
     * Returns true if the issue is applicable, false if not.
     * If the applicability status is unknown, null will be returned.
     */
    public Boolean isApplicable() {
        return this.applicableIssues != null ? this.applicableIssues.size() > 0 : null;
    }

    @SuppressWarnings("unused")
    public String getIgnoreRuleUrl() {
        return ignoreRuleUrl;
    }

}
