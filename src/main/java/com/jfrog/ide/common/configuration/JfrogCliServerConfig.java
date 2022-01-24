package com.jfrog.ide.common.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import org.jfrog.build.client.ProxyConfiguration;

import javax.net.ssl.SSLContext;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
/**
 * This class is used for managing JFrog CLI's configuration, so that is can be used by the IDEs.
 *
 * @author tala
 */
public class JfrogCliServerConfig implements ServerConfig {

    private final JsonNode serverConfig;
    private final static String USER_NAME = "user";
    private final static String PASSWORD = "password";
    private final static String ACCESS_TOKEN = "accessToken";
    private final static String REFRESH_TOKEN = "refreshToken";
    private final static String URL = "url";
    private final static String ARTIFACTORY_URL = "artifactoryUrl";
    private final static String XRAY_URL = "xrayUrl";

    public JfrogCliServerConfig(JsonNode serverConfigNode) {
        this.serverConfig = serverConfigNode;
    }

    @Override
    public String getUrl() {
        return getValueFromJson(URL);
    }

    @Override
    public String getXrayUrl() {
        return getValueFromJson(XRAY_URL);
    }

    @Override
    public String getArtifactoryUrl() {
        return getValueFromJson(ARTIFACTORY_URL);
    }

    @Override
    public String getUsername() {
        return getValueFromJson(USER_NAME);
    }

    @Override
    public String getPassword() {
        return getValueFromJson(PASSWORD);
    }

    @Override
    public String getAccessToken() {
        // Prefer username/password if possible.
        if (isNotBlank(getValueFromJson(USER_NAME))) {
            return "";
        }
        return getValueFromJson(ACCESS_TOKEN);
    }

    @Override
    public PolicyType getPolicyType() {
        return null;
    }

    @Override
    public String getProject() {
        return null;
    }

    @Override
    public String getWatches() {
        return null;
    }

    @Override
    public boolean isInsecureTls() {
        return false;
    }

    @Override
    public SSLContext getSslContext() {
        return null;
    }

    @Override
    public ProxyConfiguration getProxyConfForTargetUrl(String xrayUrl) {
        return null;
    }

    @Override
    public int getConnectionRetries() {
        return 0;
    }

    @Override
    public int getConnectionTimeout() {
        return 0;
    }

    private String getValueFromJson(String fieldName) {
        return serverConfig.get(fieldName) != null ? serverConfig.get(fieldName).asText() : "";
    }
}
