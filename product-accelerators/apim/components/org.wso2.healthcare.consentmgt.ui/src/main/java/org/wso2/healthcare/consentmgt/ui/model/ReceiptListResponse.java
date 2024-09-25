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

/**
 * Consent receipt list response
 */
public class ReceiptListResponse {
    @SerializedName("consentReceiptID")
    private String consentReceiptId;
    private String language;
    private String piiPrincipalId;
    private String tenantDomain;
    private int tenantId;
    private String state;
    private String spDisplayName;
    private String spDescription;

    public ReceiptListResponse(String consentReceiptId, String language, String piiPrincipalId, int tenantId,
            String state, String spDisplayName, String spDescription) {
        this.consentReceiptId = consentReceiptId;
        this.language = language;
        this.piiPrincipalId = piiPrincipalId;
        this.tenantId = tenantId;
        this.state = state;
        this.spDisplayName = spDisplayName;
        this.spDescription = spDescription;
    }

    public String getConsentReceiptId() {
        return this.consentReceiptId;
    }

    public void setConsentReceiptId(String consentReceiptId) {
        this.consentReceiptId = consentReceiptId;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPiiPrincipalId() {
        return this.piiPrincipalId;
    }

    public void setPiiPrincipalId(String piiPrincipalId) {
        this.piiPrincipalId = piiPrincipalId;
    }

    public String getTenantDomain() {
        return this.tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getTenantId() {
        return this.tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getSpDisplayName() {
        return this.spDisplayName;
    }

    public void setSpDisplayName(String spDisplayName) {
        this.spDisplayName = spDisplayName;
    }

    public String getSpDescription() {
        return this.spDescription;
    }

    public void setSpDescription(String spDescription) {
        this.spDescription = spDescription;
    }
}
