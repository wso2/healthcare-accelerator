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

package org.wso2.healthcare.apim.notifier.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.Mediation;
import org.wso2.carbon.apimgt.impl.GlobalMediationPolicyImpl;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.rest.api.common.RestApiCommonUtil;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.healthcare.apim.notifier.exception.OpenHealthcareNotifierExecutorException;

import java.util.List;

import static org.wso2.healthcare.apim.notifier.Constants.*;

/**
 * This class contains utility methods to do which are particular to CDS related operations
 */
public class NotifierUtils {
    private static final Log LOG = LogFactory.getLog(NotifierUtils.class);
    private static final JsonParser parser = new JsonParser();

    /**
     * This method will extract the API type defined in the API swagger definition
     *
     * @param apiId API UUID
     * @return String Custom API type defined in the x-wso2-healthcare-type swagger parameter
     * @throws OpenHealthcareNotifierExecutorException Base Class for capturing any type of exception that occurs in the Healthcare notifier component.
     */
    public static String retrieveAPITypeFromAPIDefinition(String apiId) throws OpenHealthcareNotifierExecutorException {
        JsonObject swaggerDefinitionJson = retrieveSwaggerJsonFromAPIDefinition(apiId);

        if (swaggerDefinitionJson.has(SWAGGER_INFO_OBJECT)
                && ((JsonObject) swaggerDefinitionJson.get(SWAGGER_INFO_OBJECT)).has(SWAGGER_API_TYPE_PARAMETER)) {
            JsonObject infoJson = (JsonObject) swaggerDefinitionJson.get(SWAGGER_INFO_OBJECT);
            return infoJson.get(SWAGGER_API_TYPE_PARAMETER).getAsString();
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("There is no any API type meta data exist in the API definition file for API Id: " + apiId);
            }
        }

