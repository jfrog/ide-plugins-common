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
                    if (result.getLocations() == null || result.getLocations().isEmpty() || result.getLocations().get(0).getPhysicalLocation() == null) {
                        log.error("Invalid location data for result: " + result.getRuleId());
                        continue;
                    }
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
        Applicability applicability = (result.getProperties().get("applicability") != null) ? Applicability.fromSarif(result.getProperties().get("applicability").toString().toLowerCase()) : null;
        String fixedVersions = Objects.requireNonNull(result.getProperties()).get("fixedVersion").toString();
        List<List<ImpactPath>> impactPaths = new ObjectMapper().convertValue(Objects.requireNonNull(rule.getProperties()).get("impactPaths"), new TypeReference<>() {
        });
        Severity severity = Severity.fromSarif(result.getLevel().toString());
        String fullDescription = rule.getFullDescription() != null? rule.getFullDescription().getText() : null;
        String reason = result.getMessage().getText();
        String title = getTitleByScannerType(SourceCodeScanType.SCA, rule, result);

        return new ScaIssueNode(title, reason, severity, rule.getId(), applicability, impactPaths, fixedVersions, fullDescription);
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
        Region region = getFirstRegionFromResult(result);
        int rowStart = region.getStartLine();
        int colStart = region.getStartColumn();
        int rowEnd = region.getEndLine();
        int colEnd = region.getEndColumn();
        String lineSnippet = region.getSnippet().getText();
        String reason = result.getMessage().getText();
        String title = getTitleByScannerType(reporter, rule, result);
        if (reporter.equals(SourceCodeScanType.SAST)) {
            FindingInfo[][] codeFlows = convertCodeFlowsToFindingInfo(result.getCodeFlows());
            return new SastIssueNode(title, filePath, rowStart, colStart, rowEnd, colEnd, reason,
                    lineSnippet, codeFlows, severity, rule.getId(), fullDescription);
        }

        return new FileIssueNode(title, filePath, rowStart, colStart, rowEnd, colEnd, reason,
                lineSnippet, reporter, severity, rule.getId(), fullDescription);
    }

    private String getTitleByScannerType(SourceCodeScanType reporter, ReportingDescriptor rule, Result result) {
        return switch (reporter) {
            case SCA -> rule.getId().split("_")[0];
            case SAST -> rule.getShortDescription().getText();
            default -> result.getMessage().getText();
        };
    }


    /**
     * Retrieves the first region from the given SARIF result.
     * If the result has no locations, returns an empty region with an empty snippet.
     *
     * @param result the SARIF result from which to extract the first region.
     * @return the first region from the result's locations, or an empty region if no locations are present.
     */
    private Region getFirstRegionFromResult(Result result) {
        Region emptyRegion = new Region();
        emptyRegion.setSnippet(new ArtifactContent());
        return !result.getLocations().isEmpty() ? result.getLocations().get(0).getPhysicalLocation().getRegion() : emptyRegion;
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
