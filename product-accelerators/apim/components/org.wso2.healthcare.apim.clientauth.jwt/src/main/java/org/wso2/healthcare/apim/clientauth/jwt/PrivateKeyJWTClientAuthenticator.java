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

package org.wso2.healthcare.apim.clientauth.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.common.OAuth2ErrorCodes;
import org.wso2.carbon.identity.oauth2.bean.OAuthClientAuthnContext;
import org.wso2.carbon.identity.oauth2.client.authentication.AbstractOAuthClientAuthenticator;
import org.wso2.carbon.identity.oauth2.client.authentication.OAuthClientAuthnException;
import org.wso2.healthcare.apim.clientauth.jwt.validator.JWTValidator;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.wso2.healthcare.apim.clientauth.jwt.Constants.*;
import static org.wso2.healthcare.apim.clientauth.jwt.Constants.ErrorMessages.NO_VALID_ASSERTION_ERROR;

/**
 * Client Authentication handler to implement SMART backend authorization
 * https://hl7.org/fhir/uv/bulkdata/authorization/index.html#protocol-details
 */
public class PrivateKeyJWTClientAuthenticator extends AbstractOAuthClientAuthenticator {

    private static final Log log = LogFactory.getLog(PrivateKeyJWTClientAuthenticator.class);
    private JWTValidator jwtValidator;

    public PrivateKeyJWTClientAuthenticator() {

        String tokenEPAlias = DEFAULT_AUDIENCE;
        if (isNotEmpty(properties.getProperty(TOKEN_ENDPOINT_ALIAS))) {
            tokenEPAlias = properties.getProperty(TOKEN_ENDPOINT_ALIAS);
        }
        int jwtExp = DEFAULT_JWT_EXP;
        if (isNotEmpty(properties.getProperty(ALLOWED_JWT_EXPIRY))) {
            jwtExp = Integer.parseInt(properties.getProperty(ALLOWED_JWT_EXPIRY));
        }
        jwtValidator = createJWTValidator(tokenEPAlias, jwtExp);
    }

    /**
     * To check whether the authentication is successful.
     *
     * @param httpServletRequest      http servelet request
     * @param bodyParameters          map of request body params
     * @param oAuthClientAuthnContext oAuthClientAuthnContext
     * @return true if the authentication is successful.
     * @throws OAuthClientAuthnException
     */
    @Override
    public boolean authenticateClient(HttpServletRequest httpServletRequest, Map<String, List> bodyParameters,
                                      OAuthClientAuthnContext oAuthClientAuthnContext) throws OAuthClientAuthnException {

        return jwtValidator.isValidAssertion(getSignedJWT(bodyParameters, oAuthClientAuthnContext));
    }

    /**
     * Returns whether the incoming request can be handled by the particular authenticator.
     *
     * @param httpServletRequest      http servelet request
     * @param bodyParameters          map of request body params
     * @param oAuthClientAuthnContext oAuthClientAuthnContext
     * @return true if the incoming request can be handled.
     */
    @Override
    public boolean canAuthenticate(HttpServletRequest httpServletRequest, Map<String, List> bodyParameters,
                                   OAuthClientAuthnContext oAuthClientAuthnContext) {

        Map<String, String> bodyParametersMap = getBodyParameters(bodyParameters);
        String oauthJWTAssertionType = bodyParametersMap.get(OAUTH_JWT_ASSERTION_TYPE);
        String oauthJWTAssertion = bodyParametersMap.get(OAUTH_JWT_ASSERTION);
        String scope = bodyParametersMap.get(SCOPE);
        return isValidJWTClientAssertionRequest(oauthJWTAssertionType, oauthJWTAssertion, scope);
    }

    /**
     * Retrievs the client ID which is extracted from the JWT.
     *
     * @param httpServletRequest
     * @param bodyParameters
     * @param oAuthClientAuthnContext
     * @return jwt 'sub' value as the client id
     * @throws OAuthClientAuthnException
     */
    @Override
    public String getClientId(HttpServletRequest httpServletRequest, Map<String, List> bodyParameters,
                              OAuthClientAuthnContext oAuthClientAuthnContext) throws OAuthClientAuthnException {

        SignedJWT signedJWT = getSignedJWT(bodyParameters, oAuthClientAuthnContext);
        JWTClaimsSet claimsSet = jwtValidator.getClaimSet(signedJWT);
        return jwtValidator.resolveSubject(claimsSet);
    }

    private SignedJWT getSignedJWT(Map<String, List> bodyParameters, OAuthClientAuthnContext oAuthClientAuthnContext)
            throws OAuthClientAuthnException {

        Object signedJWTFromContext = oAuthClientAuthnContext.getParameter(PRIVATE_KEY_JWT);
        if (signedJWTFromContext != null) {
            return (SignedJWT) signedJWTFromContext;
        }
        String assertion = getBodyParameters(bodyParameters).get(OAUTH_JWT_ASSERTION);
        SignedJWT signedJWT;
        if (isEmpty(assertion)) {
            throw new OAuthClientAuthnException(NO_VALID_ASSERTION_ERROR, OAuth2ErrorCodes.INVALID_REQUEST);
        }
        try {
            signedJWT = SignedJWT.parse(assertion);
        } catch (ParseException e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage());
            }
            throw new OAuthClientAuthnException("Error while parsing the JWT.", OAuth2ErrorCodes.INVALID_REQUEST);
        }
        oAuthClientAuthnContext.addParameter(PRIVATE_KEY_JWT, signedJWT);
        return signedJWT;
    }

    private boolean isValidJWTClientAssertionRequest(String clientAssertionType, String clientAssertion, String scope) {

        if (log.isDebugEnabled()) {
            log.debug("Authenticate Requested with clientAssertionType : " + clientAssertionType);
            if (IdentityUtil.isTokenLoggable(IdentityConstants.IdentityTokens.ACCESS_TOKEN)) {
                log.debug("Authenticate Requested with clientAssertion : " + clientAssertion);
            }
        }
        return OAUTH_JWT_BEARER_GRANT_TYPE.equals(clientAssertionType) && isNotEmpty(clientAssertion) &&
                isNotEmpty(scope);
    }

    private JWTValidator createJWTValidator(String tokenEPAlias, int jwtExpiry) {

        return new JWTValidator(tokenEPAlias, populateMandatoryClaims(), DEFAULT_ENABLE_JTI_CACHE, jwtExpiry);
    }

    private List<String> populateMandatoryClaims() {

        List<String> mandatoryClaims = new ArrayList<>();
        mandatoryClaims.add(ISSUER_CLAIM);
        mandatoryClaims.add(SUBJECT_CLAIM);
        mandatoryClaims.add(AUDIENCE_CLAIM);
        mandatoryClaims.add(EXPIRATION_TIME_CLAIM);
        mandatoryClaims.add(JWT_ID_CLAIM);
        return mandatoryClaims;
    }
}
