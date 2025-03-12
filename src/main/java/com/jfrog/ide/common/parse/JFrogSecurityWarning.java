package com.jfrog.ide.common.parse;

import com.jetbrains.qodana.sarif.model.*;
import com.jfrog.ide.common.nodes.subentities.FindingInfo;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import lombok.Getter;

import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Getter
public class JFrogSecurityWarning {
    private final int lineStart;
    private final int colStart;
    private final int lineEnd;
    private final int colEnd;
    private final String ruleID; // common
    private final String filePath; // common
    private final Severity severity; // common
    private final SourceCodeScanType reporter; // common
    private final String reason;
    private final String lineSnippet;
    private String scannerSearchTarget;
    private final FindingInfo[][] codeFlows;
    private final boolean isApplicable;

    public JFrogSecurityWarning(
            int lineStart,
            int colStart, int lineEnd,
            int colEnd, String reason,
            String filePath,
            String ruleID,
            String lineSnippet,
            SourceCodeScanType reporter,
            boolean isApplicable,
            Severity severity,
            FindingInfo[][] codeFlows
    ) {
        this.lineStart = lineStart;
        this.colStart = colStart;
        this.lineEnd = lineEnd;
        this.colEnd = colEnd;
        this.reason = reason;
        this.filePath = filePath;
        this.ruleID = ruleID;
        this.lineSnippet = lineSnippet;
        this.reporter = reporter;
        this.isApplicable = isApplicable;
        this.severity = severity;
        this.codeFlows = codeFlows;
    }
    @SuppressWarnings("unused")
    public JFrogSecurityWarning(Result result, SourceCodeScanType reporter, ReportingDescriptor rule) {
        this(getFirstRegion(result) != null ? getFirstRegion(result).getStartLine() - 1 : 0,
                getFirstRegion(result) != null ? getFirstRegion(result).getStartColumn() - 1 : 0,
                getFirstRegion(result) != null ? getFirstRegion(result).getEndLine() - 1 : 0,
                getFirstRegion(result) != null ? getFirstRegion(result).getEndColumn() - 1 : 0,
                result.getMessage().getText(),
                getFilePath(result),
                result.getRuleId(),
                getFirstRegion(result).getSnippet().getText(),
                reporter,
                getApplicabilityFromResult(result, rule),
                Severity.fromSarif(result.getLevel().toString()),
                convertCodeFlowsToFindingInfo(result.getCodeFlows())
        );
    }

    private static boolean getApplicabilityFromResult(Result result,PropertyOwner rule){
        return !result.getKind().equals(Result.Kind.PASS) && (Objects.requireNonNull(rule.getProperties()).get("applicability").equals("applicable"));
    }

    private static String getFilePath(Result result){
        return !result.getLocations().isEmpty() ? uriToPath(result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri()) : "";
    }

    private static FindingInfo[][] convertCodeFlowsToFindingInfo(List<CodeFlow> codeFlows) {
        if (codeFlows == null || codeFlows.isEmpty()) {
            return null;
        }
        List<ThreadFlow> flows = codeFlows.get(0).getThreadFlows();
        if (flows == null || flows.isEmpty()) {
            return null;
        }
        FindingInfo[][] results = new FindingInfo[flows.size()][];
        for (int i = 0; i < flows.size(); i++) {
            ThreadFlow flow = flows.get(i);
            List<ThreadFlowLocation> locations = flow.getLocations();
            results[i] = new FindingInfo[locations.size()];
            for (int j = 0; j < locations.size(); j++) {
                PhysicalLocation location = locations.get(j).getLocation().getPhysicalLocation();
                results[i][j] = new FindingInfo(
                        uriToPath(location.getArtifactLocation().getUri()),
                        location.getRegion().getStartLine(),
                        location.getRegion().getStartColumn(),
                        location.getRegion().getEndLine(),
                        location.getRegion().getEndColumn(),
                        location.getRegion().getSnippet().getText()
                );
            }
        }
        return results;
    }

    public boolean isApplicable() {
        return this.isApplicable;
    }

    private static Region getFirstRegion(Result result) {
        Region emptyRegion = new Region();
        emptyRegion.setMessage(new Message());
        return !result.getLocations().isEmpty() ? result.getLocations().get(0).getPhysicalLocation().getRegion() : emptyRegion;
    }
    @SuppressWarnings("unused")
    public void setScannerSearchTarget(String scannerSearchTarget) {
        this.scannerSearchTarget = scannerSearchTarget;
    }

    private static String uriToPath(String path) {
        return Paths.get(URI.create(path)).toString();
    }
}