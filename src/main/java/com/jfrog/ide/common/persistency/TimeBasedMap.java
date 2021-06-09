package com.jfrog.ide.common.persistency;

import lombok.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author yahavi
 **/
class TimeBasedMap extends LinkedHashMap<String, ScanCacheObject> {

    /**
     * We override put in order to avoid inserting expired elements.
     *
     * @param key   - key with which the specified value is to be associated
     * @param value - value to be associated with the specified key
     * @return the previous value associated with key, or null if expired or there was no mapping for key.
     */
    @Override
    public ScanCacheObject put(@NonNull String key, @NonNull ScanCacheObject value) {
        if (value.isExpired()) {
            return null;
        }
        return super.put(key, value);
    }

    /**
     * Runs after "put" to remove expired entries in cache and free memory.
     *
     * @param eldest - The least recently inserted entry in the map.
     * @return true if the eldest entry is expired.
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, ScanCacheObject> eldest) {
        return eldest.getValue().isExpired();
    }
}
