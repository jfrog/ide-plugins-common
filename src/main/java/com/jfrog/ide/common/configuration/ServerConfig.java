package com.jfrog.ide.common.configuration;

import org.jfrog.build.client.ProxyConfiguration;

import javax.net.ssl.SSLContext;

import static org.apache.commons.lang3.StringUtils.isAllBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public interface ServerConfig {
    /**
     * The policy for Xray scan:
     * VULNERABILITIES - Show all vulnerabilities
     * PROJECT - Show JFrog platform project's violations
     * WATCH - Show Watch's violations
     */
    enum PolicyType {VULNERABILITIES, PROJECT, WATCHES}

    String getUrl();

    String getXrayUrl();

    String getArtifactoryUrl();

    String getUsername();

    String getPassword();

    String getAccessToken();

    PolicyType getPolicyType();

    String getProject();

    String getWatches();

    /**
     * Return true to disable SSL certificates verification.
     *
     * @return true to disable SSL certificates verification
     */
    boolean isInsecureTls();

    /**
     * Return the SSLContext from IDE configuration. This allows to accept certificates defined in the IDE configuration.
     *
     * @return SSLContext
     */
    SSLContext getSslContext();

    /**
     * Reads the HTTP proxy configuration set in IDE configuration and returns the proxy configuration for the target URL.
     *
     * @param targetUrl The target URL.
     * @return Proxy configuration for the target URL.
     */
    ProxyConfiguration getProxyConfForTargetUrl(String targetUrl);

    /**
     * @return connection retries.
     */
    int getConnectionRetries();

    /**
     * @return connection timeout.
     */
    int getConnectionTimeout();

    /**
     * Returns external resources repository name. If it's not configured, null is returned.
     *
     * @return External resources repository name, if configured.
     */
    String getExternalResourcesRepo();

    @SuppressWarnings("unused")
    default boolean areCredentialsSet() {
        return isNoneBlank(getUsername(), getPassword()) && !isAllBlank(getUrl(), getXrayUrl(), getArtifactoryUrl());
    }
}
