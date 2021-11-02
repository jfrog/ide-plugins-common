package com.jfrog.ide.common.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import org.jfrog.build.client.ProxyConfiguration;

import javax.net.ssl.SSLContext;

public class JfrogCliServerConfig implements ServerConfig {

    private JsonNode serverConfig;
    private final String USER_NAME = "user";
    private final String PASSWORD = "password";
    private final String ACCESS_TOKEN = "accessToken";
    private final String URL = "url";
    private final String ARTIFACTORY_URL = "artifactoryUrl";
    private final String XRAY_URL = "xrayUrl";

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

    public String getAccessToken() {
        return getValueFromJson(ACCESS_TOKEN);
    }

    @Override
    public String getProject() {
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
