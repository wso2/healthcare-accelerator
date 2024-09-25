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

package org.wso2.healthcare.apim.notifier.mediators;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.axis2.AxisFault;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.healthcare.apim.notifier.exception.OpenHealthcareNotifierExecutorException;
import org.wso2.healthcare.apim.notifier.utils.NotifierUtils;

import java.util.Map;

import static org.apache.axis2.Constants.Configuration.CONTENT_TYPE;
import static org.apache.axis2.Constants.Configuration.MESSAGE_TYPE;
import static org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS;
import static org.wso2.healthcare.apim.notifier.Constants.*;

/**
 * This class only executed by the CDS APIs only
 * It will be triggered by the mediation sequence resides under <APIM_HOME>/repository/resources/customsequences/in/cds_hooks_in.xml
 * When request coming to the /cds-services resource of a CDS API then this class will be triggered
 */
public class CDSDiscoveryEndpointClassMediator extends AbstractMediator {

    JsonParser jp = new JsonParser();
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Log LOG = LogFactory.getLog(CDSDiscoveryEndpointClassMediator.class);

    @Override
    public boolean mediate(MessageContext messageContext) {

        if (log.isDebugEnabled()) {
            log.debug("CDS Hooks mediator Started");
        }

        if (messageContext instanceof Axis2MessageContext) {

            org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
                    .getAxis2MessageContext();
            String apiId = null;
            try {
                apiId = String.valueOf(messageContext.getProperty(MESSAGE_CONTEXT_API_UUID));
                int tenantId = NotifierUtils.getTenantId();

                if (apiId == null || StringUtils.isEmpty(apiId)) {
                    String msg = "API ID is empty, can not proceed further";
                    OpenHealthcareNotifierExecutorException ex = new OpenHealthcareNotifierExecutorException(
                            new NullPointerException(msg));
                    LOG.error(msg, ex);
                    return true;
                }

                RegistryService registryService = ServiceReferenceHolder.getInstance().getRegistryService();
                UserRegistry registry = registryService.getGovernanceSystemRegistry(tenantId);
                Resource resource = registry.get(HEALTHCARE_REGISTRY_CDS_API_LOCATION + apiId);

                JsonElement je;
                if (resource.getContent() == null) {
                    je = jp.parse(CDS_MEDIATOR_DEFAULT_RESPONSE);
                } else {
                    String data = RegistryUtils.decodeBytes((byte[]) resource.getContent());
                    je = jp.parse(data);
                }

                String jsonString = gson.toJson(je);
                JsonUtil.getNewJsonPayload(axis2MessageContext, jsonString, true, true);

                axis2MessageContext.setProperty(MESSAGE_TYPE, JSON_MEDIA_TYPE);
                axis2MessageContext.setProperty(CONTENT_TYPE, JSON_MEDIA_TYPE);

                Map headers = (Map) axis2MessageContext.getProperty(TRANSPORT_HEADERS);
                if (headers != null) {
                    headers.put(CORS_ORIGIN, CORS_ORIGIN_VALUE);
                    axis2MessageContext.setProperty(TRANSPORT_HEADERS, headers);
                }

            } catch (RegistryException e) {
                String msg = "Error occurred while accessing the registry";
                handleException(msg, new OpenHealthcareNotifierExecutorException(msg, e), messageContext);
            } catch (AxisFault e) {
                String msg = "Error occurred while setting the payload to the message context";
                handleException(msg, new OpenHealthcareNotifierExecutorException(msg, e), messageContext);
            } catch (OpenHealthcareNotifierExecutorException e) {
                String msg = "Error occurred while retrieving the CDS hooks definitions for API ID " + apiId;
                handleException(msg, e, messageContext);
            }
        }

        return true;
    }
}
