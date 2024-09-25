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

package org.wso2.healthcare.apim.consentmgt.mediation;

import ca.uhn.fhir.context.FhirContext;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Resource;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.util.Map;

public class FHIROutboundConsentDecisionProcessor extends AbstractMediator {

    @Override
    public boolean mediate(MessageContext messageContext) {
        if (messageContext.getProperty("_WSO2_FHIR_CONSENT_DECISION") != null) {
            String decision = (String) messageContext.getProperty("_WSO2_FHIR_CONSENT_DECISION");
            //Extract Content-Type header
            String contentType = null;
            String payloadStr;
            Axis2MessageContext axis2smc = (Axis2MessageContext) messageContext;
            org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
            Object headers = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            FhirContext fhirContext = FhirContext.forR4();
            if (headers instanceof Map) {
                Map headersMap = (Map) headers;
                contentType = (String) headersMap.get("Content-Type");
            }
            if (StringUtils.isNotBlank(contentType)) {
                if ("application/fhir+xml".equalsIgnoreCase(contentType) ||
                        "application/xml".equalsIgnoreCase(contentType) ||
                        "text/xml".equalsIgnoreCase(contentType)) {
                    OMElement payload = messageContext.getEnvelope().getBody().getFirstElement();
                    if (payload != null) {
                        IBaseResource resource = fhirContext.newXmlParser().parseResource(payload.toString());
                        try {
                            Resource processedResource = DecisionProcessor
                                    .processConsentDecision(decision, (Resource) resource);
                            payloadStr = fhirContext.newXmlParser().encodeResourceToString(processedResource);
                            OMElement omXML = AXIOMUtil.stringToOM(payloadStr);
                            messageContext.getEnvelope().getBody().addChild(omXML);
                            messageContext.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE,
                                    "application/xml");
                        } catch (FHIRConsentMgtException e) {
                            handleException("Error occurred while processing consent decision.", e, messageContext);
                        } catch (XMLStreamException e) {
                            handleException("Error occurred while populating XML payload in the message context", e,
                                    messageContext);
                        }
                    }
                } else {
                    InputStream jsonStream = JsonUtil.getJsonPayload(axis2MessageCtx);
                    IBaseResource resource = fhirContext.newJsonParser().parseResource(jsonStream);
                    try {
                        Resource processedResource = DecisionProcessor
                                .processConsentDecision(decision, (Resource) resource);
                        payloadStr = fhirContext.newJsonParser().encodeResourceToString(processedResource);
                        JsonUtil.getNewJsonPayload(axis2MessageCtx, payloadStr, true, true);
                        messageContext
                                .setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, "application/json");
                    } catch (FHIRConsentMgtException e) {
                        handleException("Error occurred while processing consent decision.", e, messageContext);
                    } catch (AxisFault e) {
                        handleException("Error occurred while populating JSON payload in message context", e,
                                messageContext);
                    }
                }
            }
        }
        return true;
    }
}
