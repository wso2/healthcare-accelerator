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

package org.wso2.healthcare.apim.claim.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.openidconnect.ClaimProvider;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.healthcare.apim.claim.mgt.CustomClaimProvider;

/**
 * This class is used for publishing UserClaimIssuer services
 */
@Component(
        name = "org.wso2.carbon.healthcare.claims.internal.ClaimMgtServiceComponent",
        immediate = true)
public class ClaimMgtServiceComponent {

    private static final Log LOG = LogFactory.getLog(ClaimMgtServiceComponent.class);

    /**
     * @return
     */
    public static RealmService getRealmService() {

        return ClaimMgtDataHolder.getInstance().getRealmService();
    }

    /**
     * @param realmService
     */
    @Reference(
            name = "realm.service",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting the Realm Service");
        }
        ClaimMgtDataHolder.getInstance().setRealmService(realmService);
    }

    /**
     * @return
     */
    public static RegistryService getRegistryService() {

        return ClaimMgtDataHolder.getInstance().getRegistryService();
    }

    /**
     * @param registryService
     */
    @Reference(
            name = "registry.service",
            service = org.wso2.carbon.registry.core.service.RegistryService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRegistryService")
    protected void setRegistryService(RegistryService registryService) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting the Registry Service");
        }
        ClaimMgtDataHolder.getInstance().setRegistryService(registryService);
    }

    /**
     * @param context
     */
    @Activate
    protected void activate(ComponentContext context) {

        LOG.info("Claim management service component activate success");
        try {
            CustomClaimProvider customClaimProvider = new CustomClaimProvider();
            context.getBundleContext().registerService(ClaimProvider.class.getName(), customClaimProvider, null);
        } catch (Throwable e) {
            LOG.error("Error while activating custom claim provider service component", e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Custom claim provider service component activate success");
        }
    }

    /**
     * @param context
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {

        LOG.debug("Custom claim provider service component is de-activated");

    }

    /**
     * @param registryService
     */
    protected void unsetRegistryService(RegistryService registryService) {

        ClaimMgtDataHolder.getInstance().setRegistryService(null);
        LOG.debug("UnSetting the Registry Service");
    }

    /**
     * @param realmService
     */
    protected void unsetRealmService(RealmService realmService) {

        ClaimMgtDataHolder.getInstance().setRealmService(null);
        LOG.debug("UnSetting the Realm Service");
    }
}
