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
package org.wso2.healthcare.apim.tokenmgt.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.AuthorizationCodeGrantHandler;
import org.wso2.healthcare.apim.claim.mgt.UserClaimIssuer;
import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.config.ClaimMgtConfig;

import java.util.Arrays;
import java.util.List;

/**
 * Custom grant handler for authorization code grant type to set the patient id in token call response according to the
 * SMART on FHIR specification -
 * http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context/index.html#requesting-context-with-scopes
 * <p>
 * Need to add the following configuration in the APIM's deployment.toml
 * <p>
 * [oauth.grant_type.authorization_code]
 * grant_handler = "org.wso2.healthcare.apim.tokenmgt.handlers.OpenHealthcareExtendedAuthorizationCodeGrantHandler"
 */
public class OpenHealthcareExtendedAuthorizationCodeGrantHandler extends AuthorizationCodeGrantHandler {

    /**
     * As per the
     * http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context/index.html#requesting-context-with-scopes
     * This scope should be added as an allowed scope in APIM's deployment.toml
     * <p>
     * [apim.oauth_config]
     * allowed_scopes = ["^device_.*", "openid", "fhirUser", "launch/patient"]
     */
    public static final String PATIENT_LAUNCH_SCOPE = "launch/patient";

    /**
     * As per the
     * http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context/index.html#patient-specific-scopes
     * This scope is granted if the patient launch context is requested.
     * FIXME: Need to do perform a user level validation before granting this scope.
     */
    public static final String PATIENT_RESOURCES_READ_SCOPE = "patient/*.read";
    private static final Log LOG = LogFactory.getLog(OpenHealthcareExtendedAuthorizationCodeGrantHandler.class);

    @Override
    public OAuth2AccessTokenRespDTO issue(OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {

        OAuth2AccessTokenRespDTO oAuth2AccessTokenRespDTO = super.issue(tokReqMsgCtx);

        List<String> requestedScopes = Arrays.asList(tokReqMsgCtx.getScope());
        if (!requestedScopes.contains(PATIENT_LAUNCH_SCOPE)) {
            // patient launch context has not been requested, hence no change to the token response
            LOG.debug("Patient launch context has not been requested, hence no change to the token response.");
            return oAuth2AccessTokenRespDTO;
        }
        String userName = tokReqMsgCtx.getAuthorizedUser().getUserName();
        String tenantDomain = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getTenantDomain();
        try {
            ClaimMgtConfig claimMgtConfig = OpenHealthcareEnvironment.getInstance().getConfig().getClaimMgtConfig();
            // gets the patient id claim uri
            String claimUri = claimMgtConfig.getClaimUri();
            // gets the patient id claim's value
            UserClaimIssuer claimIssuer = new UserClaimIssuer();
            String patientId = claimIssuer.getUserClaims(userName, tenantDomain, claimUri);
            // set patient parameter and patient id in the token response
            oAuth2AccessTokenRespDTO.addParameter("patient", patientId);
            LOG.debug("Successfully set the patient property in the token response.");
        } catch (OpenHealthcareException e) {
            LOG.warn("Unable to add patient context to the token response : Error occurred while retrieving claim " +
                    "configurations from Open Healthcare environment.", e);
        }
        return oAuth2AccessTokenRespDTO;
    }
}
