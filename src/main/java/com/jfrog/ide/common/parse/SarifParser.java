package com.jfrog.ide.common.parse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jetbrains.qodana.sarif.model.*;
import com.jfrog.ide.common.nodes.FileIssueNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.SastIssueNode;
import com.jfrog.ide.common.nodes.ScaIssueNode;
import com.jfrog.ide.common.nodes.subentities.FindingInfo;
import com.jfrog.ide.common.nodes.subentities.ImpactPath;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import org.jfrog.build.api.util.Log;
import com.jetbrains.qodana.sarif.SarifUtil;

import java.io.*;
import java.util.*;

/**
 * SarifParser is responsible for parsing SARIF reports and converting them into a list of FileTreeNode objects.
 */
public class SarifParser {
    private final Log log;

    /**
     * Constructor for SarifParser.
     *
     * @param log the logger to be used for logging errors and information.
     */
    SarifParser(Log log) {
        this.log = log;
    }

    /**
     * Parses the given SARIF report output and returns a list of FileTreeNode objects.
     *
     * @param output the SARIF report as a string.
     * @return a list of FileTreeNode objects representing the parsed findings.
     * @throws NoSuchElementException if no runs are found in the SARIF report.
     */
    List<FileTreeNode> parse (String output) {
        Reader reader = new StringReader(output);
        SarifReport report = SarifUtil.readReport(reader);
        List<Run> runs = report.getRuns();
        if (runs == null || runs.isEmpty()) {
            log.error("No runs found in SARIF report");
            throw new NoSuchElementException("No runs found in the scan SARIF report");
        }
        return new ArrayList<>(parseScanFindings(runs));
    }

    /**
     * Parses the scan findings from the given list of runs.
     *
     * @param runs the list of runs from the SARIF report.
     * @return a list of FileTreeNode objects representing the parsed findings.
     */
    private List<FileTreeNode> parseScanFindings(List<Run> runs){
        List<FileTreeNode> fileTreeNodes = new ArrayList<>();

        for (Run run : runs) {
            HashMap<String, FileTreeNode> resultsMap = new HashMap<>();
            List<Result> resultsList = run.getResults();
            // get the scanner tool name with characters only
            String sourceCodeToolName = run.getTool().getDriver().getName().replaceAll("[^a-zA-Z\\s]", "").trim();
            SourceCodeScanType reporter = SourceCodeScanType.fromParam(sourceCodeToolName);

            for (Result result : resultsList){
                ReportingDescriptor rule = run.getTool().getDriver().getRules().stream()
                        .filter(r -> r.getId().equals(result.getRuleId()))
                        .findFirst()
                        .orElse(null);

                if (rule == null) {
                    log.error("Rule not found for result: " + result.getRuleId());
                } else {
                    String filePath = result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri();

                    // Create FileTreeNodes for files with found issues
                    FileTreeNode fileNode = resultsMap.get(filePath);
                    if (fileNode == null) {
                        fileNode = new FileTreeNode(filePath);
                        resultsMap.put(filePath, fileNode);
                    }

                    if (reporter.equals(SourceCodeScanType.SCA)){
                        fileNode.addIssue(generateScaFileIssueNode(rule, result));
                    } else {
                        fileNode.addIssue(generateJasFileIssueNode(rule, result, reporter, filePath));
                    }
                }
            }
            fileTreeNodes.addAll(resultsMap.values());
        }
        return fileTreeNodes;
    }

    /**
     * Generates a ScaIssueNode from the given rule and result.
     *
     * @param rule   the reporting descriptor rule.
     * @param result the result from the SARIF report.
     * @return a ScaIssueNode representing the issue.
     */
    private FileIssueNode generateScaFileIssueNode(ReportingDescriptor rule, Result result){
        Applicability applicability = Applicability.fromSarif(Objects.requireNonNull(result.getProperties()).get("applicability").toString());
        String fixedVersions = Objects.requireNonNull(result.getProperties()).get("fixedVersion").toString();
        List<List<ImpactPath>> impactPaths = new ObjectMapper().convertValue(Objects.requireNonNull(rule.getProperties()).get("impactPaths"), new TypeReference<>() {
        });
        Severity severity = Severity.fromSarif(result.getLevel().toString());
        String fullDescription = rule.getFullDescription() != null? rule.getFullDescription().getText() : null;
        String reason = result.getMessage().getText();

        return new ScaIssueNode(SourceCodeScanType.SCA.getScannerIssueTitle(), reason, severity, rule.getId(), applicability, impactPaths, fixedVersions, fullDescription);
    }

    /**
     * Generates a FileIssueNode for JAS from the given rule, result, reporter, and file path.
     *
     * @param rule      the reporting descriptor rule.
     * @param result    the result from the SARIF report.
     * @param reporter  the source code scan type.
     * @param filePath  the file path where the issue was found.
     * @return a FileIssueNode representing the issue.
     */
    private FileIssueNode generateJasFileIssueNode(ReportingDescriptor rule, Result result, SourceCodeScanType reporter, String filePath){
        Severity severity = Severity.fromSarif(result.getLevel().toString());
        String fullDescription = rule.getFullDescription().getText();
        int rowStart = result.getLocations().get(0).getPhysicalLocation().getRegion().getStartLine();
        int colStart = result.getLocations().get(0).getPhysicalLocation().getRegion().getStartColumn();
        int rowEnd = result.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine();
        int colEnd = result.getLocations().get(0).getPhysicalLocation().getRegion().getEndColumn();
        String lineSnippet = result.getLocations().get(0).getPhysicalLocation().getRegion().getSnippet().getText();
        String reason = result.getMessage().getText();
        if (reporter.equals(SourceCodeScanType.SAST)) {
            FindingInfo[][] codeFlows = convertCodeFlowsToFindingInfo(result.getCodeFlows());
            return new SastIssueNode(reporter.getScannerIssueTitle(), filePath, rowStart, colStart, rowEnd, colEnd, reason,
                    lineSnippet, codeFlows, severity, rule.getId(), fullDescription);
        }

        return new FileIssueNode(reporter.getScannerIssueTitle(), filePath, rowStart, colStart, rowEnd, colEnd, reason,
                lineSnippet, reporter, severity, rule.getId(), fullDescription);
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
                    location.getArtifactLocation().getUri(),
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
}
