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
    private boolean isDirectDependency;
    private String fixedVersions;

    // Empty constructor for deserialization
    @SuppressWarnings("unused")
    ScaIssueNode() {
    }

    public ScaIssueNode(String title, String reason, Severity severity, String ruleID, Applicability applicability, List<List<ImpactPath>> impactPaths, String fixedVersions, String fullDescription) {
        super(title,  reason,  SourceCodeScanType.SCA,  severity,  ruleID, fullDescription);
        this.applicability = applicability;
        this.impactPaths = impactPaths;
        this.isDirectDependency = isDirectDependency(impactPaths, ruleID);
        this.fixedVersions = fixedVersions;
    }

    /**
     * Determines if the issue is a direct dependency.
     *
     * @param impactPaths  The impact paths of the violated rule.
     * @param ruleId       The rule ID associated with the issue.
     * @return true if the issue is a direct dependency, false otherwise.
     */
    private boolean isDirectDependency(List<List<ImpactPath>> impactPaths, String ruleId) {
        String dependencyName = ruleId.split("_")[1];
        String dependencyVersion = ruleId.split("_")[2];
        ImpactPath directDependency = new ImpactPath(dependencyName, dependencyVersion);

        for (List<ImpactPath> impactPathsList : impactPaths) {
            if (impactPathsList.get(1).equals(directDependency)) {
                return true; // TODO: validate that the first element is the root project
            }
        }
        return false;
    }
}
