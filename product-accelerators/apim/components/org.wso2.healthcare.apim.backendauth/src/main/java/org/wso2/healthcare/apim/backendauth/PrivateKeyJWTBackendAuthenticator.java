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

package org.wso2.healthcare.apim.backendauth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.healthcare.apim.backendauth.tokenmgt.Token;
import org.wso2.healthcare.apim.backendauth.tokenmgt.TokenManager;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.OpenHealthcareRuntimeException;
import org.wso2.healthcare.apim.core.config.BackendAuthConfig;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Mediator to authenticate with the backend using private key JWT.
 */
public class PrivateKeyJWTBackendAuthenticator extends BackendAuthenticator {

    private static final Log log = LogFactory.getLog(PrivateKeyJWTBackendAuthenticator.class);
    private static final JsonParser parser = new JsonParser();

    public PrivateKeyJWTBackendAuthenticator() throws OpenHealthcareException {
    }

    public boolean mediate(MessageContext messageContext) {

        log.info("PrivateKeyJWTBackendAuthenticator mediator is started.");

        String accessToken;
        String tokenEndpoint = backendAuthConfig.getAuthEndpoint();
        String keyAlias = backendAuthConfig.getPrivateKeyAlias();
        char[] keyStorePass = CarbonUtils.getServerConfiguration().getFirstProperty(
                "Security.KeyStore.Password").toCharArray();
        char[] trustStorePass = CarbonUtils.getServerConfiguration().getFirstProperty(
                "Security.TrustStore.Password").toCharArray();
        String clientId = backendAuthConfig.getClientId();
        int tenantId = Integer.parseInt(messageContext.getProperty(Constants.TENANT_INFO_ID).toString());
        KeyStoreManager keyStoreManager = KeyStoreManager.getInstance(tenantId);
        Key privateKey;
        Certificate publicCert;

        if (log.isDebugEnabled()) {
            log.debug("Configured client ID: " + clientId);
        }

        try {
            KeyStore primarykeyStore = keyStoreManager.getPrimaryKeyStore();
            privateKey = primarykeyStore.getKey(keyAlias, keyStorePass);
            publicCert = keyStoreManager.getDefaultPrimaryCertificate();
        } catch (Exception e) {
            log.error("Error occurred while retrieving private key from keystore.", e);
            throw new OpenHealthcareRuntimeException(e);
        }

        Token token = TokenManager.getToken(clientId, tokenEndpoint);
        if (token == null || !token.isActive()) {
            if (log.isDebugEnabled()) {
                log.debug("Access token not available in TokenManager.");
            }
            try {
                token = getAndAddNewToken(clientId, tokenEndpoint, messageContext, privateKey, publicCert, trustStorePass);
            } catch (OpenHealthcareException e) {
                log.error("Error occurred while retrieving access token.",e);
                throw new OpenHealthcareRuntimeException(e);
            }
        }
        accessToken = token.getAccessToken();

        messageContext.setProperty(Constants.PROPERTY_ACCESS_TOKEN, accessToken);

        if (messageContext instanceof Axis2MessageContext) {
            org.apache.axis2.context.MessageContext axisMsgCtx =
                    ((Axis2MessageContext) messageContext).getAxis2MessageContext();
            Object headers = axisMsgCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers instanceof Map) {
                Map headersMap = (Map) headers;
                if (headersMap.get(Constants.HEADER_NAME_AUTHORIZATION) != null) {
                    headersMap.remove(Constants.HEADER_NAME_AUTHORIZATION);
                }
                headersMap.put(Constants.HEADER_NAME_AUTHORIZATION, Constants.HEADER_VALUE_BEARER + accessToken);
                axisMsgCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
            }else {
                log.warn("Transport headers are not available in the message context.");
            }
        }else {
            log.error("Message context is not an instance of Axis2MessageContext.");
        }
        return true;
    }

    /**
     * Retrieve token from the token store. If token is not available or inactive, generate new token.
     * @param clientId - to retrieve token
     * @param tokenEndpoint - to retrieve token
     * @param messageContext - to set error response
     * @param privateKey - to sign the JWT
     * @return Token
     * @throws OpenHealthcareException - when error occurred while retrieving token
     */
    private synchronized Token getAndAddNewToken(String clientId, String tokenEndpoint, MessageContext messageContext, Key privateKey, Certificate publicCert, char[] trustStorePass) throws OpenHealthcareException {

        Token token = TokenManager.getToken(clientId, tokenEndpoint);
        if (token == null || !token.isActive()) {
            String jwt = generateJWT(clientId, tokenEndpoint, privateKey, publicCert);
            token = getAccessToken(messageContext, tokenEndpoint, jwt, backendAuthConfig.isSSLEnabled(), trustStorePass);
            TokenManager.addToken(clientId, tokenEndpoint, token);
        }else {
            if (log.isDebugEnabled()) {
                log.debug("Token exists in the token store.");
            }
        }
        return token;
    }

    /**
     * Generate JWT token with signature.
     * @param clientId - to add as subject
     * @param tokenEndpoint - audience
     * @param privateKey - to sign the JWT
     * @return JWT as string
     * @throws OpenHealthcareException JOSEException
     */
    private String generateJWT(String clientId, String tokenEndpoint, Key privateKey, Certificate publicCert) throws OpenHealthcareException {

        try {
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;
            JWSSigner signer = new RSASSASigner(rsaPrivateKey);
            long curTimeInMillis = System.currentTimeMillis();

            JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
            claimsSetBuilder.issuer(clientId);
            claimsSetBuilder.subject(clientId);
            claimsSetBuilder.audience(tokenEndpoint);
            claimsSetBuilder.jwtID(UUID.randomUUID().toString());
            claimsSetBuilder.issueTime((new Date(curTimeInMillis)));
            claimsSetBuilder.notBeforeTime((new Date(curTimeInMillis)));
            claimsSetBuilder.expirationTime(new Date(curTimeInMillis + 300000)); // Maximum expiration time is 5 min.
            JWTClaimsSet claimsSet = claimsSetBuilder.build();

            JWSAlgorithm signatureAlgorithm = new JWSAlgorithm(JWSAlgorithm.RS256.getName());
            JWSHeader.Builder headerBuilder = new JWSHeader.Builder(signatureAlgorithm);
            headerBuilder.type(JOSEObjectType.JWT);
            headerBuilder.keyID(Utils.getKID((X509Certificate) publicCert));
            JWSHeader jwsHeader = headerBuilder.build();

            SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
            signedJWT.sign(signer);
            return signedJWT.serialize();

        } catch (JOSEException e) {
            String message = "Error occurred while signing the JWT.";
            log.error(message, e);
            throw new OpenHealthcareException(message, e);
        }
    }

    /**
     * Retrieve access token from the token endpoint. This makes external HTTP call to the token endpoint.
     * @param messageContext - to set error response
     * @param tokenEndpoint - to retrieve access token
     * @param jwt - signed JWT including claims
     * @param isSSLEnabled - whether SSL is enabled
     * @return Token
     * @throws OpenHealthcareException - when error occurred while retrieving access token
     */
    private static Token getAccessToken(
            MessageContext messageContext,
            String tokenEndpoint,
            String jwt,
            boolean isSSLEnabled,
            char[] trustStorePass)
            throws OpenHealthcareException {

        long curTimeInMillis = System.currentTimeMillis();
        String trustStorePath = System.getProperty("javax.net.ssl.trustStore");
        HttpPost postRequest = new HttpPost(tokenEndpoint);
        ArrayList<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("grant_type", "client_credentials"));
        parameters.add(new BasicNameValuePair("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"));
        parameters.add(new BasicNameValuePair("client_assertion", jwt));

        try {
            postRequest.setEntity(new UrlEncodedFormEntity(parameters));
        } catch (UnsupportedEncodingException e) {
            String message = "Error occurred while preparing access token request payload.";
            log.error(message);
            throw new OpenHealthcareException(message, e);
        }
        CloseableHttpClient httpsClient;

        if (isSSLEnabled) {
            httpsClient = Utils.getHttpsClient(trustStorePath, trustStorePass);
        } else {
            httpsClient = HttpClients.createDefault();
        }
        CloseableHttpResponse response;
        try {
            response = httpsClient.execute(postRequest);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity == null) {
                String message = "Failed to retrieve access token : No entity received.";
                log.error(message);
                throw new OpenHealthcareException(message);
            }
            int responseStatus = response.getStatusLine().getStatusCode();
            String respMessage;
            respMessage = EntityUtils.toString(responseEntity);

            if (responseStatus == HttpURLConnection.HTTP_OK) {
                return extractToken(respMessage, curTimeInMillis);
            } else {
                handleTokenRequestError(messageContext, responseStatus, respMessage);
            }
        } catch (IOException e) {
            throw new OpenHealthcareRuntimeException(e);
        }
        throw new OpenHealthcareException("Error occurred while retrieving access token.");
    }

    /**
     * Handle error response from the token endpoint.
     * @param messageContext - to set error response
     * @param responseStatus - HTTP status code
     * @param respMessage - response message
     * @throws OpenHealthcareException - to propagate the error
     */
    private static void handleTokenRequestError(MessageContext messageContext, int responseStatus, String respMessage) throws OpenHealthcareException {
        String message = "Error occurred while retrieving access token. Response: [Status : " + responseStatus + " Message: " + respMessage + "]";
        log.error(message);
        OpenHealthcareException exp = new OpenHealthcareException(message);
        Utils.setErrorResponse(messageContext, exp, responseStatus, message);
        throw exp;
    }

    /**
     * Extract token from the response message.
     * @param respMessage - response message
     * @param curTimeInMillis - current time in milliseconds
     * @return - Token
     */
    private static Token extractToken(String respMessage, long curTimeInMillis) {
        JsonElement jsonElement = parser.parse(respMessage);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String accessToken = jsonObject.get("access_token").getAsString();
        long expireIn = jsonObject.get("expires_in").getAsLong();

        Token token = new Token(accessToken, curTimeInMillis, expireIn * 1000);
        if (log.isDebugEnabled()) {
            log.debug(token);
        }
        return token;
    }

    public String getType() {
        return null;
    }

    public void setTraceState(int traceState) {
        traceState = 0;
    }

    public int getTraceState() {
        return 0;
    }
}
