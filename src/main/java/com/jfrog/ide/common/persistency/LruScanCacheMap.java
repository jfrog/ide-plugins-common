package com.jfrog.ide.common.persistency;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The implementation of the scan cache. Contains a version and a map.
 * In case of incorrect version, it zaps the cache and starts over a new map.
 *
 * @author yahavi
 */
@Getter
@Setter
class LruScanCacheMap extends ScanCacheMap {

    LruScanCacheMap(int capacity) {
        artifactsMap = Collections.synchronizedMap(new LinkedHashMap<String, ScanCacheObject>(capacity) {

            /**
             * Runs after "put" to make sure the input capacity is not exceeded.
             * Delete the eldest entry if the capacity exceeded.
             *
             * @param eldest - The least recently inserted entry in the map.
             * @return true if maximum capacity exceeded.
             */
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ScanCacheObject> eldest) {
                return size() > capacity;
            }
        });
    }
}
