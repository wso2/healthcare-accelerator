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
package org.wso2.healthcare.consentmgt.ui;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.consent.mgt.core.ConsentManager;
import org.wso2.carbon.consent.mgt.core.exception.ConsentManagementException;
import org.wso2.carbon.consent.mgt.core.model.PIICategory;
import org.wso2.carbon.consent.mgt.core.model.PIICategoryValidity;
import org.wso2.carbon.consent.mgt.core.model.Purpose;
import org.wso2.carbon.consent.mgt.core.model.PurposeCategory;
import org.wso2.carbon.consent.mgt.core.model.Receipt;
import org.wso2.carbon.consent.mgt.core.model.ReceiptInput;
import org.wso2.carbon.consent.mgt.core.model.ReceiptPurposeInput;
import org.wso2.carbon.consent.mgt.core.model.ReceiptServiceInput;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.healthcare.consentmgt.core.ConsentPurposeMapper;
import org.wso2.healthcare.consentmgt.core.exception.FHIRConsentMgtException;
import org.wso2.healthcare.consentmgt.core.model.SPPurposeMapping;
import org.wso2.healthcare.consentmgt.ui.model.ConsentData;
import org.wso2.healthcare.consentmgt.ui.service.SessionDataResolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * consent management api util class
 */
public class ConsentMgtUtils {

    private static final Log log = LogFactory.getLog(ConsentMgtUtils.class);

    public static List<PurposeCategory> getConsentCategories(Integer limit, Integer offset) {
        if (limit == null) {
            limit = 0;
        }

        if (offset == null) {
            offset = 0;
        }
        try {
            return getConsentManager().listPurposeCategories(limit, offset);
        } catch (ConsentManagementException e) {
            log.error("Error occurred while getting consent purpose categories.", e);
        }
        return null;
    }

    public static List<PIICategory> getPIICategories(Integer limit, Integer offset) {
        if (limit == null) {
            limit = 0;
        }

        if (offset == null) {
            offset = 0;
        }

        try {
            return getConsentManager().listPIICategories(limit, offset);
        } catch (ConsentManagementException e) {
            log.error("Error occurred while getting pii categories.", e);
        }
        return null;
    }

    public static List<PIICategory> getPIICategoriesForGivenNames(List<String> displayNames) {
        List<PIICategory> piiCategories = getPIICategories(null, null);
        List<PIICategory> piiCategoriesForGivenNames = new ArrayList<>();
        if (piiCategories != null) {
            for (PIICategory piiCategory : piiCategories) {
                for (String displayName : displayNames) {
                    if (piiCategory.getDisplayName().equals(displayName)) {
                        piiCategoriesForGivenNames.add(piiCategory);
                        break;
                    }
                }
            }
        }
        return piiCategoriesForGivenNames;
    }

    public static List<Purpose> getAllConsentPurposes(Integer limit, Integer offset) {
        if (limit == null) {
            limit = 0;
        }

        if (offset == null) {
            offset = 0;
        }

        try {
            return getConsentManager().listPurposes(limit, offset);
        } catch (ConsentManagementException e) {
            log.error("Error occurred while getting consent purposes.", e);
        }
        return null;
    }

    public static List<SPPurposeMapping> getConsentPurposes(String appName) {
        try {
            return getConsentPurposeMapper().getSPPurposesByAppName(appName);
        } catch (FHIRConsentMgtException e) {
            log.error("Error while getting consent purposes for the app with name: " + appName, e);
        }
        return null;
    }

    public static List<SPPurposeMapping> getConsentPurposesByConsumerKey(String key) {
        try {
            return getConsentPurposeMapper().getSPPurposesByAppId(key);
        } catch (FHIRConsentMgtException e) {
            log.error("Error while getting consent purposes for the app for key:" + key, e);
        }
        return null;
    }

    public static Purpose getConsentPurposeById(int purposeId) {

        try {
            return getConsentManager().getPurpose(purposeId);
        } catch (ConsentManagementException e) {
            log.error("Error occurred while getting consent purposes.", e);
        }
        return null;
    }

    public static Receipt getConsentReceiptForUser(String loggedInUser, String tenantDomain,
            String application) {
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(loggedInUser);
        try {
            List<org.wso2.carbon.consent.mgt.core.model.ReceiptListResponse> activeReceipts = getConsentManager()
                    .searchReceipts(0, 0, loggedInUser, tenantDomain, application, "ACTIVE");
            for (org.wso2.carbon.consent.mgt.core.model.ReceiptListResponse activeReceipt : activeReceipts) {
                String consentReceiptId = activeReceipt.getConsentReceiptId();
                return getConsentManager().getReceipt(consentReceiptId);
            }
        } catch (ConsentManagementException e) {
            log.error("Error occurred while retrieving consent response.", e);
        }
        return null;
    }

