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

package org.wso2.healthcare.apim.consentmgt.mediation;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.carbon.apimgt.gateway.handlers.security.AuthenticationContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.healthcare.apim.consentmgt.mediation.cache.CacheManager;
import org.wso2.healthcare.apim.consentmgt.mediation.cache.CachedDecision;
import org.wso2.healthcare.apim.consentmgt.mediation.cache.HashGenerator;
import org.wso2.healthcare.apim.consentmgt.mediation.decision.Attribute;
import org.wso2.healthcare.apim.consentmgt.mediation.decision.Code;
import org.wso2.healthcare.apim.consentmgt.mediation.decision.Decision;
import org.wso2.healthcare.apim.consentmgt.mediation.decision.DecisionResponse;
import org.wso2.healthcare.apim.consentmgt.mediation.decision.Obligation;
import org.wso2.healthcare.apim.consentmgt.mediation.model.ClaimMetaData;
import org.wso2.healthcare.apim.consentmgt.mediation.model.Receipt;
import org.wso2.healthcare.apim.consentmgt.mediation.util.ConsentMgtUtils;
import org.wso2.healthcare.apim.consentmgt.mediation.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * This mediator is used to process the consent access decision in the gateway.
 */
public class FHIRConsentManagementImpl extends AbstractMediator {

    private static final Log log = LogFactory.getLog(FHIRConsentManagementImpl.class);

    private final Gson gsonBuilder;
    private String optInPolicy;
    private String optOutPolicy;
    private String serverPolicyType;
    private HashGenerator hashGenerator;
    private CacheManager cacheManager;
    private boolean isDecisionCacheEnabled = false;

    public FHIRConsentManagementImpl() {
        hashGenerator = new HashGenerator();
        cacheManager = new CacheManager();
        gsonBuilder = new Gson();
        optInPolicy = System.getProperty("server.consent.enforcement.policy.optin");
        optOutPolicy = System.getProperty("server.consent.enforcement.policy.optout");
        serverPolicyType = System.getProperty("server.consent.enforcement.policy.type");
        isDecisionCacheEnabled = Boolean.parseBoolean(System.getProperty("server.consent.decision.cache.enable"));
        if (StringUtils.isBlank(serverPolicyType)) {
            serverPolicyType = Constants.Policy.DEFAULT_POLICY_TYPE;
        }
        if (StringUtils.isBlank(optInPolicy)) {
            optInPolicy = Constants.Policy.DEFAULT_OPT_IN;
        }
        if (StringUtils.isBlank(optOutPolicy)) {
            optOutPolicy = Constants.Policy.DEFAULT_OPT_OUT;
        }
    }

    @Override
    public boolean mediate(MessageContext messageContext) {

        AuthenticationContext apiAuthContext = (AuthenticationContext) messageContext
                .getProperty(Constants.API_AUTH_CONTEXT_PROP_NAME);
        String tenantDomain = apiAuthContext.getSubscriberTenantDomain();
        String piiPrincipalId = MultitenantUtils.getTenantAwareUsername(apiAuthContext.getUsername());

        String oauthAppName =
                apiAuthContext.getSubscriber() + "_" + apiAuthContext.getApplicationName() + "_" + apiAuthContext
                        .getKeyType();

        //setting to used for consent get receipt api validation
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(apiAuthContext.getUsername());

        Axis2MessageContext axis2smc = (Axis2MessageContext) messageContext;
        org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
        Object headers = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        //Setting Transport Headers
        if (headers instanceof Map) {
            Map headersMap = (Map) headers;
            String authorizationHeader = (String) headersMap.get(HttpHeaders.AUTHORIZATION);
            if (StringUtils.isNotBlank(authorizationHeader)) {
                try {
                    String decisionResponseStr;
                    CachedDecision decisionResponse = null;
                    String consentParamCombination = piiPrincipalId + ":" + tenantDomain + ":" + oauthAppName;
                    String hash = hashGenerator.getDigest(consentParamCombination);
                    if (StringUtils.isNotBlank(hash)) {
                        decisionResponse = cacheManager.getDecisionCache().get(hash);
                    }
                    //cache hit
                    if (isDecisionCacheEnabled && decisionResponse != null && StringUtils
                            .isNotBlank(decisionResponse.getDecision())) {
                        decisionResponseStr = decisionResponse.getDecision();
                        if (log.isDebugEnabled()) {
                            log.debug("cache hit for consent decision(hash): " + decisionResponse.getHashVal());
                        }
                    } else {
                        decisionResponseStr = gsonBuilder
                                .toJson(getDecisionResponse(piiPrincipalId, tenantDomain, oauthAppName,
                                        authorizationHeader));
                        if (decisionResponse != null) {
                            decisionResponse.setDecision(decisionResponseStr);
                        }
                    }
                    headersMap.put(Constants.X_WSO2_CONSENT_DECISION,
                            new String(new Base64().encode(decisionResponseStr.getBytes())));
                } catch (FHIRConsentMgtException e) {
                    handleException("Error occurred while getting consent decision.", e, messageContext);
                } catch (ExecutionException e) {
                    handleException("Error occurred while getting decision cache.", e, messageContext);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Authorization header is not available for the consent api call.");
                }
            }
        }
        return true;
    }

