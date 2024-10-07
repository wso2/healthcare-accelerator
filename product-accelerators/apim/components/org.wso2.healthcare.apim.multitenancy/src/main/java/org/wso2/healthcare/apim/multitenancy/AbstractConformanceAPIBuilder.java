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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.StringWriter;

public abstract class AbstractConformanceAPIBuilder {

    private final Log log = LogFactory.getLog(AbstractConformanceAPIBuilder.class);
    private final String templateName;
    private final String context;

    public AbstractConformanceAPIBuilder(String templateName, String context) {

        this.templateName = templateName;
        this.context = context;
    }

    /**
     * Build the template content of the MetaAPI as a generic system API
     *
     * @param tenantDomain respective tenant
     * @param nameSuffix   type of the API as a suffix. (SmartConfig or Metadata)
     * @return Generic SystemAPI object
     */
    public SystemAPI build(String tenantDomain, String nameSuffix) {

        SystemAPI conformanceAPI = new SystemAPI();
        conformanceAPI.setTenantDomain(tenantDomain);
        conformanceAPI.setName("_" + tenantDomain + nameSuffix);

        String defaultAPIConfig = this.getMetadataStringForTemplate(tenantDomain, conformanceAPI.getName());
        conformanceAPI.setConfigDefinition(defaultAPIConfig);
        return conformanceAPI;
    }

    /**
     * Read the velocity template and fill the parameterized values accordingly
     *
     * @param tenantDomain respective tenant domain to add as the context
     * @param name         name of the artifact
     * @return completed content of the API artifact
     */
    private String getMetadataStringForTemplate(String tenantDomain, String name) {

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, CarbonUtils.getCarbonHome());

        //todo: clarify thread implementation
        velocityEngine.init();

        Template t = velocityEngine.getTemplate(this.getDefaultAPITemplatePath());

        VelocityContext context = new VelocityContext();
        context.put("tenantContext", "/t/" + tenantDomain + this.context);
        context.put("name", name);

        StringWriter writer = new StringWriter();
        t.merge(context, writer);
        if (log.isDebugEnabled()) {
            log.info("Template String: " + writer.toString());
        }
        return writer.toString();
    }

    /**
     * Get path for the velocity template file
     *
     * @return path to the template file
     */
    private String getDefaultAPITemplatePath() {

        return "repository" + File.separator + "resources" + File.separator + "api_templates" + File.separator +
                this.templateName + ".xml";
    }
}
