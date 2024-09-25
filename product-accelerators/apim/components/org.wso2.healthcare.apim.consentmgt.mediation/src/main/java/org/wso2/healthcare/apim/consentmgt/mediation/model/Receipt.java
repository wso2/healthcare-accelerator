/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.healthcare.apim.consentmgt.mediation.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Receipt {
    @SerializedName("consentReceiptID")
    private String consentReceiptId;
    private String version;
    private String jurisdiction;
    private String collectionMethod;
    private String publicKey;
    private String language;
    private String piiPrincipalId;
    private long consentTimestamp;
    private List<PIIController> piiControllers;
    private String piiController;
    private List<ReceiptService> services;
    private String policyUrl;
    private boolean sensitive;
    private List<String> spiCat;
    private String state;
    private String tenantDomain;
    private int tenantId;

    public Receipt() {
    }

    public String getConsentReceiptId() {
        return this.consentReceiptId;
    }

    public void setConsentReceiptId(String consentReceiptId) {
        this.consentReceiptId = consentReceiptId;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getJurisdiction() {
        return this.jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getCollectionMethod() {
        return this.collectionMethod;
    }

    public void setCollectionMethod(String collectionMethod) {
        this.collectionMethod = collectionMethod;
    }

    public String getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
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

    public long getConsentTimestamp() {
        return this.consentTimestamp;
    }

    public void setConsentTimestamp(long consentTimestamp) {
        this.consentTimestamp = consentTimestamp;
    }

    public List<PIIController> getPiiControllers() {
        return this.piiControllers;
    }

    public void setPiiControllers(List<PIIController> piiControllers) {
        this.piiControllers = piiControllers;
    }

    public List<ReceiptService> getServices() {
        return this.services;
    }

    public void setServices(List<ReceiptService> services) {
        this.services = services;
    }

    public String getPolicyUrl() {
        return this.policyUrl;
    }

    public void setPolicyUrl(String policyUrl) {
        this.policyUrl = policyUrl;
    }

    public boolean isSensitive() {
        return this.sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public List<String> getSpiCat() {
        return this.spiCat;
    }

    public void setSpiCat(List<String> spiCat) {
        this.spiCat = spiCat;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTenantDomain() {
        return this.tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public int getTenantId() {
        return this.tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getPiiController() {
        return this.piiController;
    }

    public void setPiiController(String piiController) {
        this.piiController = piiController;
    }
}