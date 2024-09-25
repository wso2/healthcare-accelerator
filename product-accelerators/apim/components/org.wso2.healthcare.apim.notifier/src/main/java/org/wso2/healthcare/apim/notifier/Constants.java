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

package org.wso2.healthcare.apim.notifier;

public class Constants {

    public static final String API_CREATE_EVENT = "API_CREATE";
    public static final String API_DELETE_EVENT = "API_DELETE";
    public static final String API_PUBLISH_EVENT = "API_PUBLISH";
    public static final String API_LIFECYCLE_CHANGE_EVENT = "API_LIFECYCLE_CHANGE";
    public static final String API_UPDATE_EVENT = "API_UPDATE";

    public static final String FHIR_API_TYPE = "fhir";
    public static final String CDS_API_TYPE = "cds-hooks";
    public static final String NONE_API_TYPE = "none";

    public static final String CDS_GLOBAL_MEDIATION = "cds_hook";
    public static final String MESSAGE_CONTEXT_API_UUID = "API_UUID";

    public static final String SWAGGER_INFO_OBJECT = "info";
    public static final String SWAGGER_API_TYPE_PARAMETER = "x-wso2-healthcare-type";
    public static final String SWAGGER_CDS_HOOKS_DEFINITIONS_PARAMETER = "x-wso2-healthcare-cds-services";

    public static final String HEALTHCARE_REGISTRY_ROOT_LOCATION = "/healthcare/";
    public static final String HEALTHCARE_REGISTRY_API_LOCATION = HEALTHCARE_REGISTRY_ROOT_LOCATION + "apiMetadata/";
    public static final String HEALTHCARE_REGISTRY_CDS_API_LOCATION = HEALTHCARE_REGISTRY_API_LOCATION + "cdsHookDefinitions/";
    public static final String HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION = HEALTHCARE_REGISTRY_API_LOCATION + "api-type-map";
    public static final String HEALTHCARE_REGISTRY_API_TYPE_MAP_DEFAULT_CONTENT = "Please see the properties section of this resources artifact to get the list of API type mapping";

    public static final String JSON_MEDIA_TYPE = "application/json";
    public static final String TEXT_MEDIA_TYPE = "text/plain";
    public static final String XML_MEDIA_TYPE = "text/xml";

    public static final String CORS_ORIGIN = "Access-Control-Allow-Origin";
    public static final String CORS_ORIGIN_VALUE = "*";

    public static final String CDS_MEDIATOR_DEFAULT_RESPONSE = "{services:[]}";
    public static final String CDS_BASE_RESOURCE_PATH = "/cds-services/";
    public static final String SERVICES = "services";
    public static final String ID = "id";
    public static final String EXTENSION = "extension";
    public static final String ORG_WSO2_HEALTHCARE_METADATA = "org.wso2.healthcare.metadata";
}
