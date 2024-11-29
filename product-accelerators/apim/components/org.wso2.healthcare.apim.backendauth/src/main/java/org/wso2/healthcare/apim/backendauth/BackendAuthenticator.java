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
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.OpenHealthcareRuntimeException;
import org.wso2.healthcare.apim.core.config.BackendAuthConfig;

/**
 * Backend authenticator mediator.
 */
public class BackendAuthenticator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(BackendAuthenticator.class);
    private String authType;
    protected final BackendAuthConfig backendAuthConfig;

    public BackendAuthenticator() throws OpenHealthcareException {
        backendAuthConfig = OpenHealthcareEnvironment.getInstance().getConfig().getBackendAuthConfig();
    }
    @Override
    public boolean mediate(MessageContext messageContext) {
        if (log.isDebugEnabled()) {
            log.debug("Backend authenticator mediator is invoked.");
        }

        if (authType == null) {
            log.error("Auth type is not defined in the message context.");
            return false;
        }
        if (authType.equals(Constants.POLICY_ATTR_AUTH_TYPE_PKJWT)) {
            if (log.isDebugEnabled()) {
                log.debug("Auth type is PKJWT.");
            }
            PrivateKeyJWTBackendAuthenticator privateKeyJWTBackendAuthenticator;
            try {
                privateKeyJWTBackendAuthenticator = new PrivateKeyJWTBackendAuthenticator();
            } catch (OpenHealthcareException e) {
                log.error("Error occurred while initializing the private key JWT backend authenticator.", e);
                throw new OpenHealthcareRuntimeException(e);
            }
            privateKeyJWTBackendAuthenticator.mediate(messageContext);
            return true;
        } else if (authType.equals(Constants.POLICY_ATTR_AUTH_TYPE_CLIENT_CRED)) {
            if (log.isDebugEnabled()) {
                log.debug("Auth type is CLIENT CREDENTIALS.");
            }
            return true;
        } else {
            log.error("Auth type is not supported.");
            return false;
        }
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }
}
