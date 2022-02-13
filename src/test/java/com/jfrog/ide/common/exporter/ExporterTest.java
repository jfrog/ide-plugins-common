package com.jfrog.ide.common.exporter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jfrog.ide.common.exporter.csv.CsvExporter;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.extractor.scan.*;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.testng.Assert.assertEquals;

/**
 * @author yahavi
 **/
public class ExporterTest {
    private static final Path EXPORTER_RESULTS = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "exporter"));
    private static final File EXPECTED_VULNERABILITIES_CSV = EXPORTER_RESULTS.resolve("vulnerabilities.csv").toFile();
    private static final File EXPECTED_VIOLATED_LICENSES_CSV = EXPORTER_RESULTS.resolve("violatedLicenses.csv").toFile();

    @Test
    public void testGenerateEmptyVulnerabilitiesReport() throws Exception {
        Exporter exporter = new CsvExporter(new DependencyTree());
        assertEquals(exporter.generateVulnerabilitiesReport(), "");
    }

    @Test
    public void testGenerateVulnerabilitiesReport() throws Exception {
        Exporter exporter = new CsvExporter(createTestTree());
        assertEquals(exporter.generateVulnerabilitiesReport(), readCsvFile(EXPECTED_VULNERABILITIES_CSV));
    }

    @Test
    public void testGenerateEmptyViolatedLicensesReport() throws Exception {
        Exporter exporter = new CsvExporter(new DependencyTree());
        assertEquals(exporter.generateViolatedLicensesReport(), "");
    }

    @Test
    public void testGenerateViolatedLicenseReport() throws Exception {
        Exporter exporter = new CsvExporter(createTestTree());
        assertEquals(exporter.generateViolatedLicensesReport(), readCsvFile(EXPECTED_VIOLATED_LICENSES_CSV));
    }

    /**
     * Read CSV file and replace all Windows line breaks with Posix line breaks.
     *
     * @param csvFile - The CSV file to read
     * @return CSV file content.
     * @throws IOException in case of any I/O error.
     */
    private String readCsvFile(File csvFile) throws IOException {
        return FileUtils.readFileToString(csvFile, StandardCharsets.UTF_8).replaceAll("\r\n", "\n");
    }

    /**
     * Create test tree populated with vulnerabilities and violated licenses.
     *
     * @return test tree.
     */
    private DependencyTree createTestTree() {
        DependencyTree root = new DependencyTree();
        root.setGeneralInfo(new GeneralInfo().componentId("root:1.0.0").pkgType("Go"));
        root.setMetadata(true);

        // Create first node
        DependencyTree node1 = new DependencyTree("node-1:1.0.0");
        node1.setGeneralInfo(new GeneralInfo().componentId("node-1:1.0.0"));
        root.add(node1);

        // Create second node
        DependencyTree node2 = new DependencyTree("node-2:1.1.0");
        node2.setGeneralInfo(new GeneralInfo().componentId("node-2:1.1.0"));
        root.add(node2);

        // Populate nodes with vulnerabilities and violated licenses.
        addVulnerabilities(node1, node2);
        addViolatedLicenses(node1, node2);

        return root;
    }

    /**
     * Add some vulnerabilities to node1 and node 2.
     *
     * @param node1 - The first node
     * @param node2 - The second node
     */
    private void addVulnerabilities(DependencyTree node1, DependencyTree node2) {
        // Create 3 issues
        Issue issue1 = new Issue("XRAY-1", Severity.Critical, "Issue 1 summary",
                Lists.newArrayList("2.0.0", "3.0.0"),
                Lists.newArrayList(new Cve("CVE-1", "8.0", "9.0"), new Cve("CVE-11", "", "9.1")),
                Collections.emptyList(), "");
        issue1.setComponent("impacted-1:1.0.0");
        Issue issue2 = new Issue("XRAY-2", Severity.Low, "Issue 2 summary",
                Lists.newArrayList("1.0.0"),
                Lists.newArrayList(new Cve("CVE-2", "7.0", "8.0")),
                Collections.emptyList(), "");
        issue2.setComponent("impacted-2:1.0.0");
        Issue issue3 = new Issue("XRAY-3", Severity.High, "Issue 3 summary",
                Lists.newArrayList("4.0.0"),
                Lists.newArrayList(new Cve("CVE-3", "7.0", "8.0")),
                Collections.emptyList(), "");
        issue3.setComponent("impacted-3:1.0.0");

        // Add issues to node1 and node 2
        node1.setIssues(Sets.newHashSet(issue1));
        node2.setIssues(Sets.newHashSet(issue1, issue2, issue3));
    }

    /**
     * Add some violated licences to node1 and node 2.
     *
     * @param node1 - The first node
     * @param node2 - The second node
     */
    private void addViolatedLicenses(DependencyTree node1, DependencyTree node2) {
        // Create 3 violated licenses
        License violatedLicense1 = new License("The apache software license version 2.0", "Apache-2.0", Collections.emptyList(), true);
        violatedLicense1.setComponent("impacted-1:1.0.0");
        License violatedLicense2 = new License("The MIT License", "MIT", Collections.emptyList(), true);
        violatedLicense2.setComponent("impacted-2:1.0.0");
        License violatedLicense3 = new License("Eclipse Public License 1.0", "EPL-1.0", Collections.emptyList(), true);
        violatedLicense3.setComponent("impacted-3:1.0.0");

        // Add violated licenses to node1 and node 2
        node1.setViolatedLicenses(Sets.newHashSet(violatedLicense1));
        node2.setViolatedLicenses(Sets.newHashSet(violatedLicense1, violatedLicense2, violatedLicense3));
    }
}
