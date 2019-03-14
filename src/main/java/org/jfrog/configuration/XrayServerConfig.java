package org.jfrog.configuration;

import org.apache.commons.lang3.StringUtils;

/**
 * @author yahavi
 */
public interface XrayServerConfig {
    String getUrl();

    String getUsername();

    String getPassword();

    @SuppressWarnings("unused")
    default boolean areCredentialsSet() {
        return StringUtils.isNoneBlank(getUrl(), getUsername(), getPassword());
    }
}
