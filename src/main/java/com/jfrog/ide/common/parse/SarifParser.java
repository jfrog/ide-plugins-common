package com.jfrog.ide.common.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jetbrains.qodana.sarif.model.*;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import org.jfrog.build.api.util.Log;
import com.jetbrains.qodana.sarif.SarifUtil;
import org.jfrog.build.api.util.NullLog;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class SarifParser {
    private Log log;

    SarifParser(Log log) {
        this.log = log;
    }

    List<JFrogSecurityWarning> parse (String output){
        Reader reader = new StringReader(output);
        List<JFrogSecurityWarning> warnings = new ArrayList<>();

        SarifReport report = SarifUtil.readReport(reader);
        // extract SCA run object from SARIF, if not exist throws an exception
        List<Run> SCARuns = report.getRuns().stream().
                filter(run -> run.getTool().getDriver().getName().contains(SourceCodeScanType.SCA.getParam()))
                .toList();



        return warnings;
    }

    private List<SCAFinding> parseSCAFindings(List<Run> SCARuns){
        List<SCAFinding> scaFindings = new ArrayList<>();
        if (SCARuns.isEmpty()) {
            throw new NoSuchElementException("SCA run not found in SARIF report");
        }

        for (Run SCARun : SCARuns) {
            // get list of results for each SCA run
            List<Result> results = SCARun.getResults();
            // get descriptor file directory path
            // TODO: verify if invocations may be more than one
            String descriptorDirPath = SCARun.getInvocations().get(0).getWorkingDirectory().getUri();

            for (Result result : results) {
                // build the JfrogSecurityWarning object
                PropertyBag properties = result.getProperties();
                if(properties!= null){
                    String applicability = properties.get("applicability").toString();
                    String fixedVersion = properties.get("fixedVersion").toString();
                }
                String descriptorFileName = result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri();
                String descriptorFullPath = descriptorDirPath + File.separator + descriptorFileName;
                ReportingDescriptor rule = SCARun.getTool().getDriver().getRules().stream()
                        .filter(r -> r.getId().equals(result.getRuleId()))
                        .findFirst()
                        .orElse(null);

                if (rule == null) {
                    log.error("Rule not found for result: " + result.getRuleId());
                } else {
                    scaFindings.add(new SCAFinding(rule, SourceCodeScanType.SCA, result));
//                warnings.add(new JFrogSecurityWarning(result, SourceCodeScanType.SCA, result.getRule()));
                }
            }
        }
        return null;
    }


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
        String jsonFilePath = "C:\\Users\\Keren Reshef\\Projects\\test projects\\test-cve-contextual-analysis\\results.json";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(new FileInputStream(jsonFilePath));
            String outputJson = jsonNode.toString();
            SarifParser sarifParser = new SarifParser(new NullLog());
            List<JFrogSecurityWarning> warnings = sarifParser.parse(outputJson);
            warnings.forEach(warning -> System.out.println(warning.toString()));
        } catch (IOException e) {
            System.out.println("Failed to read JSON file" + e.getMessage());
        }

    }
}
