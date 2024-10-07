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

package org.wso2.healthcare.apim.multitenancy;

/**
 * This class will create a tenant specific api artifact for the metadata endpoint
 */
public class MetadataAPIBuilder extends AbstractConformanceAPIBuilder {

    public static final String TEMPLATE_DEFAULT_API = "metadata_api_template";

    public MetadataAPIBuilder() {

        super(TEMPLATE_DEFAULT_API, "/metadata");
    }

    /**
     * Build the template content of the MetaAPI as a generic system API
     *
     * @param tenantDomain respective tenant
     * @return Generic SystemAPI object
     */
    public SystemAPI build(String tenantDomain) {

        return super.build(tenantDomain, "-MetadataAPI_");
    }
}
