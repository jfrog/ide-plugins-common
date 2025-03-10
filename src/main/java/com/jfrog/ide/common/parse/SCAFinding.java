package com.jfrog.ide.common.parse;

import com.jetbrains.qodana.sarif.model.MultiformatMessageString;
import com.jetbrains.qodana.sarif.model.ReportingDescriptor;
import com.jetbrains.qodana.sarif.model.Result;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;

import java.util.List;
import java.util.Objects;

public class SCAFinding {
    private Severity severity; // rules -> properties -> security severity
    private Applicability applicability; // result -> properties -> applicability
    private boolean isDirectDependency; // calculate from impactPaths
    private String ruleId; // rules -> ruleId or results -> ruleId
    private List<String> fixedVersions; // result -> properties -> fixedVersion
    private String shortDescription; // rule -> shortDescription -> text
    private String shortDescriptionMarkdown; // rule -> shortDescription -> markdown
    private String messageMarkdown;
    private String filePath; // invocation path + result -> location -> physicalLocation -> artifactLocation -> uri
    private String fullDescription; // rule -> help -> text
    private String fullDescriptionMarkdown; // rule -> help -> markdown
    private SourceCodeScanType reporter; // tool -> driver -> name
    private List<List<String>> impactPaths; // rule -> properties -> impactPaths

    // Qodana object
    private MultiformatMessageString help; // rule -> help (get text + markdown)

    public SCAFinding(ReportingDescriptor rule, SourceCodeScanType reporter, Result result){
        severity = null; // TODO: get severity from result
        applicability = Applicability.valueOf(Objects.requireNonNull(result.getProperties()).get("applicability").toString());
        isDirectDependency = isDirectDependency(getImpactPaths(rule), rule.getId());
    }

    private boolean isDirectDependency(List<List<String>> impactPaths, String ruleId) {
        // TODO: check if the dependency is listed in the impactPaths as a 2nd place element, where the first element is the project name
        // impactPath = {"name": "com.fasterxml.jackson.core:jackson-databind", "version": "2.15.2"}

        return impactPaths.stream().anyMatch(path -> path.size() > 1 && path.get(1).equals(ruleId));
    }

    private List<List<String>> getImpactPaths(ReportingDescriptor rule) {
        return (List<List<String>>) rule.getProperties().get("impactPaths");
    }

}
