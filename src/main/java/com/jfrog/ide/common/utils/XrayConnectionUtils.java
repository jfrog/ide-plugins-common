package com.jfrog.ide.common.utils;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.XrayClientBuilder;
import com.jfrog.xray.client.impl.services.summary.ComponentsImpl;
import com.jfrog.xray.client.services.summary.Components;
import com.jfrog.xray.client.services.system.Version;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static com.jfrog.ide.common.utils.Constants.MINIMAL_XRAY_VERSION_SUPPORTED;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public class XrayConnectionUtils {

    public static class Results {
        public static String success(Version xrayVersion) {
            return "Successfully connected to Xray version: " + xrayVersion.getVersion();
        }

        public static String error(Exception e) {
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return "Could not connect to Xray: " + message;
        }

        public static String unsupported(Version xrayVersion) {
            return "ERROR: Unsupported Xray version: " + xrayVersion.getVersion() + ", version " +
                    MINIMAL_XRAY_VERSION_SUPPORTED + " or above is required.";
        }
    }

    public static boolean isXrayVersionSupported(Version version) {
        return version.isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED);
    }

    /**
     * Send REST to Xray: summary/component with "testComponent". If exception thrown, return the reason.
     *
     * @param xrayClient - The xray client.
     * @return a pair of boolean and error details.
     * @throws IOException in case of connection issues.
     */
    public static Pair<Boolean, String> testComponentPermission(Xray xrayClient) throws IOException {
        try {
            Components testComponent = new ComponentsImpl();
            testComponent.addComponent("testComponent", "");
            xrayClient.summary().component(testComponent);
        } catch (HttpResponseException e) {
            switch (e.getStatusCode()) {
                case HttpStatus.SC_UNAUTHORIZED:
                    return Pair.of(false, e.getMessage() + ". Please check your credentials.");
                case HttpStatus.SC_FORBIDDEN:
                    return Pair.of(false, e.getMessage() + ". Please make sure that the user has 'View Components' permission in Xray.");
            }
        }
        return Pair.of(true, "");
    }

    public static XrayClientBuilder createXrayClientBuilder(ServerConfig serverConfig, Log logger) {
        return (XrayClientBuilder) new XrayClientBuilder()
                .setUrl(serverConfig.getXrayUrl())
                .setUserName(serverConfig.getUsername())
                .setPassword(serverConfig.getPassword())
                .setInsecureTls(serverConfig.isInsecureTls())
                .setSslContext(serverConfig.getSslContext())
                .setProxyConfiguration(serverConfig.getProxyConfForTargetUrl(serverConfig.getXrayUrl()))
                .setConnectionRetries(serverConfig.getConnectionRetries())
                .setTimeout(serverConfig.getConnectionTimeout())
                .setLog(logger);
    }

    public static ArtifactoryDependenciesClientBuilder createDependenciesClientBuilder(ServerConfig serverConfig, Log logger) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = serverConfig.isInsecureTls() ?
                SSLContextBuilder.create().loadTrustMaterial(TrustAllStrategy.INSTANCE).build() :
                serverConfig.getSslContext();
        return new ArtifactoryDependenciesClientBuilder()
                .setArtifactoryUrl(serverConfig.getArtifactoryUrl())
                .setUsername(serverConfig.getUsername())
                .setPassword(serverConfig.getPassword())
                .setProxyConfiguration(serverConfig.getProxyConfForTargetUrl(serverConfig.getArtifactoryUrl()))
                .setSslContext(sslContext)
                .setLog(logger);
    }
}
