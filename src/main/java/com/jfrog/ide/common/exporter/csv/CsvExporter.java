package com.jfrog.ide.common.exporter.csv;

import com.google.common.collect.Lists;
import com.jfrog.ide.common.exporter.Exporter;
import com.jfrog.ide.common.exporter.exportable.ExportableViolatedLicense;
import com.jfrog.ide.common.exporter.exportable.ExportableVulnerability;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.HeaderColumnNameMappingStrategyBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.collections4.comparators.FixedOrderComparator;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Severity;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yahavi
 **/
public class CsvExporter extends Exporter {
    static final String SEVERITY_COL = "SEVERITY";
    static final String IMPACTED_DEPENDENCY_COL = "IMPACTED DEPENDENCY";
    static final String IMPACTED_DEPENDENCY_VERSION_COL = "VERSION";
    static final String TYPE_COL = "TYPE";
    static final String FIXED_VERSION_COL = "FIXED VERSIONS";
    static final String DIRECT_DEPENDENCIES_COL = "DIRECT DEPENDENCIES";
    static final String CVES_COL = "CVES";
    static final String CVSS_V2_COL = "CVSS V2";
    static final String CVSS_V3_COL = "CVSS V3";
    static final String ISSUE_ID_COL = "ISSUE ID";
    static final String SUMMARY_COL = "SUMMARY";

    static final String LICENSE_NAME_COL = "LICENSE";

    private static final List<String> COLUMNS_ORDER = Lists.newArrayList(
            LICENSE_NAME_COL, SEVERITY_COL, IMPACTED_DEPENDENCY_COL, IMPACTED_DEPENDENCY_VERSION_COL, TYPE_COL,
            FIXED_VERSION_COL, DIRECT_DEPENDENCIES_COL, CVES_COL, CVSS_V2_COL, CVSS_V3_COL,
            ISSUE_ID_COL, SUMMARY_COL);

    public CsvExporter(DependencyTree root) {
        super(root);
    }

    @Override
    public String generateVulnerabilitiesReport() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
        try (Writer writer = new StringWriter()) {
            StatefulBeanToCsv<CsvVulnerabilityRow> csvWriter = createCsvWriter(CsvVulnerabilityRow.class, writer);

            List<CsvVulnerabilityRow> issueRows = collectVulnerabilities().stream()
                    .map(exportableIssue -> (CsvVulnerabilityRow) exportableIssue)
                    .sorted(Comparator.comparing(issueRow -> Severity.valueOf(issueRow.getSeverity()), Comparator.reverseOrder()))
                    .collect(Collectors.toList());
            for (CsvVulnerabilityRow issue : issueRows) {
                csvWriter.write(issue);
            }

            return writer.toString();
        }

    }

    @Override
    public String generateViolatedLicensesReport() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
        try (Writer writer = new StringWriter()) {
            StatefulBeanToCsv<CsvViolatedViolatedLicenseRow> csvWriter = createCsvWriter(CsvViolatedViolatedLicenseRow.class, writer);

            List<CsvViolatedViolatedLicenseRow> violatedLicenseRows = collectViolatedLicenses().stream()
                    .map(exportableViolatedLicense -> (CsvViolatedViolatedLicenseRow) exportableViolatedLicense)
                    .sorted(Comparator.comparing(CsvViolatedViolatedLicenseRow::getName))
                    .collect(Collectors.toList());
            for (CsvViolatedViolatedLicenseRow violatedLicenseRow : violatedLicenseRows) {
                csvWriter.write(violatedLicenseRow);
            }

            return writer.toString();
        }
    }

    private <T> StatefulBeanToCsv<T> createCsvWriter(Class<T> rowClass, Writer writer) {
        HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategyBuilder<T>().build();
        strategy.setType(rowClass);
        strategy.setColumnOrderOnWrite(new FixedOrderComparator<>(COLUMNS_ORDER));
        return new StatefulBeanToCsvBuilder<T>(writer).withMappingStrategy(strategy).build();
    }

    @Override
    protected ExportableVulnerability createExportableVulnerability(DependencyTree directDependency, Issue issue) {
        return new CsvVulnerabilityRow(directDependency, issue);
    }

    @Override
    protected ExportableViolatedLicense createExportableViolatedLicense(DependencyTree directDependency, License violatedLicense) {
        return new CsvViolatedViolatedLicenseRow(directDependency, violatedLicense);
    }
}
