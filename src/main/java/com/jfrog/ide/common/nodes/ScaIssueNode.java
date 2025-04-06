package com.jfrog.ide.common.nodes;

import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.common.parse.Applicability;
import com.jfrog.ide.common.nodes.subentities.ImpactPath;
import lombok.Getter;

import java.util.List;

@Getter
public class ScaIssueNode extends FileIssueNode {
    private Applicability applicability;
    private List<List<ImpactPath>> impactPaths;
    private String fixedVersions;
    private String componentName;
    private String componentVersion;
    // TODO: add isDirectDependency indication after implementing corresponding logic in cli-security

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    ScaIssueNode() {
    }

    public ScaIssueNode(String title, String reason, Severity severity, String ruleID, Applicability applicability, List<List<ImpactPath>> impactPaths, String fixedVersions, String fullDescription) {
        super(title, reason, SourceCodeScanType.SCA, severity, ruleID, fullDescription);
        this.applicability = applicability;
        this.impactPaths = impactPaths;
        this.fixedVersions = fixedVersions;
        parseComponentFromRuleId(ruleID);
    }

    private void parseComponentFromRuleId(String ruleId) {
        // The ruleId format is expected to be "CVE_componentName_componentVersion". For example: "CVE-2021-22060_org.springframework:spring-core_5.0.3.RELEASE"
        String[] componentParts = ruleId.split("_");
        if (componentParts.length != 3) {
            return;
        }
        this.componentName = componentParts[1];
        this.componentVersion = componentParts[2];
    }
}