package com.jfrog.ide.common.exporter.csv;

import com.jfrog.ide.common.exporter.exportable.ExportableViolatedLicense;
import com.opencsv.bean.CsvBindAndSplitByName;
import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.License;

import java.util.ArrayList;
import java.util.List;

import static com.jfrog.ide.common.exporter.csv.CsvExporter.*;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author yahavi
 **/
@Setter
@SuppressWarnings("unused")
public class CsvViolatedViolatedLicenseRow extends ExportableViolatedLicense {
    CsvViolatedViolatedLicenseRow(DependencyTree directDependency, License violatedLicense) {
        super(directDependency, violatedLicense);
    }

    @Getter
    @CsvBindByName(column = LICENSE_NAME_COL)
    private String name;

    @CsvBindByName(column = IMPACTED_DEPENDENCY_COL)
    private String impactedDependencyName;

    @CsvBindByName(column = IMPACTED_DEPENDENCY_VERSION_COL)
    private String impactedDependencyVersion;

    @CsvBindByName(column = TYPE_COL)
    private String type;

    @CsvBindAndSplitByName(column = DIRECT_DEPENDENCIES_COL, elementType = String.class, writeDelimiter = ";")
    private List<String> directDependencies;

    @Override
    public void addDirectDependency(String directDependency) {
        directDependencies = defaultIfNull(directDependencies, new ArrayList<>());
        directDependencies.add(directDependency);
    }
}
