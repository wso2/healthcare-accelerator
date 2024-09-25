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

package org.wso2.healthcare.apim.consentmgt.mediation.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

public class CacheManager {

    private LoadingCache<String, CachedDecision> decisionCache;
    /**
     * Default cache timeout is set as 5 minutes.
     */
    private long timeout = 5000;
    /**
     * The size of the messages to be cached in memory. If this is -1 then cache can contain any number of messages.
     */
    private int inMemoryCacheSize = -1;

    public LoadingCache<String, CachedDecision> getDecisionCache() {
        if (decisionCache == null) {
            if (inMemoryCacheSize > -1) {
                decisionCache = CacheBuilder.newBuilder().expireAfterWrite(timeout, TimeUnit.SECONDS)
                        .maximumSize(inMemoryCacheSize).build(new CacheLoader<String, CachedDecision>() {
                            @Override public CachedDecision load(String hash) throws Exception {
                                return createCachedDecision(hash);
                            }
                        });
            } else {
                decisionCache = CacheBuilder.newBuilder().expireAfterWrite(timeout, TimeUnit.SECONDS)
                        .build(new CacheLoader<String, CachedDecision>() {
                            @Override public CachedDecision load(String hash) throws Exception {
                                return createCachedDecision(hash);
                            }
                        });
            }
        }
        return decisionCache;
    }

    private CachedDecision createCachedDecision(String hash) {
        return new CachedDecision(hash);
    }
}


