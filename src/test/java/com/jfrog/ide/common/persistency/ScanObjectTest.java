package com.jfrog.ide.common.persistency;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author yahavi
 **/
public class ScanObjectTest {

    @Test
    public void testInvalidated() {
        ScanCacheObject scanCacheObject = new ScanCacheObject();
        Assert.assertFalse(scanCacheObject.isInvalidated());

        scanCacheObject.setLastUpdated(0);
        Assert.assertTrue(scanCacheObject.isInvalidated());

        scanCacheObject.setLastUpdated(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6));
        Assert.assertFalse(scanCacheObject.isInvalidated());
    }

}
