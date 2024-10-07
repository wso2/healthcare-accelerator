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

package org.wso2.healthcare.consentmgt.core.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * This class holds the model structure for default FHIR consent purpose with FHIR resource.
 */
public class FhirClaim {
    @SerializedName("Resource")
    private String resource;
    @SerializedName("AttributeList")
    private List<FhirClaimAttribute> attributes;
    @SerializedName("Purpose")
    private FhirPurpose fhirPurpose;

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public List<FhirClaimAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<FhirClaimAttribute> attributes) {
        this.attributes = attributes;
    }

    public FhirPurpose getFhirPurpose() {
        return fhirPurpose;
    }

    public void setFhirPurpose(FhirPurpose fhirPurpose) {
        this.fhirPurpose = fhirPurpose;
    }
}
