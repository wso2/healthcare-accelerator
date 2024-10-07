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

package org.wso2.healthcare.apim.consentmgt.mediation.util;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.wso2.healthcare.apim.consentmgt.mediation.FHIRConsentMgtException;
import org.wso2.healthcare.apim.consentmgt.mediation.internal.ServiceReferenceHolder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConsentMgtUtils {

    private static final Log log = LogFactory.getLog(ConsentMgtUtils.class);

    public static org.wso2.healthcare.apim.consentmgt.mediation.model.Receipt getActiveConsentReceiptForUser(String user,
            String tenantDomain, String application, String authorization) throws FHIRConsentMgtException {

        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("https");
        uriBuilder.setHost(ServiceReferenceHolder.getInstance().getKmServerHost());
        uriBuilder.setPath("/api/identity/consent-mgt/v1.0/consents");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("piiPrincipalId", user);
        parameters.put("spTenantDomain", tenantDomain);
        parameters.put("service", application);
        parameters.put("state", "ACTIVE");

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.ACCEPT, "application/json");
        headers.put(HttpHeaders.AUTHORIZATION, authorization);
        HttpEntity consentsResponse = APIUtil.doGet("/api/identity/consent-mgt/v1.0/consents", parameters, headers);
        Gson gson = new Gson();
        if (consentsResponse != null) {
            try {
                String responseStr = EntityUtils.toString(consentsResponse);
                org.wso2.healthcare.apim.consentmgt.mediation.model.ReceiptListResponse[] receiptListResponses = gson
                        .fromJson(responseStr, org.wso2.healthcare.apim.consentmgt.mediation.model.ReceiptListResponse[].class);
                for (org.wso2.healthcare.apim.consentmgt.mediation.model.ReceiptListResponse receiptListResponse : receiptListResponses) {
                    String consentReceiptId = receiptListResponse.getConsentReceiptId();
                    HttpEntity consentResponse = APIUtil
                            .doGet("/api/identity/consent-mgt/v1.0/consents/receipts/" + consentReceiptId, null,
                                    headers);
                    if (consentResponse != null) {
                        return gson.fromJson(EntityUtils.toString(consentResponse),
                                org.wso2.healthcare.apim.consentmgt.mediation.model.Receipt.class);
                    }
                }
            } catch (IOException e) {
                throw new FHIRConsentMgtException("Error occurred while getting consent receipts.", e);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.info("No consent receipts found for the user.");
            }
        }
        return null;
    }
}
