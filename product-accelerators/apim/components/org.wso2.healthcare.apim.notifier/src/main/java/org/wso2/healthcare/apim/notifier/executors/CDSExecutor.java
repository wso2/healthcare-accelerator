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

package org.wso2.healthcare.apim.notifier.executors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.FaultGatewaysException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.Mediation;
import org.wso2.carbon.apimgt.impl.notifier.events.APIEvent;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.healthcare.apim.notifier.exception.OpenHealthcareNotifierExecutorException;
import org.wso2.healthcare.apim.notifier.utils.HealthcareNotifierRegistryUtils;
import org.wso2.healthcare.apim.notifier.utils.NotifierUtils;

import static org.wso2.healthcare.apim.notifier.Constants.*;

/**
 * This class will extend the AbstractExecutor abstract class and
 * responsible to handle the CDS Hooks API related operations
 */
public class CDSExecutor extends AbstractExecutor {

    private static final Log LOG = LogFactory.getLog(CDSExecutor.class);

    public CDSExecutor(APIEvent event) throws OpenHealthcareNotifierExecutorException {
        super(event);
    }

    @Override
    public void executeCreateFlow() throws OpenHealthcareNotifierExecutorException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting the create flow of CDS executor");
        }

        API api = super.getApi();
        String apiId = super.getApiId();
        try {
            if (!NotifierUtils.checkExistenceOfAPIMapping(apiId)) {

                // Here we will retrieve the global mediation created and deployed in the OH APIM accelerator
                // You can find this mediation file under <APIM_HOME>/repository/resources/customsequences/in/cds_hooks_in.xml
                Mediation mediation = NotifierUtils.retrieveGlobalMediationObject(CDS_GLOBAL_MEDIATION,
                        api.getOrganization());

                if (mediation != null) {
                    // Set this cds mediation sequence to newly created API
                    api.setInSequence(mediation.getName());

                    storeCDSDefinitionsToRegistry();
                    NotifierUtils.addProperty(apiId, CDS_API_TYPE);

                    // Persist the updated API back to the DB
                    super.getApiProvider().updateAPI(api, super.getApi());
                } else {
                    String msg = "No such global mediation sequence found with the name " + CDS_GLOBAL_MEDIATION;
                    throw new OpenHealthcareNotifierExecutorException(new NullPointerException(msg));
                }
            }
        } catch (FaultGatewaysException | APIManagementException ex) {
            String msg = "Error occurred while updating the API object of the ID" + apiId;
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }
    }

    @Override
    public void executeUpdateFlow() throws OpenHealthcareNotifierExecutorException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting the update flow of CDS executor");
        }

        String apiId = super.getApiId();

        String newCDSHookDefinitions = NotifierUtils.retrieveContentFromSwagger(apiId,
                SWAGGER_CDS_HOOKS_DEFINITIONS_PARAMETER);
        String oldCDSHookDefinitions = getCDSDefinitionAsStringFromRegistry(apiId);

        // Here this will check whether there are any new changes has been introduced to the CDS definition
        // If yes then only it will update the definition in the Registry
        if (!StringUtils.equals(oldCDSHookDefinitions, newCDSHookDefinitions)) {
            storeCDSDefinitionsToRegistry();
            LOG.info("CDS definition has been updated for API ID " + apiId);
        }
    }

    @Override
    public void executeDeleteFlow() throws OpenHealthcareNotifierExecutorException {

        if (LOG.isDebugEnabled()) {
            LOG.info("Starting the delete flow of CDS executor");
        }
        String apiId = super.getApiId();
        NotifierUtils.removeProperty(apiId);
        removeCDSDefinitionFromRegistry(apiId);
    }

    @Override
    public void executeLifecycleChangeFlow() {
        //Nothing to do with this event flow because as per the CDS design no operations required
        // for this Life cycle change event
    }

    /*
     * This method will retrieve the CDS hook definition of a CDS API and
     * will store it the particular Registry location with API UUID as name the file
     * governance_registry/healthcare/apiMetadata/cdsHookDefinitions
     *
     */
    private void storeCDSDefinitionsToRegistry() throws OpenHealthcareNotifierExecutorException {

        String apiId = super.getApiId();

        String cdsDefinitionPath = HEALTHCARE_REGISTRY_CDS_API_LOCATION + apiId;
        String cdsHookDefinitions = NotifierUtils.retrieveContentFromSwagger(apiId,
                SWAGGER_CDS_HOOKS_DEFINITIONS_PARAMETER);

        if (StringUtils.isEmpty(cdsHookDefinitions)) {
            cdsHookDefinitions = CDS_MEDIATOR_DEFAULT_RESPONSE;
        }

        try {
            HealthcareNotifierRegistryUtils.storeResourcesInRegistry(super.getTenantId(), cdsDefinitionPath,
                    cdsHookDefinitions, JSON_MEDIA_TYPE);
        } catch (RegistryException ex) {
            throw new OpenHealthcareNotifierExecutorException(ex);
        }
        LOG.info("New CDS definition added to the registry successfully for the API ID " + apiId);
    }

    /*
     * This method will retrieve the CDS hook definition of a CDS API from the Registry location
     * governance_registry/healthcare/apiMetadata/cdsHookDefinitions
     *
     */
    private static String getCDSDefinitionAsStringFromRegistry(String apiId) throws OpenHealthcareNotifierExecutorException {
        int tenantId = NotifierUtils.getTenantId();
        try {
            return HealthcareNotifierRegistryUtils.getResourceAsStringFromRegistry(tenantId, HEALTHCARE_REGISTRY_CDS_API_LOCATION + apiId);
        } catch (RegistryException ex) {
            String msg = "Error occurred while retrieve the CDS definition for API ID " + apiId
                    + " from path " + HEALTHCARE_REGISTRY_CDS_API_LOCATION;
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }
    }

    /*
     * This method will remove the CDS hook definition of a CDS API from the Registry location
     * governance_registry/healthcare/apiMetadata/cdsHookDefinitions
     *
     */
    private static void removeCDSDefinitionFromRegistry(String apiId) throws OpenHealthcareNotifierExecutorException {
        int tenantId = NotifierUtils.getTenantId();
        try {
            HealthcareNotifierRegistryUtils.removeResourcesFromRegistry(tenantId, HEALTHCARE_REGISTRY_CDS_API_LOCATION + apiId);
            LOG.info("CDS definition removed from the registry successfully for the API ID " + apiId);
        } catch (RegistryException ex) {
            String msg = "Error occurred while removing the CDS definition for API ID " + apiId
                    + " from path " + HEALTHCARE_REGISTRY_CDS_API_LOCATION;
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }
    }
}
