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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.impl.definitions.OASParserUtil;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.healthcare.apim.core.OpenHealthcareException;

/**
 * Class with util methods to handle scope management
 */
public class ScopeMgtUtil {

    public static final String CLIENT_FACING_SCOPE_DELIMITER = "/";
    public static final String INTERNAL_SCOPE_DELIMITER = "-";

    public static final String API_DEF_FHIR_RESOURCE_TYPE_FLAG = "x-wso2-oh-fhir-resourceType";

    /**
     * This method will format the scope checking whether the scope should be converted to an internal supported WSO2 scope
     *
     * @param scope                                Scope obj
     * @param isFHIRScopeToWSO2ScopeMappingEnabled the flag to change standard scopes to wso2 supported scopes
     * @return formatted Scope
     */
    public static Scope formatScope(Scope scope, boolean isFHIRScopeToWSO2ScopeMappingEnabled) {

        if (isFHIRScopeToWSO2ScopeMappingEnabled) {
            String key = scope.getKey();
            String name = scope.getName();
            scope.setKey(key.replace(CLIENT_FACING_SCOPE_DELIMITER, INTERNAL_SCOPE_DELIMITER));
            scope.setName(name.replace(CLIENT_FACING_SCOPE_DELIMITER, INTERNAL_SCOPE_DELIMITER));
        }
        return scope;
    }

    public static String getFHIRResourceTypeFromAPI(APIIdentifier apiIdentifier) throws OpenHealthcareException {

        Registry registry;
        try {
            registry = ServiceReferenceHolder.getInstance().getRegistryService().getGovernanceSystemRegistry();
        } catch (RegistryException e) {
            throw new OpenHealthcareException("Failed to get GovernanceSystemRegistry", e);
        }

        String apiDef;
        try {
            apiDef = OASParserUtil.getAPIDefinition(apiIdentifier, registry);
        } catch (APIManagementException e) {
            throw new OpenHealthcareException("Failed to get the api definition.", e);
        }

        if (apiDef != null) {
            JSONObject apiDefJSON;
            try {
                apiDefJSON = (JSONObject) new JSONParser().parse(apiDef);
            } catch (ParseException e) {
                throw new OpenHealthcareException("Failed to read the api definition.", e);
            }
            return (String) apiDefJSON.get(API_DEF_FHIR_RESOURCE_TYPE_FLAG);

        }
        return null;
    }

}
