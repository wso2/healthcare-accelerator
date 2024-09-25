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

package org.wso2.healthcare.consentmgt.core;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.consent.mgt.core.model.PIICategory;
import org.wso2.carbon.consent.mgt.core.model.Purpose;
import org.wso2.carbon.consent.mgt.core.model.PurposePIICategory;
import org.wso2.healthcare.consentmgt.core.model.FhirClaim;
import org.wso2.healthcare.consentmgt.core.model.FhirClaimAttribute;
import org.wso2.healthcare.consentmgt.core.model.FhirPurpose;
import org.wso2.healthcare.consentmgt.core.util.ConsentMgtUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * This class is used to build the system default consent purposes and provisioning of built purposes.
 */
public class ConsentPurposeBuilder {

    private static final Log log = LogFactory.getLog(
            org.wso2.healthcare.consentmgt.core.ConsentPurposeBuilder.class);

    private List<PIICategory> registeredPIICategories;
    private List<Purpose> registeredPurposes;
    private final Gson gsonBuilder = new Gson();

    public ConsentPurposeBuilder() {
        registeredPIICategories = getRegisteredPIICategories();
        registeredPurposes = getRegisteredPurposes();
    }

    /**
     * Parses the consent purpose config file and provision them in OH server.
     */
    public void provisionConsentData() {
        InputStream claimConfigsInputStream = org.wso2.healthcare.consentmgt.core.ConsentPurposeBuilder.class
                .getResourceAsStream("/fhir-claim-config.json");
        FhirClaim[] fhirClaims = gsonBuilder
                .fromJson(new InputStreamReader(claimConfigsInputStream, StandardCharsets.UTF_8), FhirClaim[].class);
        for (FhirClaim fhirClaim : fhirClaims) {
            String resource = fhirClaim.getResource();
            List<FhirClaimAttribute> attributes = fhirClaim.getAttributes();
            if (attributes != null) {
                for (FhirClaimAttribute attribute : attributes) {
                    String categoryName =
                            attribute.getDisplayName() + "--TYPE:FHIR_PATH--VALUE:" + attribute.getMappedAttribute();
                    if (isNewPIICategory(categoryName)) {
                        PIICategory piiCategory = new PIICategory(categoryName, attribute.getDescription(), false,
                                attribute.getDisplayName());
                        addPIICategory(piiCategory);
                    }
                    fhirClaim.getFhirPurpose().getPiiCategoryNames().add(categoryName);
                }
            } else {
                String categoryName = resource + "--TYPE:FHIR_RESOURCE--VALUE:" + resource;
                if (isNewPIICategory(categoryName)) {
                    PIICategory piiCategory = new PIICategory(categoryName, resource, false, resource);
                    addPIICategory(piiCategory);
                }
                fhirClaim.getFhirPurpose().getPiiCategoryNames().add(resource);
            }
            addConsentPurpose(fhirClaim.getFhirPurpose());
        }
    }

    private void addConsentPurpose(FhirPurpose purpose) {
        if (purpose != null && !isAlreadyAddedPurpose(purpose.getName())) {
            Purpose purposeToBePersisted = new Purpose(purpose.getName(), purpose.getDescription(), purpose.getGroup(),
                    purpose.getGroupType());
            List<String> piiCategoryNames = purpose.getPiiCategoryNames();
            List<PIICategory> registeredPIICategories = getRegisteredPIICategories();
            for (PIICategory registeredPIICategory : registeredPIICategories) {
                if (piiCategoryNames.contains(registeredPIICategory.getName())) {
                    PIICategory piiCategory = new PIICategory(registeredPIICategory.getId());
                    PurposePIICategory purposePIICategory = new PurposePIICategory(piiCategory, false);
                    purposeToBePersisted.getPurposePIICategories().add(purposePIICategory);
                }
            }
            Purpose persistedPurpose = ConsentMgtUtils.addPurpose(purposeToBePersisted);
            if (persistedPurpose != null && log.isDebugEnabled()) {
                log.debug("Consent purpose is added: " + persistedPurpose.getName());
            }
        }
    }

    private void addPIICategory(PIICategory piiCategory) {
        PIICategory piiCategoryPersisted = ConsentMgtUtils.addPIICategory(piiCategory);
        if (piiCategoryPersisted != null && log.isDebugEnabled()) {
            log.debug("PII category is added: " + piiCategoryPersisted.getName());
        }
    }

    private boolean isNewPIICategory(String piiCategoryName) {
        if (registeredPIICategories != null) {
            for (PIICategory registeredPIICategory : registeredPIICategories) {
                if (piiCategoryName.equals(registeredPIICategory.getName())) {
                    return false;
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("PII Category: " + piiCategoryName + " is already added.");
        }
        return true;
    }

    private boolean isAlreadyAddedPurpose(String purposeName) {
        if (registeredPurposes != null) {
            for (Purpose registeredPurpose : registeredPurposes) {
                if (purposeName.equals(registeredPurpose.getName())) {
                    return true;
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Purpose: " + purposeName + " is already added.");
        }
        return false;
    }

    private List<PIICategory> getRegisteredPIICategories() {
        return ConsentMgtUtils.listPIICategories();
    }

    private List<Purpose> getRegisteredPurposes() {
        return ConsentMgtUtils.listPurposes();
    }
}
