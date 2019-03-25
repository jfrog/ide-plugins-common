package com.jfrog.ide.common.utils;

import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.services.summary.ComponentsImpl;
import com.jfrog.xray.client.services.summary.Components;
import com.jfrog.xray.client.services.system.Version;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

import java.io.IOException;

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
            return "ERROR: Unsupported Xray version: " + xrayVersion + ", version " +
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
}
