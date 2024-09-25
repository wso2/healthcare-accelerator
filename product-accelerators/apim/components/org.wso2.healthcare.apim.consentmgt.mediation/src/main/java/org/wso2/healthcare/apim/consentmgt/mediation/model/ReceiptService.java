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

import java.util.List;

public class ReceiptService {
    private String service;
    private String spDisplayName;
    private String spDescription;
    private String tenantDomain;
    private int tenantId;
    List<ConsentPurpose> purposes;
    private int receiptToServiceId;

    public ReceiptService() {
    }

    public String getService() {
        return this.service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getTenantDomain() {
        return this.tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public List<ConsentPurpose> getPurposes() {
        return this.purposes;
    }

    public void setPurposes(List<ConsentPurpose> purposes) {
        this.purposes = purposes;
    }

    public int getReceiptToServiceId() {
        return this.receiptToServiceId;
    }

    public void setReceiptToServiceId(int receiptToServiceId) {
        this.receiptToServiceId = receiptToServiceId;
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
