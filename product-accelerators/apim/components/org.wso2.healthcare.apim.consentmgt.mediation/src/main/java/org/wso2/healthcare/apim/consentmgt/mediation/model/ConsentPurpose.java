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

package org.wso2.healthcare.apim.consentmgt.mediation.model;

import java.util.ArrayList;
import java.util.List;

public class ConsentPurpose {
    private String purpose;
    private int purposeId;
    private String purposeDescription;
    private List<String> purposeCategory;
    private String consentType;
    private List<PIICategoryValidity> piiCategory;
    private boolean primaryPurpose;
    private String termination;
    private boolean thirdPartyDisclosure;
    private String thirdPartyName;
    private int serviceToPurposeId;

    public ConsentPurpose() {
    }

    public String getPurpose() {
        return this.purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public List<String> getPurposeCategory() {
        return this.purposeCategory;
    }

    public void setPurposeCategory(List<String> purposeCategory) {
        this.purposeCategory = purposeCategory;
    }

    public String getConsentType() {
        return this.consentType;
    }

    public void setConsentType(String consentType) {
        this.consentType = consentType;
    }

    public List<PIICategoryValidity> getPiiCategory() {
        return (List)(this.piiCategory == null ? new ArrayList() : this.piiCategory);
    }

    public void setPiiCategory(List<PIICategoryValidity> piiCategory) {
        this.piiCategory = piiCategory;
    }

    public boolean isPrimaryPurpose() {
        return this.primaryPurpose;
    }

    public void setPrimaryPurpose(boolean primaryPurpose) {
        this.primaryPurpose = primaryPurpose;
    }

    public String getTermination() {
        return this.termination;
    }

    public void setTermination(String termination) {
        this.termination = termination;
    }

    public boolean isThirdPartyDisclosure() {
        return this.thirdPartyDisclosure;
    }

    public void setThirdPartyDisclosure(boolean thirdPartyDisclosure) {
        this.thirdPartyDisclosure = thirdPartyDisclosure;
    }

    public String getThirdPartyName() {
        return this.thirdPartyName;
    }

    public void setThirdPartyName(String thirdPartyName) {
        this.thirdPartyName = thirdPartyName;
    }

    public int getServiceToPurposeId() {
        return this.serviceToPurposeId;
    }

    public void setServiceToPurposeId(int serviceToPurposeId) {
        this.serviceToPurposeId = serviceToPurposeId;
    }

    public int getPurposeId() {
        return this.purposeId;
    }

    public void setPurposeId(int purposeId) {
        this.purposeId = purposeId;
    }

    public String getPurposeDescription() {
        return this.purposeDescription;
    }

    public void setPurposeDescription(String purposeDescription) {
        this.purposeDescription = purposeDescription;
    }
}

