/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.apim.conformance;

import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handle OAS2 API definitions.
 */
public class OAS2Handler implements OASHandler {

    private static final Log log = LogFactory.getLog(OAS2Handler.class);
    private Swagger swagger;

    public OAS2Handler(String apiDefinition) {

        swagger = getSwagger(apiDefinition);
    }

    /**
     * get vendor specific extension of the API definition file.
     *
     * @return
     */
    @Override
    public Map<String, Object> getVendorExtentions() {

        return swagger.getVendorExtensions();
    }

    /**
     * set OAS2,OAS3 definition file.
     *
     * @param apiDefinition
     */
    @Override
    public void setAPIDefinition(String apiDefinition) {

        swagger = getSwagger(apiDefinition);
    }

    /**
     * Get title of API definition.
     *
     * @return
     */
    @Override
    public String getTitle() {

        return swagger.getInfo().getTitle();
    }

    /**
     * Get searchparameter names.
     *
     * @return
     */
    @Override
    public List<String> getSearchParameters() {

        List<String> searchParamNames = new ArrayList<>();
        if (swagger.getPaths().get("/") != null && swagger.getPaths().get("/").getGet() != null) {
            for (Parameter parameter : swagger.getPaths().get("/").getGet().getParameters()) {
                searchParamNames.add(parameter.getName());
            }
        }
        return searchParamNames;
    }

    /**
     * Extracted from org.wso2.carbon.apimgt.impl.definitions.OAS2Parser in the base product.
     *
     * @param oasDefinition
     * @return
     */
    Swagger getSwagger(String oasDefinition) {
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult parseAttemptForV2 = parser.readWithInfo(oasDefinition);
        if (CollectionUtils.isNotEmpty(parseAttemptForV2.getMessages())) {
            log.error("Errors found when parsing OAS definition");
            for (String message : parseAttemptForV2.getMessages()) {
                log.error(message);
            }
        }
        return parseAttemptForV2.getSwagger();
    }
}
