package com.jfrog.ide.common.persistency;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * Test the scan cache object.
 *
 * @author yahavi
 */
public class ScanObjectTest {

    @Test
    public void testIsExpired() {
        ScanCacheObject scanCacheObject = new ScanCacheObject();
        Assert.assertFalse(scanCacheObject.isExpired());

        scanCacheObject.setLastUpdated(0);
        Assert.assertTrue(scanCacheObject.isExpired());

        scanCacheObject.setLastUpdated(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6));
        Assert.assertFalse(scanCacheObject.isExpired());
    }

}
