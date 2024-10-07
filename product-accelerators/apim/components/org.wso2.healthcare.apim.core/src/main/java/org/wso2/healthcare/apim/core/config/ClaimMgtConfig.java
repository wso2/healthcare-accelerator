/*
 * Copyright (c) 2024 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License, Version
 *  2.0 (the "License"); you may not use this file except in compliance with
 *  the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.wso2.healthcare.apim.core.config;

/**
 * Configs related to identity claims.
 */
public class ClaimMgtConfig {
    private String claimUri = ConfigConstants.DEFAULT_PATIENT_ID_CLAIM_URI;
    private String patientIdKey = ConfigConstants.DEFAULT_PATIENT_ID_KEY;
    private String fhirUserClaimContext = ConfigConstants.DEFAULT_FHIRUSER_CLAIM_CONTEXT;
    private String fhirUserMappedLocalClaim = ConfigConstants.DEFAULT_FHIRUSER_MAPPED_LOCAL_CLAIM;
    private Boolean enable = false;

    public ClaimMgtConfig() {

    }

    public String getClaimUri() {

        return claimUri;
    }

    public void setClaimUri(String claimUri) {

        this.claimUri = claimUri;
    }

    public String getPatientIdKey() {

        return patientIdKey;
    }

    public void setPatientIdKey(String patientIdKey) {

        this.patientIdKey = patientIdKey;
    }

    public String getFhirUserClaimContext() {

        return fhirUserClaimContext;
    }

    public void setFhirUserClaimContext(String fhirUserClaimContext) {

        this.fhirUserClaimContext = fhirUserClaimContext;
    }

    public String getFhirUserMappedLocalClaim() {

        return fhirUserMappedLocalClaim;
    }

    public void setFhirUserMappedLocalClaim(String fhirUserMappedLocalClaim) {

        this.fhirUserMappedLocalClaim = fhirUserMappedLocalClaim;
    }

    public Boolean isEnable() {

        return enable;
    }

    public void setEnable(Boolean enable) {

        this.enable = enable;
    }
}
