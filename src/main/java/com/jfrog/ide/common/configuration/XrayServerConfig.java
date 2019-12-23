package com.jfrog.ide.common.configuration;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.client.http.model.ProxyConfig;
import org.jfrog.client.util.KeyStoreProvider;
import org.jfrog.client.util.KeyStoreProviderException;

/**
 * @author yahavi
 */
public interface XrayServerConfig {
    String getUrl();

    String getUsername();

    String getPassword();

    boolean isNoHostVerification();

    /**
     * Reads the certifications KeyStore provider in IDE configuration and returns the KeyStoreProvider.
     *
     * @return KeyStoreProvider
     */
    KeyStoreProvider getKeyStoreProvider() throws KeyStoreProviderException;

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
