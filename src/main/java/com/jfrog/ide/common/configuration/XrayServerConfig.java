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

    ProxyConfig getProxyConfig(String hostUrl);

    @SuppressWarnings("unused")
    default boolean areCredentialsSet() {
        return StringUtils.isNoneBlank(getUrl(), getUsername(), getPassword());
    }
}
