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

package org.wso2.healthcare.apim.core.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.healthcare.apim.core.OpenHealthcareException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple client for captcha related interactions
 */
public class CaptchaClient {

    private static final Log LOG = LogFactory.getLog(CaptchaClient.class);
    private static final JSONParser parser = new JSONParser();

    private final HttpClient httpClient;
    private final String verifyAPIUrl;
    private final char[] secretKey;

    /**
     * Captcha client Constructor
     *
     * @param verifyAPIUrl verify API Url
     * @param secretKey secret key
     * @throws MalformedURLException
     */
    public CaptchaClient(String verifyAPIUrl, char[] secretKey) throws MalformedURLException {

        this.verifyAPIUrl = verifyAPIUrl;
        this.secretKey = secretKey;

        URL url = new URL(verifyAPIUrl);
        this.httpClient = APIUtil.getHttpClient(url.getPort(), url.getProtocol());
    }

    /**
     * Verify given token against Google Re-Captcha verify endpoint
     *
     * @param token token to verify
     * @return true if token successfully verified
     * @throws OpenHealthcareException
     */
    public boolean verifyToken(String token) throws OpenHealthcareException {
        HttpPost postRequest = new HttpPost(verifyAPIUrl);
        try {
            postRequest.setEntity(createVerifyPayload(token));
            HttpResponse response = httpClient.execute(postRequest);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Object msgObject = parser.parse(EntityUtils.toString(response.getEntity()));
                if (msgObject instanceof JSONObject) {
                    JSONObject responseObj = (JSONObject) msgObject;
                    Object successObj = responseObj.get("success");
                    return successObj instanceof Boolean && (Boolean) successObj;
                }
            } else {
                LOG.warn("Google ReCaptcha verification call failed " +
                        "[ Status code : " + response.getStatusLine().getStatusCode() +
                        " | response : " + EntityUtils.toString(response.getEntity()));
            }
            return false;
        } catch (UnsupportedEncodingException e) {
            throw new OpenHealthcareException("Error occurred while creating the verify request payload", e);
        } catch (IOException e) {
            throw new OpenHealthcareException("Error occurred while verifying Captcha Token", e);
        } catch (ParseException e) {
            throw new OpenHealthcareException("Error occurred while parsing response from google re-captcha", e);
        }  finally {
            postRequest.reset();
        }
    }

    /**
     * Create payload entity for verify payload
     *
     * @param token
     * @return
     * @throws UnsupportedEncodingException
     */
    private UrlEncodedFormEntity createVerifyPayload(String token) throws UnsupportedEncodingException {
        List<BasicNameValuePair> entityList = new ArrayList<>(2);
        entityList.add(new BasicNameValuePair("secret", new String(secretKey)));
        entityList.add(new BasicNameValuePair("response", token));
        return new UrlEncodedFormEntity(entityList);
    }
}
