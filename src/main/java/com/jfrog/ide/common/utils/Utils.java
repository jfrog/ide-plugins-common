package com.jfrog.ide.common.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.xray.client.services.common.Cve;
import com.jfrog.xray.client.services.summary.VulnerableComponents;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Severity;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author yahavi
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Utils {

    public static ObjectMapper createMapper() {
        return createMapper(null);
    }

    public static ObjectMapper createYAMLMapper() {
        return createMapper(new YAMLFactory());
    }

    private static ObjectMapper createMapper(JsonFactory jf) {
        return new ObjectMapper(jf)
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(NON_EMPTY)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    }

    public static String createComponentId(String artifactId, String version) {
        return String.join(":", artifactId, version);
    }

    public static String createComponentId(String groupId, String artifactId, String version) {
        return String.join(":", groupId, artifactId, version);
    }

    public static String createLicenseString(License license) {
        if (license.isFullNameEmpty() || StringUtils.isBlank(license.getName())) {
            return license.getName();
        }
        return license.getFullName() + " (" + license.getName() + ")";
    }

    public static License toLicense(com.jfrog.xray.client.services.summary.License other) {
        List<String> moreInfoUrl = Lists.newArrayList(ListUtils.emptyIfNull(other.moreInfoUrl()));
        return new License(other.getFullName(), other.getName(), moreInfoUrl);
    }

    public static Issue toIssue(com.jfrog.xray.client.services.summary.Issue other) {
        Severity severity = Severity.fromString(other.getSeverity());
        List<? extends VulnerableComponents> vulnerableComponentsList = other.getVulnerableComponents();
        List<String> fixedVersions = null;
        if (CollectionUtils.isNotEmpty(vulnerableComponentsList)) {
            VulnerableComponents vulnerableComponents = vulnerableComponentsList.get(0);
            fixedVersions = vulnerableComponents.getFixedVersions();
        }
        return new Issue(other.getIssueId(), severity, other.getSummary(), fixedVersions, toCves(other.getCves()),
                Collections.emptyList(), "");
    }

    /**
     * Convert list of {@link Cve} to list of {@link org.jfrog.build.extractor.scan.Cve}
     *
     * @param cves - CVE list
     * @return first non-empty CVE ID or an empty string.
     */
    public static List<org.jfrog.build.extractor.scan.Cve> toCves(List<? extends Cve> cves) {
        return ListUtils.emptyIfNull(cves).stream()
                .map(clientCve -> new org.jfrog.build.extractor.scan.Cve(clientCve.getId(), clientCve.getCvssV2Score(), clientCve.getCvssV3Score()))
                .collect(Collectors.toList());
    }

    /**
     * Returns the server configured sslContext or Strategy that trust all certificates if
     * InsecureTls was chosen by the user.
     *
     * @param serverConfig the user's configuration for a JFrog platform server.
     * @return the server configured sslContext or Strategy that trust all certificates if
     * InsecureTls was chosen by the user.
     */
    public static SSLContext createSSLContext(ServerConfig serverConfig) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return serverConfig.isInsecureTls() ?
                SSLContextBuilder.create().loadTrustMaterial(TrustAllStrategy.INSTANCE).build() :
                serverConfig.getSslContext();
    }

    /**
     * Returns Set of Paths cleaned of subdirectories.
     * For example the set ["/a", "/b/c", "/a/d"] will become ["/a", "/b/c"]
     *
     * @param paths Paths to consolidate.
     * @return Set of Paths cleaned of subdirectories.
     */
    public static Set<Path> consolidatePaths(Set<Path> paths) {
        Set<Path> finalPaths = new HashSet<>();
        // Create a sorted by length list of paths
        List<Path> sortedList = paths.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .sorted(Comparator.comparingInt(Path::getNameCount))
                .collect(Collectors.toList());
        sortedList.forEach(currentPath -> {
                    boolean isRootPath = true;
                    // Iterate over the sorted by directories count length list.
                    for (Path shorterPath : sortedList) {
                        // CurrentPath is shorter or equals to the shortPath therefore all the next paths can't contain the currentPath
                        if (currentPath.getNameCount() <= shorterPath.getNameCount()) {
                            break;
                        }
                        // The currentPath is subPath and we should not add it to the list
                        if (currentPath.startsWith(shorterPath)) {
                            isRootPath = false;
                            break;
                        }
                    }
                    if (isRootPath) {
                        finalPaths.add(currentPath);
                    }
                }
        );
        return finalPaths;
    }

    public static String removeComponentIdPrefix(String compId) {
        return StringUtils.substringAfter(compId, "://");
    }

    /**
     * Resolve Xray URL from the input Xray URL (may be blank) or from the input platform URL.
     *
     * @param xrayUrl     - Customize Xray URL or blank
     * @param platformUrl - JFrog platform URL
     * @return the resolved Xray URL
     */
    public static String resolveXrayUrl(String xrayUrl, String platformUrl) {
        return resolveProductUrl(xrayUrl, platformUrl, "xray");
    }

    /**
     * Resolve Artifactory URL from the input Artifactory URL (may be blank) or from the input platform URL.
     *
     * @param artifactoryUrl - Customize Artifactory URL or blank
     * @param platformUrl    - JFrog platform URL
     * @return the resolved Xray URL
     */
    public static String resolveArtifactoryUrl(String artifactoryUrl, String platformUrl) {
        return resolveProductUrl(artifactoryUrl, platformUrl, "artifactory");
    }

    /**
     * Get the OS and architecture name.
     *
     * @return the OS and architecture name.
     * @throws IOException in case of unsupported OS.
     */
    public static String getOSAndArc() throws IOException {
        String arch = SystemUtils.OS_ARCH;
        // Windows
        if (SystemUtils.IS_OS_WINDOWS) {
            return "windows-amd64";
        }
        // Mac
        if (SystemUtils.IS_OS_MAC) {
            if (StringUtils.equalsAny(arch, "aarch64", "arm64")) {
                return "mac-arm64";
            } else {
                return "mac-amd64";
            }
        }
        // Linux
        if (SystemUtils.IS_OS_LINUX) {
            switch (arch) {
                case "i386":
                case "i486":
                case "i586":
                case "i686":
                case "i786":
                case "x86":
                    return "linux-386";
                case "amd64":
                case "x86_64":
                case "x64":
                    return "linux-amd64";
                case "arm":
                case "armv7l":
                    return "linux-arm";
                case "aarch64":
                    return "linux-arm64";
                case "ppc64":
                case "ppc64le":
                    return "linux-" + arch;
            }
        }
        throw new IOException(String.format("Unsupported OS: %s-%s", SystemUtils.OS_NAME, arch));
    }

    /**
     * Resolve Artifactory or Xray URL from the input product URL (may be blank) or from the input platform URL.
     *
     * @param productUrl      - Customize Artifactory/Xray URL or blank
     * @param platformUrl     - JFrog platform URL
     * @param productEndpoint - "artifactory" or "xray"
     * @return the resolved Artifactory or Xray URL
     */
    private static String resolveProductUrl(String productUrl, String platformUrl, String productEndpoint) {
        String url = trimToEmpty(productUrl);
        if (isNotBlank(url)) {
            return removeEnd(url, "/");
        }
        url = trimToEmpty(platformUrl);
        if (isBlank(url)) {
            return "";
        }
        return removeEnd(url, "/") + "/" + productEndpoint;
    }
}
