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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.axis2.AxisFault;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.healthcare.apim.notifier.exception.OpenHealthcareNotifierExecutorException;
import org.wso2.healthcare.apim.notifier.exception.OpenHealthcareNotifierExecutorRuntimeException;
import org.wso2.healthcare.apim.notifier.utils.HealthcareNotifierRegistryUtils;
import org.wso2.healthcare.apim.notifier.utils.NotifierUtils;

import java.util.Map;

import static org.apache.axis2.Constants.Configuration.*;
import static org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS;
import static org.wso2.healthcare.apim.notifier.Constants.*;

/**
 * This mediator class will be triggerred, when a request coming to the CDS hook endpoint (/cds-services/{ID})
 * Basically, this call will retrieve the CDS hook definition by API ID and append it to the request
 * This retrieved CDS Hook definition will be appended to the extensions filed in the CDS request payload
 *
 * @see "http://cds-hooks.hl7.org/2.0/#extensions"
 */
public class CDSHooksDefinitionRetrieverClassMediator extends AbstractMediator {

    private final Log LOG = LogFactory.getLog(CDSHooksDefinitionRetrieverClassMediator.class);

    /**
     * Gson Json parser
     */
    private final JsonParser jp = new JsonParser();

    /**
     * Gson builder to make the Json object pretty
     */
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public boolean mediate(MessageContext messageContext) {

        if (log.isDebugEnabled()) {
            log.debug("CDS Hooks definition retriever mediator Started");
        }

        if (messageContext instanceof Axis2MessageContext) {

            String apiId = null;
            org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
                    .getAxis2MessageContext();
            try {
                apiId = String.valueOf(messageContext.getProperty(MESSAGE_CONTEXT_API_UUID));
                int tenantId = NotifierUtils.getTenantId();

                if (apiId == null || StringUtils.isEmpty(apiId)) {
                    String msg = "API ID is empty, can not proceed further";
                    throw new OpenHealthcareNotifierExecutorRuntimeException(new NullPointerException(msg));
                }
                JsonObject hook = getHookDefinition(apiId, tenantId, axis2MessageContext);
                if (hook != null) {
                    JsonObject payload = appendHookToRequestPayload(hook, axis2MessageContext);
                    String jsonString = gson.toJson(payload);
                    JsonUtil.getNewJsonPayload(axis2MessageContext, jsonString, true, true);
                } else {
                    throw new OpenHealthcareNotifierExecutorRuntimeException(
                            "No CDS Hook definition found in the Registry for the API Id: " + apiId + " API name: ");
                }

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

    /**
     * This method will retrieve the already stored CDS Hook definitions from Registry by API ID
     *
     * @return It will return the CDS Hooks as Gson Json object
     * @throws RegistryException Exception that occurs when using the Registry APIs
     */
    private JsonObject getHookDefinition(String apiId, int tenantId, org.apache.axis2.context.MessageContext axis2MessageContext) throws RegistryException {
        String toURL = (String) axis2MessageContext.getProperty(TRANSPORT_IN_URL);
        String hookId = toURL.split(CDS_BASE_RESOURCE_PATH)[1];
        String cdsHookDefinitionRegistryPath = HEALTHCARE_REGISTRY_CDS_API_LOCATION + apiId;

        String cdsHookDefinitions = HealthcareNotifierRegistryUtils.getResourceAsStringFromRegistry(tenantId, cdsHookDefinitionRegistryPath);

        if (LOG.isDebugEnabled()) {
            LOG.debug("CDS Hook definitions for API Id: " + apiId + " retrieved form the Registry: " + cdsHookDefinitionRegistryPath);
        }

        JsonArray definitionListJson = jp
                .parse(cdsHookDefinitions)
                .getAsJsonObject()
                .get(SERVICES)
                .getAsJsonArray();

        JsonObject hook;
        for (JsonElement je : definitionListJson) {
            hook = je.getAsJsonObject();
            if (StringUtils.equals(hook.get(ID).getAsString(), hookId)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("CDS Hook definition for hook Id: " + hook.get(ID) + " retrieved : " + hookId);
                }
                return hook;
            }
        }
        return null;
    }

    /**
     * This method will append the CDS Hooks to the request (message context) payload
     *
     * @return Payload with CDS Hooks appended
     */
    private JsonObject appendHookToRequestPayload(JsonObject hook, org.apache.axis2.context.MessageContext axis2MessageContext) {
        JsonObject payload = jp.parse(JsonUtil.jsonPayloadToString(axis2MessageContext)).getAsJsonObject();

        if (payload.has(EXTENSION)) {
            JsonObject extension = payload.get(EXTENSION).getAsJsonObject();
            extension.add(ORG_WSO2_HEALTHCARE_METADATA, hook);
            payload.add(EXTENSION, extension);
        } else {
            payload.add(EXTENSION, hook);
        }

        return payload;
    }
}