    private DecisionResponse getDecisionResponse(String piiPrincipalId, String tenantDomain, String oauthAppName,
            String authorizationHeader) throws FHIRConsentMgtException {
        Receipt activeConsentReceiptForUser = ConsentMgtUtils
                .getActiveConsentReceiptForUser(piiPrincipalId, tenantDomain, oauthAppName, authorizationHeader);
        DecisionResponse decisionResponse = new DecisionResponse();
        if (activeConsentReceiptForUser != null) {
            String policyUrl = activeConsentReceiptForUser.getPolicyUrl();
            List<ClaimMetaData> consentClaimsFromReceipt = getConsentClaimsFromReceipt(activeConsentReceiptForUser);
            Decision decision = new Decision();
            Obligation obligation = new Obligation();
            decision.addObligation(obligation);
            Code obligationCode = new Code();
            if (StringUtils.isNotBlank(policyUrl)) {
                if (policyUrl.equals(optInPolicy)) {
                    decision.setDecision(Constants.Policy.PERMIT);
                    obligationCode.setCode("deny");
                } else if (policyUrl.equals(optOutPolicy)) {
                    decision.setDecision(Constants.Policy.DENY);
                    obligationCode.setCode("approve");
                } else {
                    throw new FHIRConsentMgtException("provided policy: " + policyUrl + " is not supported.");
                }
            } else {
                decision.setDecision(Constants.Policy.DENY);
                obligationCode.setCode("approve");
            }
            obligation.setId(obligationCode);
            decisionResponse.addDecision(decision);

            for (ClaimMetaData claimMetaData : consentClaimsFromReceipt) {
                Attribute attribute = new Attribute();
                if (claimMetaData.getClaimUri().contains(Constants.PII.TYPE_IDENTIFIER) && claimMetaData.getClaimUri()
                        .contains(Constants.PII.VALUE_IDENTIFIER)) {
                    String type = StringUtils
                            .substringBetween(claimMetaData.getClaimUri(), Constants.PII.TYPE_IDENTIFIER,
                                    Constants.PII.VALUE_IDENTIFIER);
                    String value = claimMetaData.getClaimUri()
                            .substring(claimMetaData.getClaimUri().indexOf(Constants.VALUE) + 6);
                    if (Constants.FHIR_RESOURCE.equals(type)) {
                        attribute.setDataType(value);
                        attribute.setAttributeId(Constants.ALL_RESOURCE_FIELDS);
                        obligation.addAttribute(attribute);
                    } else if (Constants.FHIR_PATH.equals(type)) {
                        if (value.contains(".")) {
                            attribute.setDataType(value.substring(0, value.indexOf(".")));
                            attribute.setAttributeId(value.substring(value.indexOf(".") + 1));
                            obligation.addAttribute(attribute);
                        }
                    }
                } else if (claimMetaData.getClaimUri().contains(".")) {
                    attribute.setDataType(
                            claimMetaData.getClaimUri().substring(0, claimMetaData.getClaimUri().indexOf(".")));
                    attribute.setAttributeId(
                            claimMetaData.getClaimUri().substring(claimMetaData.getClaimUri().indexOf(".") + 1));
                    obligation.addAttribute(attribute);
                } else {
                    attribute.setDataType(claimMetaData.getClaimUri());
                    attribute.setAttributeId(Constants.ALL_RESOURCE_FIELDS);
                    obligation.addAttribute(attribute);
                }
            }
        }
        return decisionResponse;
    }

