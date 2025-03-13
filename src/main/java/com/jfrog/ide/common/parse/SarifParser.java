package com.jfrog.ide.common.parse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jetbrains.qodana.sarif.model.*;
import com.jfrog.ide.common.nodes.FileIssueNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.ScaIssueNode;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import org.jfrog.build.api.util.Log;
import com.jetbrains.qodana.sarif.SarifUtil;
import org.jfrog.build.api.util.NullLog;

import java.io.*;
import java.util.*;

public class SarifParser {
    private Log log;

    SarifParser(Log log) {
        this.log = log;
    }

    List<FileTreeNode> parse (String output) {
        Reader reader = new StringReader(output);
        List<FileTreeNode> fileTreeNodes = new ArrayList<>();

        SarifReport report = SarifUtil.readReport(reader);
        // extract SCA run object from SARIF

        List<Run> SCARuns = report.getRuns().stream().
                filter(run -> run.getTool().getDriver().getName().contains(SourceCodeScanType.SCA.getParam()))
                .toList();
        if (SCARuns.isEmpty()) {
            log.error("SCA run not found in SARIF report");
            throw new NoSuchElementException("SCA run not found in SARIF report");
        }
        fileTreeNodes.addAll(parseSCAFindings(SCARuns));

        List<Run> JASRuns = report.getRuns().stream().filter(run -> !SCARuns.contains(run)).toList();

        fileTreeNodes.addAll(parseJASFindings(JASRuns));

        return fileTreeNodes;
    }

    private List<FileTreeNode> parseSCAFindings(List<Run> SCARuns){
        // a method for parsing SCA findings and build FileTreeNodes and IssueNodes
        List<FileTreeNode> scaFileTreeNodes = new ArrayList<>();

        for (Run SCARun : SCARuns) {
            // TODO: invocations is a list. we will fetch the path from result->locations->physicalLocation->artifactLocation->uri
            HashMap<String, FileTreeNode> resultsMap = new HashMap<>();
            List<Result> resultsList = SCARun.getResults();

            for (Result result : resultsList){
                String filePath = result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri();
                Severity severity = Severity.fromSarif(result.getLevel().toString());
                Applicability applicability = Applicability.fromSarif(Objects.requireNonNull(result.getProperties()).get("applicability").toString());

                ReportingDescriptor rule = SCARun.getTool().getDriver().getRules().stream()
                        .filter(r -> r.getId().equals(result.getRuleId()))
                        .findFirst()
                        .orElse(null);

                String fixedVersions = result.getProperties().get("fixedVersion").toString();

                if (rule == null) {
                    log.error("Rule not found for result: " + result.getRuleId());
                } else {
                    List<List<ImpactPath>> impactPaths = new ObjectMapper().convertValue(Objects.requireNonNull(rule.getProperties()).get("impactPaths"), new TypeReference<>() {
                    });

                    // Create FileTreeNodes for files with found issues
                    FileTreeNode fileNode = resultsMap.get(filePath);
                    if (fileNode == null) {
                        fileNode = new FileTreeNode(filePath);
                        resultsMap.put(filePath, fileNode);
                    }
                    String fullDescription = rule.getFullDescription().getText();

                    // params: String title, String reason, Severity severity, String ruleID, Applicability applicability, List<List<String>> impactPaths, boolean isDirectDependency, List<String> fixedVersions
                    ScaIssueNode scaIssueNode = new ScaIssueNode(SourceCodeScanType.SCA.getParam(), fullDescription, severity, rule.getId(), applicability, impactPaths, fixedVersions);

                    fileNode.addIssue(scaIssueNode);
                }
            }

            scaFileTreeNodes.addAll(resultsMap.values());
        }
        return scaFileTreeNodes;
    }

