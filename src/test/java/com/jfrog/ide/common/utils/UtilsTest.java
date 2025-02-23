package com.jfrog.ide.common.utils;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.jfrog.ide.common.utils.Utils.resolveArtifactoryUrl;
import static com.jfrog.ide.common.utils.Utils.resolveXrayUrl;
import static org.testng.Assert.assertEquals;


public class UtilsTest {

    @DataProvider
    public static Object[][] xrayUrlDataProvider() {
        return new Object[][]{
                {"", "", ""},
                {"", "https://acme.jfrog.io", "https://acme.jfrog.io/xray"},
                {"", "https://acme.jfrog.io/", "https://acme.jfrog.io/xray"},
                {"https://acme.jfrog.io/xray", "https://acme.jfrog.io", "https://acme.jfrog.io/xray"},
                {"https://acme.jfrog.io/xray/", "https://acme.jfrog.io", "https://acme.jfrog.io/xray"}
        };
    }

    @Test(dataProvider = "xrayUrlDataProvider")
    public void testResolveXrayUrl(String inputXrayUrl, String inputPlatformUrl, String expectedXrayUrl) {
        assertEquals(resolveXrayUrl(inputXrayUrl, inputPlatformUrl), expectedXrayUrl);
    }

    @DataProvider
    public static Object[][] artifactoryUrlDataProvider() {
        return new Object[][]{
                {"", "", ""},
                {"", "https://acme.jfrog.io", "https://acme.jfrog.io/artifactory"},
                {"", "https://acme.jfrog.io/", "https://acme.jfrog.io/artifactory"},
                {"https://acme.jfrog.io/artifactory", "https://acme.jfrog.io", "https://acme.jfrog.io/artifactory"},
                {"https://acme.jfrog.io/artifactory/", "https://acme.jfrog.io", "https://acme.jfrog.io/artifactory"}
        };
    }

    @Test(dataProvider = "artifactoryUrlDataProvider")
    public void testResolveArtifactoryUrl(String inputArtifactoryUrl, String inputPlatformUrl, String expectedArtifactoryUrl) {
        assertEquals(resolveArtifactoryUrl(inputArtifactoryUrl, inputPlatformUrl), expectedArtifactoryUrl);
    }

}
