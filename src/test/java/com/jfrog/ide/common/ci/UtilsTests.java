package com.jfrog.ide.common.ci;

import org.apache.commons.codec.EncoderException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.jfrog.ide.common.ci.Utils.createAqlForBuildArtifacts;
import static org.testng.Assert.assertEquals;

/**
 * @author yahavi
 **/
public class UtilsTests {

    @DataProvider
    private Object[][] buildPatternsProvider() {
        return new Object[][]{
                {"simpleBuild", "items.find({\"repo\":\"artifactory-build-info\",\"path\":{\"$match\":\"simpleBuild\"}}).include(\"name\",\"repo\",\"path\",\"created\").sort({\"$desc\":[\"created\"]}).limit(100)"},
                {"*", "items.find({\"repo\":\"artifactory-build-info\",\"path\":{\"$match\":\"*\"}}).include(\"name\",\"repo\",\"path\",\"created\").sort({\"$desc\":[\"created\"]}).limit(100)"},
                {"build/with/slash", "items.find({\"repo\":\"artifactory-build-info\",\"path\":{\"$match\":\"build?2Fwith?2Fslash\"}}).include(\"name\",\"repo\",\"path\",\"created\").sort({\"$desc\":[\"created\"]}).limit(100)"},
        };
    }

    @Test(dataProvider = "buildPatternsProvider")
    public void testCreateAqlForBuildArtifacts(String buildPattern, String expected) throws EncoderException {
        assertEquals(createAqlForBuildArtifacts(buildPattern, "artifactory-build-info"), expected);
    }
}
