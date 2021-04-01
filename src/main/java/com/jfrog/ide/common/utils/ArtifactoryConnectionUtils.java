package com.jfrog.ide.common.utils;

import com.jfrog.ide.common.configuration.ServerConfig;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Represents connection utils for Artifactory.
 *
 * @author yahavi
 */
public class ArtifactoryConnectionUtils {

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
