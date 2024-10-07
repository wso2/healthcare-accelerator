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

package org.wso2.healthcare.apim.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.apim.core.caching.OHCacheManager;
import org.wso2.healthcare.apim.core.config.OpenHealthcareConfig;

/**
 * The Open-Healthcare runtime environment
 */
public class OpenHealthcareEnvironment {

    public static final String APIM_PROFILE_DEFAULT = "default";
    public static final String APIM_PROFILE_PUBLISHER = "api-publisher";

    private static final Log LOG = LogFactory.getLog(OpenHealthcareEnvironment.class);
    private static final OpenHealthcareEnvironment INSTANCE = new OpenHealthcareEnvironment();

    private boolean initialized = false;
    private String profile;
    private AsyncExecutor asyncExecutor = null;
//    private WorkflowHolder workflowHolder = null;
    private OpenHealthcareConfig config = null;
    private OHCacheManager ohCacheManager = null;

    private OpenHealthcareEnvironment() {
    }

    public static OpenHealthcareEnvironment getInstance() {
        return INSTANCE;
    }

    /**
     * Initialize Open-Healthcare environment
     *
     * @param config Open-Healthcare Configuration
     * @throws OpenHealthcareException
     */
    public void initialize(OpenHealthcareConfig config) throws OpenHealthcareException {
        if (config == null) {
            throw new OpenHealthcareException("Configuration is null, unable to initialize");
        }
        this.config = config;
        this.asyncExecutor = new AsyncExecutor();

        profile = System.getProperty("profile");
        LOG.info("Server Profile : " + profile);

        if (APIM_PROFILE_DEFAULT.equalsIgnoreCase(profile) || APIM_PROFILE_PUBLISHER.equalsIgnoreCase(profile)) {
            initializeCaching();
        }
        initialized = true;
    }

    /**
     * Get Async Executor to execute sync tasks
     *
     * @return
     * @throws OpenHealthcareException
     */
    public AsyncExecutor getAsyncExecutor() throws OpenHealthcareException {
        if (initialized) {
            return asyncExecutor;
        }
        throw new OpenHealthcareException("Open-Healthcare Environment is not initialized");
    }

    /**
     * Get APIM server profile
     *
     * @return
     */
    public String getProfile() {
        return profile;
    }

    /**
     * Get cache manager
     * @return
     */
    public OHCacheManager getOHCacheManager() {
        return ohCacheManager;
    }

    /**
     * Get open-healthcare config
     *
     * @return
     * @throws OpenHealthcareException
     */
    public OpenHealthcareConfig getConfig() throws OpenHealthcareException {
        if (initialized) {
            return config;
        }
        throw new OpenHealthcareException("Open-Healthcare Environment is not initialized");
    }

    private void initializeCaching() {
        ohCacheManager = new OHCacheManager();
        // initialize APIHub Cache
        ohCacheManager.createCache(config.getApiHubConfig().getCacheConfig());
    }
}
