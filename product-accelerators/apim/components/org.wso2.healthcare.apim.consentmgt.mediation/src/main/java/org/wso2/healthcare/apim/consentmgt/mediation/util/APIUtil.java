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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.wso2.healthcare.apim.consentmgt.mediation.internal.ServiceReferenceHolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public class APIUtil {

    private static final Log log = LogFactory.getLog(APIUtil.class);

    public static HttpEntity doGet(String path, Map<String, String> parameters, Map<String, String> headers) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("https");
        uriBuilder.setHost(ServiceReferenceHolder.getInstance().getKmServerHost());
        uriBuilder.setPath(path);
        if (parameters != null) {
            for (String key : parameters.keySet()) {
                uriBuilder.addParameter(key, parameters.get(key));
            }
        }

        try {
            HttpGet request = new HttpGet(uriBuilder.build().toString());
            for (String headerName : headers.keySet()) {
                request.addHeader(headerName, headers.get(headerName));
            }
            CloseableHttpResponse response = httpClient.execute(request);
            return response.getEntity();
        } catch (URISyntaxException | IOException e) {
            log.error("Error occured while getting response.", e);
        }
        return null;
    }

    public static HttpEntity doPost(String path, byte[] payload, String contentType, Map<String, String> headers) {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("https");
        uriBuilder.setHost(ServiceReferenceHolder.getInstance().getKmServerHost());
        uriBuilder.setPath(path);
        try {
            HttpPost post = new HttpPost(uriBuilder.build().toString());
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    post.setHeader(entry.getKey(), entry.getValue());
                }
            }
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContentType(contentType);
            entity.setContent(new ByteArrayInputStream(payload));
            post.setEntity(entity);
            CloseableHttpResponse response = client.execute(post);
            return response.getEntity();
        } catch (IOException e) {
            log.error("Error occurred while post operation.", e);
        } catch (URISyntaxException e) {
            log.error("Invalid URI given.", e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                //ignore
            }
        }
        return null;
    }
}
