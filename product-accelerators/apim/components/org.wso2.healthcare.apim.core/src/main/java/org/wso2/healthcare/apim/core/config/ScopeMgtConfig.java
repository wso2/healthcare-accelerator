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

import org.wso2.carbon.apimgt.api.model.Scope;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains role and scopes related configs
 */
public class ScopeMgtConfig {

    private List<String> roles;
    private boolean enableFHIRScopeToWSO2ScopeMapping;
    private final ArrayList<Scope> scopes;

    public ScopeMgtConfig() {

        scopes = new ArrayList<>();
    }

    public void setRoles(List<String> roles) {

        this.roles = roles;
    }

    public void addScope(Scope scope) {

        this.scopes.add(scope);
    }

    public List<String> getRoles() {

        return roles;
    }

    public ArrayList<Scope> getScopes() {

        return scopes;
    }

    public boolean isFHIRScopeToWSO2ScopeMappingEnabled() {

        return enableFHIRScopeToWSO2ScopeMapping;
    }

    public void setEnableFHIRScopeToWSO2ScopeMapping(boolean enableFHIRScopeToWSO2ScopeMapping) {

        this.enableFHIRScopeToWSO2ScopeMapping = enableFHIRScopeToWSO2ScopeMapping;
    }
}
