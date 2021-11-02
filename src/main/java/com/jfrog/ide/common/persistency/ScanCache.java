package com.jfrog.ide.common.persistency;

import com.google.common.collect.Sets;
import com.jfrog.ide.common.utils.Utils;
import com.jfrog.xray.client.services.common.Cve;
import com.jfrog.xray.client.services.graph.Component;
import com.jfrog.xray.client.services.graph.License;
import com.jfrog.xray.client.services.graph.Violation;
import com.jfrog.xray.client.services.graph.Vulnerability;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.Severity;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Cache for Xray scan.
 *
 * @author yahavi
 */
public abstract class ScanCache {

    ScanCacheMap scanCacheMap;
    File file;

    public void write() throws IOException {
        scanCacheMap.write(file);
    }

    public void add(com.jfrog.xray.client.services.summary.Artifact artifact) {
        scanCacheMap.put(Utils.getArtifact(artifact));
    }

    public void add(Artifact artifact) {
        scanCacheMap.put(artifact);
    }

    public void add(Violation violation, String packageType) {
        addComponents(violation.getComponents(), Severity.valueOf(violation.getSeverity()), violation.getSummary(), packageType, violation.getCves());
    }

    public void add(Vulnerability vulnerability, String packageType) {
        addComponents(vulnerability.getComponents(), Severity.valueOf(vulnerability.getSeverity()), vulnerability.getSummary(), packageType, vulnerability.getCves());
    }

    public void add(License license, String packageType, boolean violation) {
        for (Map.Entry<String, ? extends Component> entry : license.getComponents().entrySet()) {
            String id = entry.getKey();
            id = id.substring(id.indexOf("://") + 3);
            Component component = entry.getValue();
            org.jfrog.build.extractor.scan.License issue = new org.jfrog.build.extractor.scan.License(new ArrayList<>(),
                    license.getName(), license.getKey(), component.getFixedVersions(), violation);

            if (this.contains(id)) {
                Artifact artifact = get(id);
                Set<org.jfrog.build.extractor.scan.License> licenses = artifact.getLicenses();
                // We should override existing info, in case of forced scan.
                licenses.remove(issue);
                licenses.add(issue);
                artifact.setLicenses(licenses);

                continue;
            }
            // If not exist, creates a new data object.
            GeneralInfo info = new GeneralInfo(id, component.getImpactPaths().get(0).get(0).getFullPath(), packageType);
            Artifact artifact = new Artifact(info, new HashSet<>(), new HashSet<>() {{
                add(issue);
            }});
            this.add(artifact);
        }

    }

    public Artifact get(String id) {
        return scanCacheMap.get(id);
    }

    public boolean contains(String id) {
        return scanCacheMap.contains(id);
    }

    ScanCacheMap getScanCacheMap() {
        return scanCacheMap;
    }

    void setScanCacheMap(ScanCacheMap scanCacheMap) {
        this.scanCacheMap = scanCacheMap;
    }

    private void addComponents(Map<String, ? extends Component> components, Severity severity, String summary, String packageType, List<? extends Cve> cves) {
        String cveId = ListUtils.emptyIfNull(cves).stream().map(Cve::getId).filter(StringUtils::isNotBlank).findAny().orElse("");
        for (Map.Entry<String, ? extends Component> entry : components.entrySet()) {
            String id = StringUtils.substringAfter(entry.getKey(), "://");
            Component component = entry.getValue();
            Issue issue = new Issue("", severity, StringUtils.defaultIfBlank(summary, "N/A"), component.getFixedVersions(), cveId);

            if (contains(id)) {
                Artifact artifact = get(id);
                Set<Issue> issues = artifact.getIssues();
                // We should override existing info, in case of forced scan.
                issues.remove(issue);
                issues.add(issue);
                artifact.setIssues(issues);
                continue;
            }
            // If not exist, creates a new data object.
            GeneralInfo info = new GeneralInfo(id, component.getImpactPaths().get(0).get(0).getFullPath(), packageType);
            Artifact artifact = new Artifact(info, Sets.newHashSet(issue), new HashSet<>());
            add(artifact);
        }
    }
}
