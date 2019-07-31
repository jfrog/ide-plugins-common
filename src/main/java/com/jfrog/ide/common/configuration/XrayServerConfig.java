package com.jfrog.ide.common.configuration;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.client.http.model.ProxyConfig;

/**
 * @author yahavi
 */
public interface XrayServerConfig {
    String getUrl();

    String getUsername();

    String getPassword();

    /**
     * Reads the http proxy configuration set in IDE configuration and returns the proxy config for the Xray URL.
     *
     * @param xrayUrl - Xray url.
     * @return proxy config for the Xray URL.
     */
    ProxyConfig getProxyConfForTargetUrl(String xrayUrl);

    @SuppressWarnings("unused")
    default boolean areCredentialsSet() {
        return StringUtils.isNoneBlank(getUrl(), getUsername(), getPassword());
    }
}
