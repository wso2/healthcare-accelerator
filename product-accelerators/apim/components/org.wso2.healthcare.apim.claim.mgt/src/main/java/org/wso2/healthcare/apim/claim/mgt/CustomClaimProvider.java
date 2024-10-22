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

package org.wso2.healthcare.apim.claim.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.authz.OAuthAuthzReqMessageContext;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeRespDTO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.openidconnect.ClaimProvider;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.healthcare.apim.claim.mgt.util.ClaimMgtConstants;
import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.config.ClaimMgtConfig;
import org.wso2.carbon.apimgt.api.APIManagementException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to insert patientID claim into ID token.
 */
public class CustomClaimProvider implements ClaimProvider {

    private static final Log LOG = LogFactory.getLog(CustomClaimProvider.class);

    /**
     * when the ID Token request comes from Authorize endpoint
     *
     * @param oAuthAuthzReqMessageContext
     * @param oAuth2AuthorizeRespDTO
     * @return
     * @throws IdentityOAuth2Exception
     */
    @Override
    public Map<String, Object> getAdditionalClaims(OAuthAuthzReqMessageContext oAuthAuthzReqMessageContext,
                                                   OAuth2AuthorizeRespDTO oAuth2AuthorizeRespDTO)
            throws IdentityOAuth2Exception {

        Map<String, Object> additionalClaims = new HashMap<>();
        UserClaimIssuer claimIssuer = new UserClaimIssuer();
        String userName = String.valueOf(oAuthAuthzReqMessageContext.getAuthorizationReqDTO().getUser());
        String tenantDomain = oAuthAuthzReqMessageContext.getAuthorizationReqDTO().getTenantDomain();
        List<String> requestedScopes = Arrays.asList(oAuth2AuthorizeRespDTO.getScope());
        if (!requestedScopes.contains(ClaimMgtConstants.FHIRUSER_KEY)) {
            return additionalClaims;
        }
        return getClaimsByUser(additionalClaims, claimIssuer, userName, tenantDomain);
    }

    /**
     * when ID Token request comes from the token endpoint.
     *
     * @param oAuthTokenReqMessageContext
     * @param oAuth2AccessTokenRespDTO
     * @return
     * @throws IdentityOAuth2Exception
     */
    @Override
    public Map<String, Object> getAdditionalClaims(OAuthTokenReqMessageContext oAuthTokenReqMessageContext,
                                                   OAuth2AccessTokenRespDTO oAuth2AccessTokenRespDTO)
            throws IdentityOAuth2Exception {

        Map<String, Object> additionalClaims = new HashMap<>();
        UserClaimIssuer claimIssuer = new UserClaimIssuer();
        String userName = oAuthTokenReqMessageContext.getAuthorizedUser().getUserName();
        String tenantDomain = oAuthTokenReqMessageContext.getOauth2AccessTokenReqDTO().getTenantDomain();
        List<String> requestedScopes;
        requestedScopes = Arrays.asList(oAuth2AccessTokenRespDTO.getAuthorizedScopes().split(" "));
        if (!requestedScopes.contains(ClaimMgtConstants.FHIRUSER_KEY)) {
            return additionalClaims;
        }
        return getClaimsByUser(additionalClaims, claimIssuer, userName, tenantDomain);
    }

    /**
     * This method will extract additional claims for a given user
     * @param additionalClaims This map will get updated with matching claim
     * @param claimIssuer Userclaim issuer
     * @param userName username
     * @param tenantDomain tenant domain of the user
     * @return updated additionalClaims map
     */
    private Map<String, Object> getClaimsByUser(Map<String, Object> additionalClaims, UserClaimIssuer claimIssuer,
                                                String userName, String tenantDomain) {

        try {
            ClaimMgtConfig claimMgtConfig = OpenHealthcareEnvironment.getInstance().getConfig().getClaimMgtConfig();
            //validate config
            String claimUrl = claimMgtConfig.getClaimUri();
            String fhirUserClaimContext = claimMgtConfig.getFhirUserClaimContext();
            String fhirUserMappedLocalClaim = claimMgtConfig.getFhirUserMappedLocalClaim();
            List<String> availableClaims =
                    Arrays.asList(CarbonContext.getThreadLocalCarbonContext().getUserRealm().getClaimManager()
                            .getAllClaimUris());
            if (!availableClaims.contains(claimUrl) || !availableClaims.contains(fhirUserMappedLocalClaim)) {
                throw new OpenHealthcareException("Configured claim URL: " + claimUrl + " does not match with " +
                        "available Local claims.");
            }
            //receive the fhirUser claim
            String fhirUserId = claimIssuer.getUserClaims(userName, tenantDomain, fhirUserMappedLocalClaim);
            String fhirUserClaimUrl =
                    APIUtil.getGatewayendpoint(ClaimMgtConstants.GATEWAY_TRANSPORT, tenantDomain) + fhirUserClaimContext + "/" +
                            fhirUserId;
            //All the claims which added to the map will be inserted in the ID token
            if (fhirUserId != null) {
                additionalClaims.put(ClaimMgtConstants.FHIRUSER_KEY, fhirUserClaimUrl);
            }
        } catch (OpenHealthcareException | UserStoreException | APIManagementException e) {
            LOG.error("Error Occurred in Claim Management Handler", e);
        }
        return additionalClaims;
    }
}