        return NONE_API_TYPE;
    }

    /**
     * This method will return the API swagger definition of an API as GSON Json object
     *
     * @param apiId API UUID
     * @return @linkplain com.google.gson.JSONObject Swagger definition of the API for the provided API ID
     * @throws OpenHealthcareNotifierExecutorException Base Class for capturing any type of exception that occurs in the Healthcare notifier component.
     */
    public static JsonObject retrieveSwaggerJsonFromAPIDefinition(String apiId) throws OpenHealthcareNotifierExecutorException {
        String swaggerDefinition = retrieveSwaggerStringFromAPIDefinition(apiId);
        return (JsonObject) parser.parse(swaggerDefinition);
    }

    /**
     * This method will retrieve the API swagger definition of an API as string
     *
     * @param apiId API UUID
     * @return String Swagger definition of the API for the provided API ID
     * @throws OpenHealthcareNotifierExecutorException Base Class for capturing any type of exception that occurs in the Healthcare notifier component.
     */
    public static String retrieveSwaggerStringFromAPIDefinition(String apiId) throws OpenHealthcareNotifierExecutorException {
        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            API api = retrieveAPI(apiId);
            return RestApiCommonUtil.retrieveSwaggerDefinition(apiId, api, apiProvider);
        } catch (APIManagementException ex) {
            String msg = "Error occurred while retrieving the Swagger definition for API ID " + apiId;
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }
    }

    /**
     * This method will retrieve the API object
     *
     * @param apiId API UUID
     * @return @linkplain org.wso2.carbon.apimgt.api.model.API API object of the provided API ID
     * @throws OpenHealthcareNotifierExecutorException Base Class for capturing any type of exception that occurs in the Healthcare notifier component.
     */
    public static API retrieveAPI(String apiId) throws OpenHealthcareNotifierExecutorException {
        try {
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            return apiProvider.getAPIbyUUID(apiId, tenantDomain);
        } catch (APIManagementException ex) {
            String msg = "Error occurred while retrieving the API object for ID " + apiId;
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }
    }

    /**
     * This method will retrieve the API swagger definition of an API as string
     *
     * @param mediationName Name of global mediation sequence to fetched
     * @return @link org.wso2.carbon.apimgt.api.model.Mediation Global mediation sequence object of the provided mediation name
     * @throws OpenHealthcareNotifierExecutorException Base Class for capturing any type of exception that occurs in the Healthcare notifier component.
     */
    public static Mediation retrieveGlobalMediationObject(String mediationName, String organization) throws OpenHealthcareNotifierExecutorException {
        try {
            GlobalMediationPolicyImpl mediationPolicyImpl = new GlobalMediationPolicyImpl(organization);
            List<Mediation> mediationList = mediationPolicyImpl.getAllGlobalMediationPolicies();
            return mediationList.stream()
                    .filter(entry -> StringUtils.equals(entry.getName(), mediationName))
                    .findAny()
                    .orElse(null);
        } catch (APIManagementException ex) {
            String msg = "Error occurred while retrieving global mediation policies";
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }

    }

    /**
     * This method will extract the content of specific parameter in the swagger definition of an API
     *
     * @param apiId            API UUID
     * @param swaggerParameter Name of swagger parameter to fetched from the Swagger definition
     * @return String Value of swagger parameter for the provided name in the Swagger definition
     * @throws OpenHealthcareNotifierExecutorException Base Class for capturing any type of exception that occurs in the Healthcare notifier component.
     */
    public static String retrieveContentFromSwagger(String apiId, String swaggerParameter) throws OpenHealthcareNotifierExecutorException {
        JsonObject swaggerDefinitionJson = NotifierUtils.retrieveSwaggerJsonFromAPIDefinition(apiId);
        if (swaggerDefinitionJson.has(swaggerParameter)) {
            return swaggerDefinitionJson.get(swaggerParameter).toString();
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("There is no any data for the provided parameter: " + swaggerParameter + " for the API Id: " + apiId);
            }
        }
        return null;
    }

    /**
     * This method will add API_UUID and API type (fhir or cds_hooks) as property to apis directory in the healthcare registry
     *
     * @param apiId API UUID
     * @param value Value of the property to be added for the resource
     * @throws OpenHealthcareNotifierExecutorException Base Class for capturing any type of exception that occurs in the Healthcare notifier component.
     */
    public static void addProperty(String apiId, String value) throws OpenHealthcareNotifierExecutorException {
        try {
            int tenantId = getTenantId();
            if (!HealthcareNotifierRegistryUtils.isResourceExist(tenantId, HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION)) {
                HealthcareNotifierRegistryUtils.storeResourcesInRegistry(tenantId, HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION,
                        HEALTHCARE_REGISTRY_API_TYPE_MAP_DEFAULT_CONTENT, TEXT_MEDIA_TYPE);
            }
            HealthcareNotifierRegistryUtils.addPropertyToResource(tenantId, HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION,
                    apiId, value);
            LOG.info("API type mapping added successfully for the API ID " + apiId + " type " + value);
        } catch (RegistryException ex) {
            String msg = "Error occurred while adding a property to the resource in the path " + HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION;
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }
    }

    /**
     * This method will retrieve properties added to apis directory in the healthcare registry
     *
     * @param apiId API UUID
     * @return String API type property stored for the provided API ID in the registry
     * @throws OpenHealthcareNotifierExecutorException Base Class for capturing any type of exception that occurs in the Healthcare notifier component.
     */
    public static String retrieveProperty(String apiId) throws OpenHealthcareNotifierExecutorException {
        try {
            int tenantId = getTenantId();
            return HealthcareNotifierRegistryUtils.retrievePropertyOfResource(tenantId, HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION, apiId);
        } catch (RegistryException ex) {
            String msg = "Error occurred while retrieve a property of the resource in the path " + HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION;
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }
    }

    /**
     * This method will remove properties added to apis directory in the healthcare registry
     *
     * @param apiId API UUID
     * @throws OpenHealthcareNotifierExecutorException Base Class for capturing any type of exception that occurs in the Healthcare notifier component.
     */
    public static void removeProperty(String apiId) throws OpenHealthcareNotifierExecutorException {
        try {
            int tenantId = getTenantId();
            HealthcareNotifierRegistryUtils.removePropertyFromResource(tenantId, HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION, apiId);
            LOG.info("API type mapping removed successfully for the API ID " + apiId);
        } catch (RegistryException ex) {
            String msg = "Error occurred while remove a property of the resource in the path " + HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION;
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }
    }

    /**
     * This method will check the existence of a property added to apis directory in the healthcare registry
     *
     * @param apiId API UUID
     * @return boolean True, If an API type property found for the provided registry path and the API ID
     * @throws OpenHealthcareNotifierExecutorException Base Class for capturing any type of exception that occurs in the Healthcare notifier component.
     */
    public static boolean checkExistenceOfAPIMapping(String apiId) throws OpenHealthcareNotifierExecutorException {
        int tenantId = getTenantId();
        try {
            return HealthcareNotifierRegistryUtils.isPropertyExistInResource(tenantId, HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION, apiId);
        } catch (RegistryException ex) {
            String msg = "Error occurred while checking the existence of a property of the resource in the path "
                    + HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION;
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }
    }

    public static int getTenantId() throws OpenHealthcareNotifierExecutorException {
        try {
            String requestedTenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            return ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getTenantId(requestedTenantDomain);
        } catch (UserStoreException ex) {
            String msg = "Error occurred while retrieve the logged in user tenant domain";
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }
    }
}
