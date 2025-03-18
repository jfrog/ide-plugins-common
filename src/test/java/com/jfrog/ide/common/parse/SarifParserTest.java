package com.jfrog.ide.common.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SarifParserTest {
    private String sarifReport;


    private String readSarifReportFromFile(String filePath) throws IOException {
//        String jsonFilePath = "src/test/resources/parse/sca_no_jas.json";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(new FileInputStream(filePath));
        return jsonNode.toString();
    }

    @Test
    public void testParseInvalidSarifReport() throws IOException {
        this.sarifReport = readSarifReportFromFile("src/test/resources/parse/sca_no_jas.json");

    }
}
