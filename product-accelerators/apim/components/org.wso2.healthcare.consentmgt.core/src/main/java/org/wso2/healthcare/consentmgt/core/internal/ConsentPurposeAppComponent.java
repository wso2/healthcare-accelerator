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

package org.wso2.healthcare.consentmgt.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.consent.mgt.core.ConsentManager;
import org.wso2.carbon.consent.mgt.core.PrivilegedConsentManager;
import org.wso2.carbon.consent.mgt.core.constant.ConsentConstants;
import org.wso2.carbon.consent.mgt.core.exception.ConsentManagementRuntimeException;
import org.wso2.carbon.consent.mgt.core.util.ConsentConfigParser;
import org.wso2.carbon.core.ServerStartupObserver;
import org.wso2.healthcare.consentmgt.core.ConsentPurposeMapper;
import org.wso2.healthcare.consentmgt.core.ConsentPurposeMapperImpl;
import org.wso2.healthcare.consentmgt.core.FHIRServerStartupObserver;
import org.wso2.healthcare.consentmgt.core.PIICategoryMapper;
import org.wso2.healthcare.consentmgt.core.PIICategoryMapperImpl;
import org.wso2.healthcare.consentmgt.core.task.RevokeConsentsCleanupTask;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@Component(
        name = "carbon.healthcare.consent.core.app.component",
        immediate = true
)
public class ConsentPurposeAppComponent {

    private static final Log log = LogFactory.getLog(ConsentPurposeAppComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {
        ConsentConfigParser configParser = new ConsentConfigParser();
        DataSource dataSource = initDataSource(configParser);
        ConsentPurposeAppComponentDataHolder.getInstance().setDataSource(dataSource);

        BundleContext bundleContext = componentContext.getBundleContext();
        bundleContext.registerService(ConsentPurposeMapper.class.getName(), new ConsentPurposeMapperImpl(), null);
        bundleContext.registerService(PIICategoryMapper.class.getName(), new PIICategoryMapperImpl(), null);
        //registering the fhir server startup observer service
        bundleContext.registerService(ServerStartupObserver.class, new FHIRServerStartupObserver(), null);
        log.info("ConsentPurposeAppComponent is activated.");
    }

    /**
     * Setting consent core service
     * @param consentMgtService
     */
    @Reference(
            name = "consentmgt.service",
            service = org.wso2.carbon.consent.mgt.core.PrivilegedConsentManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConsentMgtService")
    protected void setConsentMgtService(PrivilegedConsentManager consentMgtService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Consent Management Service");
        }
        ConsentPurposeAppComponentDataHolder.getInstance().setConsentManager(consentMgtService);
        //initializing consent cleanup task
        RevokeConsentsCleanupTask revokeConsentsCleanupTask = new RevokeConsentsCleanupTask();
        revokeConsentsCleanupTask.execute();
    }

    /**
     * Un setting consent core service
     * @param consentMgtService
     */
    protected void unsetConsentMgtService(ConsentManager consentMgtService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Consent Management Service");
        }
        ConsentPurposeAppComponentDataHolder.getInstance().setConsentManager(null);
    }

    private DataSource initDataSource(ConsentConfigParser configParser) {

        String dataSourceName = configParser.getConsentDataSource();
        DataSource dataSource;
        Context ctx;
        try {
            ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(dataSourceName);
            return dataSource;
        } catch (NamingException e) {
            throw new ConsentManagementRuntimeException(ConsentConstants.ErrorMessages
                    .ERROR_CODE_DATABASE_INITIALIZATION.getMessage(),
                    ConsentConstants.ErrorMessages
                            .ERROR_CODE_DATABASE_INITIALIZATION.getCode(), e);
        }
    }
}