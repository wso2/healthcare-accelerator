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

package org.wso2.healthcare.apim.core;

import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.securevault.SecretCallbackHandlerService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Service reference holder
 */
public class ReferenceHolder {

    private static final ReferenceHolder INSTANCE = new ReferenceHolder();

    private RealmService realmService;
    private ConfigurationContext mainServerConfigContext;
    private SecretCallbackHandlerService secretCallbackHandlerService;
    private ClaimMetadataManagementService claimMetadataManagementService;
    private APIManagerConfigurationService apiManagerConfigurationService;
    private IdpManager idpManager;

    public static ReferenceHolder getInstance() {
        return INSTANCE;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    public ConfigurationContext getMainServerConfigContext() {

        return mainServerConfigContext;
    }

    public void setMainServerConfigContext(ConfigurationContext mainServerConfigContext) {

        this.mainServerConfigContext = mainServerConfigContext;
    }

    public SecretCallbackHandlerService getSecretCallbackHandlerService() {

        return secretCallbackHandlerService;
    }

    public void setSecretCallbackHandlerService(SecretCallbackHandlerService secretCallbackHandlerService) {

        this.secretCallbackHandlerService = secretCallbackHandlerService;
    }

    public APIManagerConfigurationService getApiManagerConfigurationService() {

        return apiManagerConfigurationService;
    }

    public void setApiManagerConfigurationService(APIManagerConfigurationService apiManagerConfigurationService) {

        this.apiManagerConfigurationService = apiManagerConfigurationService;
    }

    public ClaimMetadataManagementService getClaimMetadataManagementService() {

        return claimMetadataManagementService;
    }

    public void setClaimMetadataManagementService(ClaimMetadataManagementService claimMetadataManagementService) {

        this.claimMetadataManagementService = claimMetadataManagementService;
    }

    public IdpManager getIdpManager() {

        return idpManager;
    }

    public void setIdpManager(IdpManager idpManager) {

        this.idpManager = idpManager;
    }
}
