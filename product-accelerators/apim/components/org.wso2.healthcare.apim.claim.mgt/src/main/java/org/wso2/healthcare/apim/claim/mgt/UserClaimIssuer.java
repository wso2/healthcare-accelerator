/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.healthcare.apim.claim.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.healthcare.apim.claim.mgt.internal.ClaimMgtDataHolder;
import org.wso2.healthcare.apim.claim.mgt.util.ClaimMgtConstants;
import org.wso2.healthcare.apim.core.OpenHealthcareException;

/**
 * Class to retrieve claims from user store
 */
public class UserClaimIssuer {

    private static final Log LOG = LogFactory.getLog(UserClaimIssuer.class);

    /**
     * @param userName username
     * @param tenantDomain tenant domain of the user
     * @param claimMgtConfig config object from OpenHealthcareEnvironment
     * @return PatientID
     * @throws OpenHealthcareException
     */
    public String getUserClaims(String userName, String tenantDomain, String claimUrl)
            throws OpenHealthcareException {

        String patientID = null;
        RegistryService registryService;
        RealmService realmService;
        UserRealm realm;
        UserStoreManager userStore;

        Claim[] claimArray = null;
        try {
            registryService = ClaimMgtDataHolder.getInstance().getRegistryService();
            realmService = ClaimMgtDataHolder.getInstance().getRealmService();
            realm = AnonymousSessionUtil.getRealmByTenantDomain(registryService, realmService, tenantDomain);
            userStore = realm.getUserStoreManager();
            //Get all the claims from user store
            claimArray = userStore.getUserClaimValues(userName, null);
        } catch (UserStoreException e) {
            if (e.getMessage().contains(ClaimMgtConstants.MSG_USER_NOTFOUND)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("User " + userName + " not found in user store", e);
                }
            } else {
                throw new OpenHealthcareException("Error occurred during getting the user", e);
            }
        } catch (CarbonException e) {
            throw new OpenHealthcareException("Error occurred during getting realm ", e);
        }
        if (claimArray != null) {
            for (Claim claim : claimArray) {
                //check for the patientID claim
                if (claim.getClaimUri().equals(claimUrl)) {
                    patientID = claim.getValue();
                }
            }
        }
        return patientID;
    }

}
