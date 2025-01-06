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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.healthcare.apim.backendauth.impl.BackendAuthHandler;
import org.wso2.healthcare.apim.backendauth.impl.ClientCredentialsBackendAuthenticator;
import org.wso2.healthcare.apim.backendauth.impl.PrivateKeyJWTBackendAuthenticator;
import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.config.BackendAuthConfig;

import java.util.Map;

/**
 * Backend authenticator mediator.
 */
public class BackendAuthenticator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(BackendAuthenticator.class);
    private String authType;
    private String tokenEndpoint;
    private String clientId;
    private char[] clientSecret;
    private String keyAlias;
    private String configType;
    private String configValue;
    private final Map<String, BackendAuthConfig> backendAuthConfig;
    private final PrivateKeyJWTBackendAuthenticator privateKeyJWTBackendAuthenticator;
    private final ClientCredentialsBackendAuthenticator clientCredentialsBackendAuthenticator;

    public BackendAuthenticator() throws OpenHealthcareException {
        backendAuthConfig = OpenHealthcareEnvironment.getInstance().getConfig().getBackendAuthConfig();
        privateKeyJWTBackendAuthenticator = new PrivateKeyJWTBackendAuthenticator();
        clientCredentialsBackendAuthenticator = new ClientCredentialsBackendAuthenticator();

    }

    @Override
    public boolean mediate(MessageContext messageContext) {
        if (log.isDebugEnabled()) {
            log.debug("Backend authenticator mediator is invoked.");
        }

        BackendAuthConfig config;
        if ("INLINE".equals(configType)){
            if (log.isDebugEnabled()) {
                log.debug("Config value is inline. Default config is: " + configValue);
            }
            if (backendAuthConfig.containsKey(configValue)) {
                // default config is found in the backend auth configurations
                config = backendAuthConfig.get(configValue);

                // check for overridden values
                overrideConfigs(config, messageContext);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Default Config value is not found in the backend auth configurations. All configs has to be inline");
                }
                config = new BackendAuthConfig();
                //validating configs
                if (this.authType == null || this.tokenEndpoint == null || this.clientId == null) {
                    log.error("One or more required fields are missing in the message context.");
                    return false;
                } else if (Constants.POLICY_ATTR_AUTH_TYPE_PKJWT.equals(authType) && this.keyAlias == null) {
                    log.error("Key alias is missing in the message context.");
                    return false;
                } else if (Constants.POLICY_ATTR_AUTH_TYPE_CLIENT_CRED.equals(authType) && this.clientSecret == null) {
                    log.error("Client secret is missing in the message context.");
                    return false;
                }
                config.setAuthType(authType);
                config.setAuthEndpoint(tokenEndpoint);
                config.setClientSecret(clientSecret);
                config.setClientId(Utils.resolveConfigValues(clientId, messageContext));
                config.setPrivateKeyAlias(Utils.resolveConfigValues(keyAlias, messageContext));
            }
        } else if ("PREDEFINED".equals(configType)) {
            config = backendAuthConfig.get(configValue);
        } else {
            log.error("Invalid Config type is entered. Please contact the API administrator.");
            return false;
        }

        String accessToken;
        BackendAuthHandler currentAuthenticator;

        switch (config.getAuthType()) {
            case Constants.POLICY_ATTR_AUTH_TYPE_PKJWT:
                if (log.isDebugEnabled()) {
                    log.debug("Auth type is PKJWT.");
                }
                currentAuthenticator = privateKeyJWTBackendAuthenticator;
                accessToken = privateKeyJWTBackendAuthenticator.fetchValidAccessToken(messageContext, config);
                break;
            case Constants.POLICY_ATTR_AUTH_TYPE_CLIENT_CRED:
                if (log.isDebugEnabled()) {
                    log.debug("Auth type is CLIENT CREDENTIALS.");
                }
                currentAuthenticator = clientCredentialsBackendAuthenticator;
                accessToken = clientCredentialsBackendAuthenticator.fetchValidAccessToken(messageContext, config);
                break;
            default:
                log.error("Auth type is not supported.");
                return false;
        }

        if (messageContext instanceof Axis2MessageContext) {
            org.apache.axis2.context.MessageContext axisMsgCtx =
                    ((Axis2MessageContext) messageContext).getAxis2MessageContext();
            Object headers = axisMsgCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers instanceof Map) {
                Map headersMap = (Map) headers;
                if (headersMap.get(Constants.HEADER_NAME_AUTHORIZATION) != null) {
                    headersMap.remove(Constants.HEADER_NAME_AUTHORIZATION);
                }
                headersMap.put(Constants.HEADER_NAME_AUTHORIZATION, currentAuthenticator.getAuthHeaderScheme() + accessToken);
                axisMsgCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
            } else {
                log.warn("Transport headers are not available in the message context.");
            }
        } else {
            log.error("Message context is not an instance of Axis2MessageContext.");
        }
        return true;
    }

    /**
     * Override the default configurations with the values in the policy attributes or message context.
     *
     * @param config          BackendAuthConfig configuration object with default values
     * @param messageContext  MessageContext synapse message context
     */
    private void overrideConfigs(BackendAuthConfig config, MessageContext messageContext){
        if (this.authType != null) {
            config.setAuthType(authType);
        }
        if (this.tokenEndpoint != null) {
            config.setAuthEndpoint(tokenEndpoint);
        }
        if (this.clientId != null) {
            config.setClientId(Utils.resolveConfigValues(this.clientId, messageContext));
        }
        if (this.clientSecret != null) {
            config.setClientSecret(clientSecret);
        }
        if (this.keyAlias != null) {
            config.setPrivateKeyAlias(Utils.resolveConfigValues(this.keyAlias, messageContext));
        }
    }

    @SuppressWarnings("Setters will be called by the ESB config builder")
    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret.toCharArray();
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }
}
