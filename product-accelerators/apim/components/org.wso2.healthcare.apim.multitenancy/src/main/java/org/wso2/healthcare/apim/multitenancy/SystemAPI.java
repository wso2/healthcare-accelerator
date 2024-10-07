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

package org.wso2.healthcare.apim.multitenancy;

/**
 * Represent system API
 */
public class SystemAPI {

    private String name;
    private String tenantDomain;
    private String configDefinition;

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getConfigDefinition() {

        return configDefinition;
    }

    public void setConfigDefinition(String configDefinition) {

        this.configDefinition = configDefinition;
    }

    public String getTenantDomain() {

        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {

        this.tenantDomain = tenantDomain;
    }
}
