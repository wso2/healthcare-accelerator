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
package org.wso2.healthcare.consentmgt.ui.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 *  Class for hold consent data
 */
public class ConsentData {
    @SerializedName("app_id")
    private String appId;
    private String tenantDomain;
    private String sessionDataKeyConsent;
    private List<Purpose> purposes;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public List<Purpose> getPurposes() {
        return purposes;
    }

    public void setPurposes(List<Purpose> purposes) {
        this.purposes = purposes;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public String getSessionDataKeyConsent() {
        return sessionDataKeyConsent;
    }

    public void setSessionDataKeyConsent(String sessionDataKeyConsent) {
        this.sessionDataKeyConsent = sessionDataKeyConsent;
    }
}
