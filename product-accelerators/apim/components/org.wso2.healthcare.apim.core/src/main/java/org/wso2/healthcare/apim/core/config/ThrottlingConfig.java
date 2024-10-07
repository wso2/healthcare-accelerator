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

package org.wso2.healthcare.apim.core.config;

import org.wso2.carbon.apimgt.api.model.policy.ApplicationPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains throttling related configurations
 */
public class ThrottlingConfig {

    private boolean disableStandardProductThrottlePolicies = false;
    private List<ApplicationPolicy> applicationPolicies = new ArrayList<>(4);

    public boolean isDisableStandardProductThrottlePolicies() {
        return disableStandardProductThrottlePolicies;
    }

    public void setDisableStandardProductThrottlePolicies(boolean disableStandardProductThrottlePolicies) {
        this.disableStandardProductThrottlePolicies = disableStandardProductThrottlePolicies;
    }

    public void addApplicationPolicy(ApplicationPolicy applicationPolicy) {
        applicationPolicies.add(applicationPolicy);
    }

    public List<ApplicationPolicy> getApplicationPolicies() {
        return applicationPolicies;
    }
}
