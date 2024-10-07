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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.securevault.SecretCallbackHandlerService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.healthcare.apim.core.config.OpenHealthcareConfig;

@Component(
        name = "org.wso2.healthcare.apim.core.OpenHealthcareComponent",
        immediate = true)
public class OpenHealthcareComponent {

    private static final Log LOG = LogFactory.getLog(OpenHealthcareComponent.class);

    private static SecretCallbackHandlerService secretCallbackHandlerService;

    protected void activate(ComponentContext componentContext) throws Exception {
        LOG.info("Activating Open-Healthcare component ...");
        try {
            initializeHealthcareComponent();
        } catch (OpenHealthcareException e) {
            LOG.fatal("Error occurred during initializing Open-Healthcare-APIM component", e);
            throw new OpenHealthcareException("Error occurred while activating Open-Healthcare component", e);
        } catch (Throwable throwable) {
            // Since exceptions are not logged by upstream code we are logging here to help troubleshooting
            LOG.error("Error occurred while activating OpenHealthcareComponent", throwable);
            throw throwable;
        }
        LOG.info("Open-Healthcare component successfully activated...");
    }

    @Reference(
            name = "org.wso2.carbon.user.core.service.RealmService",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {
        LOG.debug("Acquired RealmService to Open-Healthcare environment");
        ReferenceHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        LOG.debug("Release RealmService from Open-Healthcare environment");
        ReferenceHolder.getInstance().setRealmService(null);
    }


    @Reference(
            name = "org.wso2.carbon.utils.ConfigurationContextService",
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigurationContextService")
    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        LOG.debug("Acquired ConfigurationContextService to Open-Healthcare environment");
        ReferenceHolder.getInstance().setMainServerConfigContext(contextService.getServerConfigContext());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        LOG.debug("Release ConfigurationContextService from Open-Healthcare environment");
        ReferenceHolder.getInstance().setMainServerConfigContext(null);
    }

    @Reference(
            name = "org.wso2.carbon.securevault.SecretCallbackHandlerService",
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetSecretCallbackHandlerService")
    protected void setSecretCallbackHandlerService(SecretCallbackHandlerService secretCallbackHandlerService) {
        LOG.debug("Acquired SecretCallbackHandlerService to Open-Healthcare environment");
        ReferenceHolder.getInstance().setSecretCallbackHandlerService(secretCallbackHandlerService);
    }

    protected void unsetSecretCallbackHandlerService(SecretCallbackHandlerService secretCallbackHandlerService) {
        LOG.debug("Release SecretCallbackHandlerService from Open-Healthcare environment");
        ReferenceHolder.getInstance().setSecretCallbackHandlerService(null);
    }


    @Reference(
            name = "org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService",
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetClaimMetadataManagementService")
    protected void setClaimMetadataManagementService(ClaimMetadataManagementService claimMetadataManagementService) {
        LOG.debug("Acquired ClaimMetadataManagementService to Open-Healthcare environment");
        ReferenceHolder.getInstance().setClaimMetadataManagementService(claimMetadataManagementService);
    }

    protected void unsetClaimMetadataManagementService(ClaimMetadataManagementService claimMetadataManagementService) {
        LOG.debug("Release ClaimMetadataManagementService from Open-Healthcare environment");
        ReferenceHolder.getInstance().setClaimMetadataManagementService(null);
    }

    @Reference(name = "org.wso2.carbon.apimgt.impl.APIManagerConfigurationService",
            service = org.wso2.carbon.apimgt.impl.APIManagerConfigurationService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAPIManagerConfigurationService")
    protected void setAPIManagerConfigurationService(APIManagerConfigurationService amcService) {
        // This is mainly added to sync activation of this bundle after APIManagerConfigurationService is registered
        LOG.debug("Acquired APIManagerConfigurationService to Open-Healthcare environment");
        ReferenceHolder.getInstance().setApiManagerConfigurationService(amcService);
    }

    protected void unsetAPIManagerConfigurationService(APIManagerConfigurationService amcService) {
        LOG.debug("Release APIManagerConfigurationService from Open-Healthcare environment");
        ReferenceHolder.getInstance().setApiManagerConfigurationService(null);
    }


    @Reference(
            name = "org.wso2.carbon.idp.mgt.IdentityProviderManager",
            service = org.wso2.carbon.idp.mgt.IdpManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdpManager")
    protected void setIdpManager(IdpManager idpManager) {
        LOG.debug("Acquired IdpManager to Open-Healthcare environment");
        ReferenceHolder.getInstance().setIdpManager(idpManager);
    }

    protected void unsetIdpManager(IdpManager idpManager) {
        LOG.debug("Release IdpManager from Open-Healthcare environment");
        ReferenceHolder.getInstance().setIdpManager(null);
    }

    private void initializeHealthcareComponent() throws OpenHealthcareException {
        OpenHealthcareConfig openHealthcareConfig = OpenHealthcareConfig.build();
        // Initialize environment
        OpenHealthcareEnvironment.getInstance().initialize(openHealthcareConfig);
    }
}
