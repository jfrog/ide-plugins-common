package com.jfrog.ide.common.utils;

import com.jfrog.ide.common.configuration.ServerConfig;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static com.jfrog.ide.common.utils.Constants.RECOMMENDED_ARTIFACTORY_VERSION_SUPPORTED;

/**
 * Represents connection utils for Artifactory.
 *
 * @author yahavi
 */
public class ArtifactoryConnectionUtils {

    public static class Results {
        public static String success(ArtifactoryVersion artifactoryVersion) {
            return "Successfully connected to Artifactory version: " + artifactoryVersion.toString();
        }

        public static String error(Exception e) {
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return "Could not connect to Artifactory: " + message;
        }

        public static String unsupported(ArtifactoryVersion artifactoryVersion) {
            return "Detected Artifactory version: " + artifactoryVersion.toString() + ". Version " + RECOMMENDED_ARTIFACTORY_VERSION_SUPPORTED +
                    " or above is recommended to get extended VCS information and hierarchical build dependencies tree.";
        }
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
