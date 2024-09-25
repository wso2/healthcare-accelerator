/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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
package org.wso2.healthcare.consentmgt.ui.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth.cache.SessionDataCache;
import org.wso2.carbon.identity.oauth.cache.SessionDataCacheEntry;
import org.wso2.carbon.identity.oauth.cache.SessionDataCacheKey;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is used for access the session data set by the authentication framework.
 */
public class SessionDataResolver {

    private static final Log log = LogFactory.getLog(SessionDataResolver.class);

    private static final SessionDataResolver instance = new SessionDataResolver();

    public static SessionDataResolver getInstance() {
        return instance;
    }

    /**
     * Retrieve and optionally delete the parameters available for the given key. Optionally filter them if a non empty
     * filter is provided.
     * @param key The correlation key
     * @param filter Set of attributes which are required.
     * @param deleteOnRead Whether to remove the parameters on read.
     * @return Map where key represent the name of the parameter and value represent the value of the parameter
     */
    public Map<String, Serializable> getParameterMap(String key, Set<String> filter, boolean deleteOnRead) {

        SessionDataCacheKey cacheKey = new SessionDataCacheKey(key);
        SessionDataCacheEntry cacheEntry = SessionDataCache.getInstance().getValueFromCache(cacheKey);
        if (cacheEntry != null && !cacheEntry.getEndpointParams().isEmpty()) {
            Map<String, Serializable> endpointParams = new HashMap<>(cacheEntry.getEndpointParams());
            if (filter != null && !filter.isEmpty()) {
                endpointParams.keySet().retainAll(filter);
            }
            if (deleteOnRead) {
                cacheEntry.getEndpointParams().keySet().removeAll(endpointParams.keySet());
                SessionDataCache.getInstance().clearCacheEntry(cacheKey);
                SessionDataCache.getInstance().addToCache(cacheKey, cacheEntry);
            }
            return endpointParams;
        }
        if (log.isDebugEnabled()) {
           log.debug("No matching session data is found.");
        }
        return Collections.emptyMap();
    }

}