    private List<FileTreeNode> parseJASFindings(List<Run> JASRuns) {
        // a method for parsing SCA findings and build FileTreeNodes and IssueNodes
        List<FileTreeNode> jasFileTreeNodes = new ArrayList<>();

        for (Run JASRun : JASRuns) {
            List<Result> resultsList = JASRun.getResults();
            HashMap<String, FileTreeNode> resultsMap = new HashMap<>();
            String sourceCodeToolName = JASRun.getTool().getDriver().getName();
            SourceCodeScanType reporter = SourceCodeScanType.fromParam(sourceCodeToolName); // TODO: handle SAST frog emoji in tool name

            for (Result result : resultsList) {
                ReportingDescriptor rule = JASRun.getTool().getDriver().getRules().stream()
                        .filter(r -> r.getId().equals(result.getRuleId()))
                        .findFirst()
                        .orElse(null);

                if (rule == null) {
                    log.error("Rule not found for result: " + result.getRuleId());
                } else {
                    String filePath = result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri();

                    FileTreeNode fileNode = resultsMap.get(filePath);
                    if (fileNode == null) {
                        fileNode = new FileTreeNode(filePath);
                        resultsMap.put(filePath, fileNode);
                    }

                    Severity severity = Severity.fromSarif(result.getLevel().toString());
                    // params for constructor: String title, String reason, SourceCodeScanType reportType, Severity severity, String ruleID
                    FileIssueNode issueNode = new FileIssueNode(reporter.getParam(), rule.getShortDescription().getText(),
                            reporter, severity, rule.getId()); // TODO: add location info

                    fileNode.addIssue(issueNode);
                }
            }
            jasFileTreeNodes.addAll(resultsMap.values());
        }
        return jasFileTreeNodes;
    }

//    private List<FileTreeNode> createSpecificFileIssueNodes(Run run, SourceCodeScanType reporter) {
//        HashMap<String, FileTreeNode> results = new HashMap<>();
//        List<Result> resultsList = run.getResults();
//
//        for (Result result : resultsList){
//            String filePath = result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri();
//            Severity severity = Severity.fromSarif(result.getLevel().toString());
//            Applicability applicability = Applicability.fromSarif(result.getLevel().toString());
//
//            ReportingDescriptor rule = run.getTool().getDriver().getRules().stream()
//                    .filter(r -> r.getId().equals(result.getRuleId()))
//                    .findFirst()
//                    .orElse(null);
//
//            List<List<ImpactPath>> impactPaths = ImpactPath.deserializeImpactPaths(result.getProperties().get("impactPaths").toString());
//            String fixedVersions = result.getProperties().get("fixedVersions").toString();
//
//            if (rule == null) {
//                log.error("Rule not found for result: " + result.getRuleId());
//            } else {
//                // Create FileTreeNodes for files with found issues
//                FileTreeNode fileNode = results.get(filePath);
//                if (fileNode == null) {
//                    fileNode = new FileTreeNode(filePath);
//                    results.put(filePath, fileNode);
//                }
//                String fullDescription = rule.getFullDescription().getText();
//
//
//                // params: String title, String reason, Severity severity, String ruleID, Applicability applicability, List<List<String>> impactPaths, boolean isDirectDependency, List<String> fixedVersions
//                ScaIssueNode scaIssueNode = new ScaIssueNode(reporter.getParam(), fullDescription, severity, rule.getId(), applicability, impactPaths, fixedVersions);
//
//                fileNode.addIssue(scaIssueNode);
//            }
//        }
//
//        return new ArrayList<>(results.values());
//    }


    public static void main(String[] args) {
//        JfrogCliDriver cliDriver = new JfrogCliDriver(null, new NullLog());
//        File workingDirectory = new File("C:\\Users\\Keren Reshef\\Projects\\test projects\\test-cve-contextual-analysis");
//        String[] commandArgs = {"audit", "--format=sarif"};
//
//        try {
//            CommandResults results = cliDriver.runCommand(workingDirectory, commandArgs, new ArrayList<>(), new NullLog());
//            if(!results.isOk()){
//                System.out.println("Running JFrog CLI command failed: " + results.getErr());
//                return;
//            }
//            String output = results.getRes();
//            SarifParser sarifParser = new SarifParser(new NullLog());
//            List<JFrogSecurityWarning> warnings = sarifParser.parse(output);
//            warnings.forEach(warning -> System.out.println(warning.toString()));
//        } catch (IOException | InterruptedException e) {
//            System.out.println("Failed to run JFrog CLI command: " + e.getMessage());
//        }

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
