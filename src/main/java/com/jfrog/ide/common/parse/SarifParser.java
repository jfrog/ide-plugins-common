package com.jfrog.ide.common.parse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jetbrains.qodana.sarif.model.*;
import com.jfrog.ide.common.nodes.FileIssueNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.ScaIssueNode;
import com.jfrog.ide.common.nodes.subentities.ImpactPath;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import org.jfrog.build.api.util.Log;
import com.jetbrains.qodana.sarif.SarifUtil;
import org.jfrog.build.api.util.NullLog;

import java.io.*;
import java.util.*;

public class SarifParser {
    private final Log log;

    SarifParser(Log log) {
        this.log = log;
    }

    List<FileTreeNode> parse (String output) {
        Reader reader = new StringReader(output);

        SarifReport report = SarifUtil.readReport(reader);
        // extract SCA run object from SARIF

        List<Run> runs = report.getRuns();
        if (runs.isEmpty()) {
            log.error("No runs found in SARIF report");
            throw new NoSuchElementException("No runs found in the scan SARIF report");
        }

        return new ArrayList<>(parseScanFindings(runs));
    }


    private List<FileTreeNode> parseScanFindings(List<Run> runs){
        // a method for parsing SCA findings and build FileTreeNodes and IssueNodes
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


    private FileIssueNode generateScaFileIssueNode(ReportingDescriptor rule, Result result){
        Applicability applicability = Applicability.fromSarif(Objects.requireNonNull(result.getProperties()).get("applicability").toString());
        String fixedVersions = Objects.requireNonNull(result.getProperties()).get("fixedVersion").toString();
        List<List<ImpactPath>> impactPaths = new ObjectMapper().convertValue(Objects.requireNonNull(rule.getProperties()).get("impactPaths"), new TypeReference<>() {
        });
        Severity severity = Severity.fromSarif(result.getLevel().toString());
        String fullDescription = rule.getFullDescription().getText();

        return new ScaIssueNode(SourceCodeScanType.SCA.getScannerIssueTitle(), fullDescription, severity, rule.getId(), applicability, impactPaths, fixedVersions);
    }

    private FileIssueNode generateJasFileIssueNode(ReportingDescriptor rule, Result result, SourceCodeScanType reporter, String filePath){
        Severity severity = Severity.fromSarif(result.getLevel().toString());
        String fullDescription = rule.getFullDescription().getText(); // TODO: is needed?
        int rowStart = result.getLocations().get(0).getPhysicalLocation().getRegion().getStartLine();
        int colStart = result.getLocations().get(0).getPhysicalLocation().getRegion().getStartColumn();
        int rowEnd = result.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine();
        int colEnd = result.getLocations().get(0).getPhysicalLocation().getRegion().getEndColumn();
        String lineSnippet = result.getLocations().get(0).getPhysicalLocation().getRegion().getSnippet().getText();
        String reason = result.getMessage().getText();

        return new FileIssueNode(reporter.getScannerIssueTitle(), filePath, rowStart, colStart, rowEnd, colEnd, reason,
                lineSnippet, reporter, severity, rule.getId());
    }


    public static void main(String[] args) {
        // read results from json file
        String jsonFilePath = "C:\\Users\\Keren Reshef\\Projects\\jfrog-cli-security\\tests\\testdata\\projects\\jas\\jas\\results.json";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(new FileInputStream(jsonFilePath));
            String outputJson = jsonNode.toString();
            SarifParser sarifParser = new SarifParser(new NullLog());
            List<FileTreeNode> results = sarifParser.parse(outputJson);
            results.forEach(result -> System.out.println(result.toString()));
        } catch (IOException e) {
            System.out.println("Failed to read JSON file" + e.getMessage());
        }
    }
}
