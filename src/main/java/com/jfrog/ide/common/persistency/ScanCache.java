package com.jfrog.ide.common.persistency;

import com.jfrog.ide.common.tree.*;
import com.jfrog.xray.client.services.scan.Component;
import com.jfrog.xray.client.services.scan.License;
import com.jfrog.xray.client.services.scan.Violation;
import com.jfrog.xray.client.services.scan.Vulnerability;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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

    // TODO: remove
//    public void add(com.jfrog.xray.client.services.summary.Artifact artifact) {
//        scanCacheMap.put(Utils.getArtifact(artifact));
//    }

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
            // TODO: I set unknown severity, but all this will probably be changed or removed.
            com.jfrog.ide.common.tree.License cacheLicense = new com.jfrog.ide.common.tree.License(
                    license.getLicenseName(), license.getLicenseKey(), license.getReferences(), Severity.Unknown, "");

            Artifact artifact = get(id);
            if (artifact != null) {
                // TODO: the implementations of these methods were changed! if it's still needed, handling existing issues should probably be done inside Artifact class.
//                Set<com.jfrog.ide.common.tree.License> licenses = artifact.getLicenses();
//                // We should override existing info, in case of forced scan.
//                licenses.remove(cacheLicense);
//                licenses.add(cacheLicense);
//                artifact.setLicenses(licenses);
                // TODO: temporary implementation:
                artifact.addIssueOrLicense(cacheLicense);
                continue;
            }
            // If not exist, creates a new data object.
            artifact = new Artifact(new GeneralInfo().componentId(id));
            artifact.addIssueOrLicense(cacheLicense);
            add(artifact);
        }
    }

    public void add(Vulnerability vulnerability) {
        // TODO: uncomment and fix
        // Search for a CVE with ID. Due to UI limitations, we take only the first match.
//        List<Cve> cves = toCves(vulnerability.getCves());
//        for (Map.Entry<String, ? extends Component> entry : vulnerability.getComponents().entrySet()) {
//            String id = StringUtils.substringAfter(entry.getKey(), "://");
//
//            Component component = entry.getValue();
//            Artifact artifact;
//            if (contains(id)) {
//                artifact = get(id);
//            } else {
//                GeneralInfo info = new GeneralInfo(id, component.getImpactPaths().get(0).get(0).getFullPath(), "");
//                artifact = new Artifact(info);
//                add(artifact);
//            }
//
//            // TODO: handle no cves!
//            for (Cve cve : cves) {
//                Issue issue = new Issue(vulnerability.getIssueId(), Severity.valueOf(vulnerability.getSeverity()),
//                        StringUtils.defaultIfBlank(vulnerability.getSummary(), "N/A"),
//                        component.getFixedVersions(), cve, vulnerability.getReferences(), vulnerability.getIgnoreRuleUrl());
//                artifact.addIssueOrLicense(issue);
//            }
//        }
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