    private List<ClaimMetaData> getConsentClaimsFromReceipt(Receipt receipt) {
        List<org.wso2.healthcare.apim.consentmgt.mediation.model.PIICategoryValidity> piiCategoriesFromServicesTest = getPIICategoriesFromServices(
                receipt.getServices());
        return getClaimsFromPIICategoryValidity(piiCategoriesFromServicesTest);
    }

    private List<org.wso2.healthcare.apim.consentmgt.mediation.model.PIICategoryValidity> getPIICategoriesFromServices(
            List<org.wso2.healthcare.apim.consentmgt.mediation.model.ReceiptService> receiptServices) {
        List<org.wso2.healthcare.apim.consentmgt.mediation.model.PIICategoryValidity> piiCategoryValidityMap = new ArrayList<>();
        for (org.wso2.healthcare.apim.consentmgt.mediation.model.ReceiptService receiptService : receiptServices) {
            List<org.wso2.healthcare.apim.consentmgt.mediation.model.ConsentPurpose> purposes = receiptService.getPurposes();
            for (org.wso2.healthcare.apim.consentmgt.mediation.model.ConsentPurpose purpose : purposes) {
                if (!"DEFAULT".equals(purpose.getPurpose())) {
                    piiCategoryValidityMap.addAll(piiCategoryValidityMap.size(), purpose.getPiiCategory());
                }
            }
        }
        return piiCategoryValidityMap;
    }

    private List<ClaimMetaData> getClaimsFromPIICategoryValidity(
            List<org.wso2.healthcare.apim.consentmgt.mediation.model.PIICategoryValidity> piiCategories) {
        List<ClaimMetaData> claimMetaDataList = new ArrayList<>();
        for (org.wso2.healthcare.apim.consentmgt.mediation.model.PIICategoryValidity piiCategory : piiCategories) {
            if (isConsentForClaimValid(piiCategory)) {
                ClaimMetaData claimMetaData = new ClaimMetaData();
                claimMetaData.setClaimUri(piiCategory.getName());
                claimMetaData.setDisplayName(piiCategory.getDisplayName());
                claimMetaDataList.add(claimMetaData);
            }
        }
        return claimMetaDataList;
    }

    protected boolean isConsentForClaimValid(
            org.wso2.healthcare.apim.consentmgt.mediation.model.PIICategoryValidity piiCategory) {
        String validity = piiCategory.getValidity();

        if (validity == null || validity.isEmpty()) {
            return true;
        }

        String[] consentValidityEntries = validity.split(",");
        for (String consentValidityEntry : consentValidityEntries) {
            if (isSupportedExpiryType(consentValidityEntry)) {
                String[] validityEntry = consentValidityEntry.split(":", 2);
                if (validityEntry.length == 2) {
                    try {
                        String validTime = validityEntry[1];
                        if (isExpiryIndefinite(validTime)) {
                            return true;
                        }
                        long consentExpiryInMillis = Long.parseLong(validTime);
                        long currentTimeMillis = System.currentTimeMillis();
                        return isExpired(currentTimeMillis, consentExpiryInMillis);
                    } catch (NumberFormatException e) {
                        log.error("Not a valid milliseconds value.", e);
                        return false;
                    }
                }
                return false;
            }
        }
        return true;
    }

    private boolean isExpired(long currentTimeMillis, long consentExpiryInMillis) {
        return consentExpiryInMillis > currentTimeMillis;
    }

    private boolean isExpiryIndefinite(String validTime) {
        return Constants.INDEFINITE.equalsIgnoreCase(validTime);
    }

    private boolean isSupportedExpiryType(String consentValidityEntry) {
        return consentValidityEntry.toUpperCase().startsWith(Constants.VALID_UNTIL);
    }
}
