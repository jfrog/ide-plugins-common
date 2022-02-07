package com.jfrog.ide.common.persistency;

import com.google.common.collect.Sets;
import com.jfrog.ide.common.utils.Utils;
import com.jfrog.xray.client.services.scan.Component;
import com.jfrog.xray.client.services.scan.License;
import com.jfrog.xray.client.services.scan.Violation;
import com.jfrog.xray.client.services.scan.Vulnerability;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jfrog.ide.common.utils.Utils.toCves;

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

    public void add(Violation violation) {
        if (StringUtils.isBlank(violation.getLicenseKey())) {
            add((Vulnerability) violation);
        } else {
            add(violation, true);
        }
    }

    public void add(License license, boolean violation) {
        for (Map.Entry<String, ? extends Component> entry : license.getComponents().entrySet()) {
            String id = StringUtils.substringAfter(entry.getKey(), "://");
            org.jfrog.build.extractor.scan.License cacheLicense = new org.jfrog.build.extractor.scan.License(
                    license.getLicenseName(), license.getLicenseKey(), license.getReferences(), violation);

            Artifact artifact = get(id);
            if (artifact != null) {
                Set<org.jfrog.build.extractor.scan.License> licenses = artifact.getLicenses();
                // We should override existing info, in case of forced scan.
                licenses.remove(cacheLicense);
                licenses.add(cacheLicense);
                artifact.setLicenses(licenses);
                continue;
            }
            // If not exist, creates a new data object.
            artifact = new Artifact(new GeneralInfo().componentId(id), new HashSet<>(), Sets.newHashSet(cacheLicense));
            add(artifact);
        }
    }

    public void add(Vulnerability vulnerability) {
        // Search for a CVE with ID. Due to UI limitations, we take only the first match.
        List<Cve> cves = toCves(vulnerability.getCves());
        for (Map.Entry<String, ? extends Component> entry : vulnerability.getComponents().entrySet()) {
            String id = StringUtils.substringAfter(entry.getKey(), "://");
            Component component = entry.getValue();
            Issue issue = new Issue(vulnerability.getIssueId(), Severity.valueOf(vulnerability.getSeverity()),
                    StringUtils.defaultIfBlank(vulnerability.getSummary(), "N/A"),
                    component.getFixedVersions(), cves, vulnerability.getReferences(), vulnerability.getIgnoreRuleUrl());

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
            GeneralInfo info = new GeneralInfo(id, component.getImpactPaths().get(0).get(0).getFullPath(), "");
            Artifact artifact = new Artifact(info, Sets.newHashSet(issue), new HashSet<>());
            add(artifact);
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
}