    public static void saveConsent(String consentStr) {
        Gson gson = new Gson();
        ConsentData consentData = gson.fromJson(consentStr, ConsentData.class);
        ReceiptInput receiptInput = new ReceiptInput();
        if (StringUtils.isNotBlank(consentData.getSessionDataKeyConsent())) {
            String loggedInUser = consentData.getSessionDataKeyConsent();
            if (StringUtils.isNotBlank(loggedInUser)) {
                receiptInput.setPiiPrincipalId(loggedInUser);
            }
        }
        //by default everything is opted out otherwise specifically mentioned
        String policyURL = "https://terminology.hl7.org/CodeSystem/v3-ActCode#OPTOUT";
        receiptInput.setPolicyUrl(policyURL);
        receiptInput.setJurisdiction("CA");
        receiptInput.setLanguage("EN");
        receiptInput.setCollectionMethod("Sign-in Flow");
        ReceiptServiceInput receiptServiceInput = new ReceiptServiceInput();
        receiptServiceInput.setService(consentData.getAppId());
        receiptServiceInput.setTenantDomain(consentData.getTenantDomain());
        receiptServiceInput.setSpDisplayName(consentData.getAppId());
        List<org.wso2.healthcare.consentmgt.ui.model.Purpose> purposes = consentData.getPurposes();
        List<ReceiptPurposeInput> receiptPurposeInputList = new ArrayList<>();
        for (org.wso2.healthcare.consentmgt.ui.model.Purpose purpose : purposes) {
            ReceiptPurposeInput purposeInput = new ReceiptPurposeInput();
            purposeInput.setPurposeId(Integer.valueOf(purpose.getPurposeId()));
            List<Integer> categoryList = new ArrayList<>();
            //todo get from user
            categoryList.add(1);
            // set purpose category
            purposeInput.setPurposeCategoryId(categoryList);
            purposeInput.setConsentType("EXPLICIT");
            //todo: set from user input
            purposeInput.setPrimaryPurpose(true);
            if (StringUtils.isNotBlank(purpose.getTermination())) {
                purposeInput.setTermination(purpose.getTermination());
            } else {
                purposeInput.setTermination("VALID_UNTIL:INDEFINITE");
            }
            purposeInput.setThirdPartyDisclosure(false);
            purposeInput.setThirdPartyName("");
            List<org.wso2.healthcare.consentmgt.ui.model.PIICategory> piiCategories = purpose.getPiiCategories();
            List<PIICategoryValidity> piiCategoryValidityList = new ArrayList<>();
            for (org.wso2.healthcare.consentmgt.ui.model.PIICategory piiCategory : piiCategories) {
                if (piiCategory.isSelected()) {
                    PIICategoryValidity piiCategoryValidity = new PIICategoryValidity(
                            Integer.valueOf(piiCategory.getCategoryId()), "VALID_UNTIL:INDEFINITE");
                    piiCategoryValidityList.add(piiCategoryValidity);
                }
            }
            purposeInput.setPiiCategory(piiCategoryValidityList);
            if (piiCategoryValidityList.size() > 0) {
                receiptPurposeInputList.add(purposeInput);
            }
        }
        List<ReceiptServiceInput> receiptServiceInputs = new ArrayList<>();
        receiptServiceInput.setPurposes(receiptPurposeInputList);
        receiptServiceInputs.add(receiptServiceInput);
        receiptInput.setServices(receiptServiceInputs);

        try {
            getConsentManager().addConsent(receiptInput);
        } catch (ConsentManagementException e) {
            log.error("Error occurred while saving consent.", e);
        }
    }

    public static ConsentManager getConsentManager() {

        return (ConsentManager) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getOSGiService(ConsentManager.class, null);
    }

    public static ConsentPurposeMapper getConsentPurposeMapper() {
        return (ConsentPurposeMapper) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getOSGiService(ConsentPurposeMapper.class, null);
    }

    public static String getLoggedInUser(String keyType, String correlationKey, String parameters) {

        Set<String> filter;
        if (StringUtils.isNotBlank(parameters)) {
            filter = new HashSet<>(Arrays.asList(parameters.split(",")));
        } else {
            filter = Collections.emptySet();
        }
        Map<String, Serializable> paramMap = SessionDataResolver.getInstance().getParameterMap(correlationKey,
                filter, false);
        if (!"OauthConsentKey".equals(keyType)) {
           return null;
        }
        return (String) paramMap.get("loggedInUser");
    }
}
