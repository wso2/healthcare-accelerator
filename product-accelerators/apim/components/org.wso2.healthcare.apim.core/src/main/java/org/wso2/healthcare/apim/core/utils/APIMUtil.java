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

package org.wso2.healthcare.apim.core.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIConsumer;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.api.model.Subscriber;
import org.wso2.carbon.apimgt.api.model.Tier;
import org.wso2.carbon.apimgt.api.model.policy.ApplicationPolicy;
import org.wso2.carbon.apimgt.api.model.policy.Policy;
import org.wso2.carbon.apimgt.api.model.policy.PolicyConstants;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.token.ClaimsRetriever;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.ReferenceHolder;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;

/**
 * Utility class for APIM related utilities
 */
public class APIMUtil {

    private static final Log LOG = LogFactory.getLog(APIMUtil.class);

    /**
     * Utility function to cleanup standard default application tiers except Unlimited tier (that can we hidden by
     * [apim.throttling] configuration)
     *
     * @param tenantDomain
     * @throws OpenHealthcareException
     */
    public static void cleanupDefaultApplicationThrottlePolicies(String tenantDomain) throws OpenHealthcareException {
        try {
            APIProvider provider = getAPIProvider();
            Policy[] policies = provider.getPolicies(getCurrentUsername(), PolicyConstants.POLICY_LEVEL_APP);
            for (Policy policy : policies) {
                if (!(APIConstants.UNLIMITED_TIER.equals(policy.getPolicyName())) || APIUtil.isEnabledUnlimitedTier()) {
                    // We do not remove unlimited tier if unlimited tier is disabled in APIM since its handled by
                    // APIM level
                    LOG.info("Cleaning application policy : " + policy.getPolicyName());
                    provider.deletePolicy(getCurrentUsername(), PolicyConstants.POLICY_LEVEL_APP, policy.getPolicyName());
                }
            }
        } catch (APIManagementException e) {
            throw new OpenHealthcareException(
                    "Error occurred cleaning default standard application throttle policies of tenant: " + tenantDomain);
        }
    }

    /**
     * Utility function to add application throttle policies
     *
     * @param tenantDomain
     * @param newAppPolicy
     * @throws OpenHealthcareException
     */
    public static void addApplicationThrottlePolicy(String tenantDomain, ApplicationPolicy newAppPolicy)
            throws OpenHealthcareException {

        APIProvider provider = getAPIProvider();
        try {
            LOG.info("Adding policy : " + newAppPolicy.getPolicyName());
            provider.addPolicy(newAppPolicy);
        } catch (APIManagementException e) {
            throw new OpenHealthcareException("Error occurred while adding new application policy : " +
                    newAppPolicy.getPolicyName() + " to tenant : " + tenantDomain, e);
        }
    }

    /**
     * Create application for a given user
     *
     * @param name
     * @param username
     * @param tenantDomain
     * @throws OpenHealthcareException
     * @return
     */
    public static Application createApplication(String name, String username, String tenantDomain) throws OpenHealthcareException {
        return createApplication(name, username, "" ,tenantDomain);
    }

    /**
     * Function to retrieve API Provider
     *
     * @return
     * @throws OpenHealthcareException
     */
    public static APIProvider getAPIProvider() throws OpenHealthcareException {
        String username = getCurrentUsername();
        if (username == null) {
            throw new OpenHealthcareException("Unable to retrieve API provider for tenant : " +
                    CarbonContext.getThreadLocalCarbonContext().getTenantDomain() + " since username is not set");
        }
        try {
            return APIManagerFactory.getInstance().getAPIProvider(username);
        } catch (APIManagementException e) {
            throw new OpenHealthcareException("Error occurred while retrieving APIProvider for user : " + username);
        }
    }

    /**
     * Function to retrieve API Consumer
     *
     * @return
     * @throws OpenHealthcareException
     */
    public static APIConsumer getAPIConsumer() throws OpenHealthcareException {
        String username = getCurrentUsername();
        if (username == null) {
            throw new OpenHealthcareException("Unable to retrieve API provider for tenant : " +
                    CarbonContext.getThreadLocalCarbonContext().getTenantDomain() + " since username is not set");
        }
        try {
            return APIManagerFactory.getInstance().getAPIConsumer(username);
        } catch (APIManagementException e) {
            throw new OpenHealthcareException("Error occurred while retrieving APIProvider for user : " + username);
        }
    }

    private static String getCurrentUsername() {
        return CarbonContext.getThreadLocalCarbonContext().getUsername();
    }

    private static Application createApplication(String name, String username, String groupingId, String tenantDomain)
            throws OpenHealthcareException {
        APIConsumer consumer = getAPIConsumer();
        ApiMgtDAO apiMgtDAO = ApiMgtDAO.getInstance();
        Subscriber subscriber;

        try {
            subscriber = consumer.getSubscriber(username);
        } catch (APIManagementException e) {
            throw new OpenHealthcareException("Error occurred while retrieving subscriber for user" + username);
        }
        if (subscriber == null) {
            subscriber = new Subscriber(username);
            subscriber.setSubscribedDate(new Date());
            try {
                int tenantId = ReferenceHolder.getInstance().getRealmService().getTenantManager().getTenantId(tenantDomain);
                SortedMap<String, String> claims = APIUtil.getClaims(username, tenantId, ClaimsRetriever
                        .DEFAULT_DIALECT_URI);
                String email = claims.get(APIConstants.EMAIL_CLAIM);
                if (StringUtils.isNotEmpty(email)) {
                    subscriber.setEmail(email);
                } else {
                    subscriber.setEmail(StringUtils.EMPTY);
                }
                subscriber.setTenantId(tenantId);
                apiMgtDAO.addSubscriber(subscriber, groupingId);

            } catch (APIManagementException | UserStoreException e) {
                String msg = "Error while adding the subscriber " + subscriber.getName();
                throw new OpenHealthcareException(msg, e);
            }
        }

        try {
            // create application
            Application application = new Application(name, subscriber);

            Map<String, Tier> throttlingTiers = APIUtil.getTiers(APIConstants.TIER_APPLICATION_TYPE, tenantDomain);
            Set<Tier> tierValueList = new HashSet<>(throttlingTiers.values());
            List<Tier> sortedTierList = APIUtil.sortTiers(tierValueList);
            application.setTier(sortedTierList.get(0).getName());

            //application will not be shared within the group
            application.setGroupId("");
            application.setTokenType(APIConstants.TOKEN_TYPE_JWT);
            application.setUUID(UUID.randomUUID().toString());
            application.setDescription(APIConstants.DEFAULT_APPLICATION_DESCRIPTION);
            apiMgtDAO.addApplication(application, subscriber.getName(), null);

            // Activate application
            Application persistedApplication = apiMgtDAO.getApplicationByUUID(application.getUUID());
            apiMgtDAO.updateApplicationStatus(persistedApplication.getId(), APIConstants.ApplicationStatus.APPLICATION_APPROVED);

            return persistedApplication;
        } catch (APIManagementException e) {
            throw new OpenHealthcareException("Error occurred while adding new application : " + name + " for user : " + username);
        }
    }
}
