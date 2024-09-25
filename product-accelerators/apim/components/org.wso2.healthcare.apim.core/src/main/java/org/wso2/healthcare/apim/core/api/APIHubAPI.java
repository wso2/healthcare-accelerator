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

package org.wso2.healthcare.apim.core.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.caching.OHCacheManager;
import org.wso2.healthcare.apim.core.config.APIHubConfig;

import javax.cache.CacheManager;

/**
 * Internal Java API for interactions related for APIHub
 */
public class APIHubAPI {

    private static final Log LOG = LogFactory.getLog(APIHubAPI.class);

    /**
     * Retrieve message from cache
     *
     * @param resourceQuery
     * @return
     */
    public static Object getCachedResponse(String resourceQuery) {
        try {
            OHCacheManager cacheManager = OpenHealthcareEnvironment.getInstance().getOHCacheManager();
            return cacheManager.getCachedObject(APIHubConfig.HEALTHCARE_APIHUB_CACHE, resourceQuery);
        } catch (OpenHealthcareException e) {
            LOG.error("Error occurred while retrieving cached object", e);
            return null;
        }
    }

    /**
     * Add message to cache
     *
     * @param resourceQuery
     * @param message
     */
    public static void cacheResponse(String resourceQuery, Object message) {
        try {
            OHCacheManager cacheManager = OpenHealthcareEnvironment.getInstance().getOHCacheManager();
            cacheManager.addToCache(APIHubConfig.HEALTHCARE_APIHUB_CACHE, resourceQuery, message);
        } catch (OpenHealthcareException e) {
            LOG.error("Error occurred while adding message to APIHub message cache", e);
        }
    }
}
