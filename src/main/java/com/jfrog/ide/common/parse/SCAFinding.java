package com.jfrog.ide.common.parse;

import com.jetbrains.qodana.sarif.model.Message;
import com.jetbrains.qodana.sarif.model.MultiformatMessageString;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;

import java.util.List;

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


    private boolean isDirectDependency(List<List<String>> impactPaths, String dependencyName) {
        return impactPaths.stream().anyMatch(path -> path.size() > 1 && path.get(1).equals(dependencyName));
    }
}
