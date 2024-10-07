/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.apim.core.caching;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.impl.CacheImpl;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.config.CacheConfig;

import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.CacheConfiguration;
import javax.cache.CacheManager;
import javax.cache.Caching;

/**
 * Responsible for managing cache
 */
public class OHCacheManager {

    private static final Log LOG = LogFactory.getLog(OHCacheManager.class);
    private static final String HEALTHCARE_CACHE_MANAGER = "HEALTHCARE_CACHE_MANAGER";
    private CacheManager cacheManager;

    public OHCacheManager() {
        initialize();
    }

    private void initialize() {
        //Create cache manager
        cacheManager = Caching.getCacheManager(HEALTHCARE_CACHE_MANAGER);
        LOG.info("Cache manager initialized. Status:" + cacheManager.getStatus());
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Function to create a cache
     *
     * @param cacheConfig
     */
    public void createCache(CacheConfig cacheConfig) {
        Cache<?,?> cache = cacheManager.
                createCacheBuilder(cacheConfig.getName()).
                setExpiry(CacheConfiguration.ExpiryType.MODIFIED, new CacheConfiguration.Duration(TimeUnit.SECONDS,
                        cacheConfig.getExpiry())).
                setExpiry(CacheConfiguration.ExpiryType.ACCESSED, new CacheConfiguration.Duration(TimeUnit.SECONDS,
                        cacheConfig.getExpiry())).
                setStoreByValue(false).
                build();
        if (cache instanceof CacheImpl) {
            CacheImpl<?,?> carbonCache = (CacheImpl<?,?>) cache;
            carbonCache.setCapacity(cacheConfig.getCapacity());
        }
    }

    /**
     * Function to add cache entry to targeted cache
     *
     * @param cacheName
     * @param key
     * @param cachedObject
     * @throws OpenHealthcareException
     */
    public void addToCache(String cacheName, String key, Object cachedObject) throws OpenHealthcareException {
        Cache<String , Object> cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new OpenHealthcareException("Cache with name : " + cacheName + " not found");
        }
        cache.put(key, cachedObject);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Entry added to cache [" + cacheName + "] key : " + key);
        }
    }

    /**
     * Function to get cache
     *
     * @param cacheName
     * @param key
     * @return
     * @throws OpenHealthcareException
     */
    public Object getCachedObject(String cacheName, String key) throws OpenHealthcareException {
        Cache<String , Object> cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new OpenHealthcareException("Cache with name : " + cacheName + " not found");
        }
        Object obj = cache.get(key);
        if (LOG.isDebugEnabled()) {
            if (obj != null) {
                LOG.debug("Entry found in cache [" + cacheName + "] for key : " + key);
            } else {
                LOG.debug("Entry not found in cache [" + cacheName + "] for key : " + key);
            }
        }
        return obj;
    }
}
