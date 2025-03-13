package com.jfrog.ide.common.nodes;

import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.common.parse.Applicability;
import com.jfrog.ide.common.parse.ImpactPath;
import lombok.Getter;

import java.util.List;

@Getter
public class ScaIssueNode extends FileIssueNode {
    private Applicability applicability;
    private List<List<ImpactPath>> impactPaths;
    private boolean isDirectDependency;
    private String fixedVersions;

    // Empty constructor for deserialization
    ScaIssueNode() {
    }

    public ScaIssueNode(String title, String reason, Severity severity, String ruleID, Applicability applicability, List<List<ImpactPath>> impactPaths, String fixedVersions) {
        super(title,  reason,  SourceCodeScanType.SCA,  severity,  ruleID);
        this.applicability = applicability;
        this.impactPaths = impactPaths;
        this.isDirectDependency = isDirectDependency(impactPaths, ""); // TODO: implement correctly
        this.fixedVersions = fixedVersions;
    }

    private boolean isDirectDependency(List<List<ImpactPath>> impactPaths, String dependencyName) {

        return false;
    }


}
