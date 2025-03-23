package com.jfrog.ide.common.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.common.nodes.FileIssueNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.SastIssueNode;
import com.jfrog.ide.common.nodes.ScaIssueNode;
import com.jfrog.ide.common.nodes.subentities.FindingInfo;
import com.jfrog.ide.common.nodes.subentities.Severity;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import static org.testng.Assert.*;


public class SarifParserTest {
    private SarifParser parser;
    private List<FileTreeNode> results;
    private final String resourcesDir = "src/test/resources/parse/";


    @BeforeClass
    public void setUp() {
        parser = new SarifParser(new NullLog());
    }

    private String readSarifReportFromFile(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(new FileInputStream(filePath));
        return jsonNode.toString();
    }

    @Test
    public void testParseInvalidSarifReport() {
        // test a report without "runs" element
        assertThrows(NoSuchElementException.class, () -> parser.parse(readSarifReportFromFile(resourcesDir + "invalid_sarif.json")));
        // test a report without "results" element
        assertThrows(NullPointerException.class, () -> parser.parse(readSarifReportFromFile(resourcesDir + "invalid_sarif_no_results.json")));
    }

    @Test
    public void testParseSarifReportWithOnlyScaResults() throws IOException {
        results = parser.parse(readSarifReportFromFile(resourcesDir + "sca_no_jas.json"));

        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getChildren().size(), 10);
        assertEquals(results.get(0).getSeverity(), Severity.High);
        results.get(0).getChildren().forEach(node -> assertEquals(node.getClass(), ScaIssueNode.class));
        results.forEach(fileTreeNode -> fileTreeNode.getChildren().forEach(node -> {
            if (node instanceof ScaIssueNode scaIssueNode) {
                assertNotNull(scaIssueNode.getImpactPaths());
                assertFalse(scaIssueNode.getImpactPaths().isEmpty());
            }
        }));
    }

    @Test
    public void testParseSarifReportWithCodeFlowsInSast() throws IOException {
        results = parser.parse(readSarifReportFromFile(resourcesDir + "code_flows_in_sast.json"));

        assertEquals(results.size(), 2);
        results.forEach(fileTreeNode -> fileTreeNode.getChildren().forEach(node -> {
            if (node instanceof SastIssueNode sastIssueNode) {
                assertNotNull(sastIssueNode.getCodeFlows());
                assertTrue(sastIssueNode.getCodeFlows().length > 0);
                for (FindingInfo[] codeFlow : sastIssueNode.getCodeFlows()) {
                    assertNotNull(codeFlow);
                    assertTrue(codeFlow.length > 0);
                    for (FindingInfo findingInfo : codeFlow) {
                        assertNotNull(findingInfo.getLineSnippet());
                        assertTrue(findingInfo.getRowStart() > 0);
                        assertTrue(findingInfo.getColStart() > 0);
                        assertTrue(findingInfo.getRowEnd() > 0);
                        assertTrue(findingInfo.getColEnd() > 0);
                    }
                }
            }
        }));
    }

    @Test
    public void testParseSarifReportWithScaAndJasResults() throws IOException {
        results = parser.parse(readSarifReportFromFile(resourcesDir + "sca_iac_secrets_sast.json"));

        assertEquals(results.size(), 4);

        results.forEach(fileTreeNode -> {
            switch (((FileIssueNode) fileTreeNode.getChildren().get(0)).getReporterType()) {
                case SCA:
                    assertEquals(fileTreeNode.getSeverity(), Severity.High);
                    assertEquals(fileTreeNode.getChildren().size(), 10);
                    break;
                case IAC:
                    assertEquals(fileTreeNode.getSeverity(), Severity.Medium);
                    assertEquals(fileTreeNode.getChildren().size(), 5);
                    break;
                case SECRETS:
                    assertEquals(fileTreeNode.getSeverity(), Severity.High);
                    assertEquals(fileTreeNode.getChildren().size(), 1);
                    break;
                case SAST:
                    assertEquals(fileTreeNode.getSeverity(), Severity.Medium);
                    assertEquals(fileTreeNode.getChildren().size(), 3);
                    break;
            }
        });
    }
}
