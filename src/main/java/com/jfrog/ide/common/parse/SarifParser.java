package com.jfrog.ide.common.parse;

import com.jetbrains.qodana.sarif.model.*;
import com.jfrog.ide.common.configuration.JfrogCliDriver;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import org.jfrog.build.api.util.Log;
import com.jetbrains.qodana.sarif.SarifUtil;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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
        Run SCARun = report.getRuns().stream().
                filter(run -> run.getTool().getDriver().getName().contains(SourceCodeScanType.SCA.getParam()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Couldn't find SCA scan results in CLI scan output"));

        // get list of results
        List<Result> results = SCARun.getResults();
        for (Result result : results) {// build the JfrogSecurityWarning object
            PropertyBag properties = result.getProperties();
            if(properties!= null){
                String applicability = properties.get("applicability").toString();
                String fixedVersion = properties.get("fixedVersion").toString();
            }
        }

        return warnings;
    }


    public static void main(String[] args) {
        JfrogCliDriver cliDriver = new JfrogCliDriver(null, new NullLog());
        File workingDirectory = new File("C:\\Users\\Keren Reshef\\Projects\\test projects\\test-cve-contextual-analysis");
        String[] commandArgs = {"audit", "--format=sarif"};
        try {
            CommandResults results = cliDriver.runCommand(workingDirectory, commandArgs, new ArrayList<>(), new NullLog());
            if(!results.isOk()){
                System.out.println("Running JFrog CLI command failed: " + results.getErr());
                return;
            }
            String output = results.getRes();
            SarifParser sarifParser = new SarifParser(new NullLog());
            List<JFrogSecurityWarning> warnings = sarifParser.parse(output);
            warnings.forEach(warning -> System.out.println(warning.toString()));
        } catch (IOException | InterruptedException e) {
            System.out.println("Failed to run JFrog CLI command: " + e.getMessage());
        }

    }
}
