package com.jfrog.ide.common.persistency;

import lombok.Getter;
import lombok.Setter;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * The implementation of the scan cache. Contains a version and a map.
 * In case of incorrect version, it zaps the cache and starts over a new map.
 *
 * @author yahavi
 */
@Getter
@Setter
class XrayScanCacheMap extends ScanCacheMap {

    XrayScanCacheMap() {
        artifactsMap = Collections.synchronizedMap(new TimeBasedMap());
    }

    @Override
    void read(File file, Log logger) throws IOException {
        readCommonCache(file, logger);
    }
}
