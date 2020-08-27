package com.jfrog.ide.common.configuration;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.client.ProxyConfiguration;

import javax.net.ssl.SSLContext;

/**
 * @author yahavi
 */
public interface XrayServerConfig {
    String getUrl();

    String getUsername();

    String getPassword();

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
     * Reads the http proxy configuration set in IDE configuration and returns the proxy configuration for the Xray URL.
     *
     * @param xrayUrl - Xray url.
     * @return proxy config for the Xray URL.
     */
    ProxyConfiguration getProxyConfForTargetUrl(String xrayUrl);

    /**
     * @return connection retries.
     */
    int getConnectionRetries();

    /**
     * @return connection timeout.
     */
    int getConnectionTimeout();

    @SuppressWarnings("unused")
    default boolean areCredentialsSet() {
        return StringUtils.isNoneBlank(getUrl(), getUsername(), getPassword());
    }
}
