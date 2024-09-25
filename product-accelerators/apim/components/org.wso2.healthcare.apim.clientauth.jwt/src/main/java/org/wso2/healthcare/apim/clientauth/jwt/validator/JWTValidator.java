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

package org.wso2.healthcare.apim.clientauth.jwt.validator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.common.OAuth2ErrorCodes;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDO;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.client.authentication.OAuthClientAuthnException;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.identity.oauth2.validators.jwt.JWKSBasedJWTValidator;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;
import org.wso2.healthcare.apim.clientauth.jwt.Constants;
import org.wso2.healthcare.apim.clientauth.jwt.cache.JWTCache;
import org.wso2.healthcare.apim.clientauth.jwt.cache.JWTCacheEntry;
import org.wso2.healthcare.apim.clientauth.jwt.dao.JWTEntry;
import org.wso2.healthcare.apim.clientauth.jwt.dao.JWTStorageManager;
import org.wso2.healthcare.apim.clientauth.jwt.internal.JWTServiceComponent;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * This class is used to validate the JWT which is coming along with the request.
 */
public class JWTValidator {

    private static final Log log = LogFactory.getLog(JWTValidator.class);

    private final String validAudience;
    List<String> mandatoryClaims;
    private final JWTCache jwtCache;
    private final boolean enableJTICache;
    private final JWTStorageManager jwtStorageManager;
    private final long jwtExpiry;

    public JWTValidator(String validAudience, List<String> mandatoryClaims, boolean enableJTICache, int jwtExpiry) {

        this.validAudience = validAudience;
        this.jwtExpiry = (long) jwtExpiry * Constants.TO_MILLIS;
        this.jwtStorageManager = new JWTStorageManager();
        this.mandatoryClaims = mandatoryClaims;
        this.enableJTICache = enableJTICache;
        this.jwtCache = JWTCache.getInstance();
    }

    /**
     * To validate the JWT assertion.
     *
     * @param signedJWT Validate the token
     * @return true if the jwt is valid.
     * @throws IdentityOAuth2Exception
     */
    public boolean isValidAssertion(SignedJWT signedJWT) throws OAuthClientAuthnException {


        if (signedJWT == null) {
            return logAndThrowException(Constants.ErrorMessages.NO_VALID_ASSERTION_ERROR);
        }
        try {
            JWTClaimsSet claimsSet = getClaimSet(signedJWT);

            if (claimsSet == null) {
                throw new OAuthClientAuthnException(Constants.ErrorMessages.EMPTY_CLAIMS_ERROR, OAuth2ErrorCodes.INVALID_REQUEST);
            }

            if (!validateHeaders(signedJWT)) {
                return false;
            }

            String jwtIssuer = claimsSet.getIssuer();
            String jwtSubject = resolveSubject(claimsSet);
            List<String> audience = claimsSet.getAudience();
            Date expirationTime = claimsSet.getExpirationTime();
            String jti = claimsSet.getJWTID();
            Date issuedAtTime = claimsSet.getIssueTime();
            long currentTimeInMillis = System.currentTimeMillis();
            long timeStampSkewMillis = OAuthServerConfiguration.getInstance().getTimeStampSkewInSeconds() * 1000;
            OAuthAppDO oAuthAppDO = getOAuthAppDO(jwtSubject);
            String consumerKey = oAuthAppDO.getOauthConsumerKey();
            String tenantDomain = oAuthAppDO.getUser().getTenantDomain();
            if (!validateMandatoryFields(mandatoryClaims, claimsSet)) {
                return false;
            }

            //Validate issuer and subject.
            if (!validateIssuer(jwtIssuer, consumerKey) || !validateSubject(jwtSubject, consumerKey)) {
                return false;
            }

            // Get audience.
            String validAud = getValidAudience(tenantDomain);
            long expTime = 0;
            if (expirationTime != null) {
                expTime = expirationTime.getTime();
            }

            //Validate signature validation, audience, exp time, jti.
            if (!validateJTI(signedJWT, jti, currentTimeInMillis, timeStampSkewMillis, expTime, jwtIssuer) ||
                    !validateAudience(validAud, audience) ||
                    !validateJWTWithExpTime(expirationTime, currentTimeInMillis, timeStampSkewMillis) ||
                    !validateAgeOfTheToken(issuedAtTime, expirationTime, currentTimeInMillis, timeStampSkewMillis)
                    || !isValidSignature(consumerKey, signedJWT, tenantDomain, jwtSubject)) {
                return false;
            }

            return true;

        } catch (IdentityOAuth2Exception e) {
            return logAndThrowException(e.getMessage());
        }
    }

    private boolean validateMandatoryFields(List<String> mandatoryClaims, JWTClaimsSet claimsSet) throws OAuthClientAuthnException {

        for (String mandatoryClaim : mandatoryClaims) {
            if (claimsSet.getClaim(mandatoryClaim) == null) {
                String errorMessage = "Mandatory field :" + mandatoryClaim + " is missing in the JWT assertion.";
                return logAndThrowException(errorMessage);
            }
        }
        return true;
    }

    // "REQUIRED. sub. This MUST contain the client_id of the OAuth Client."
    public boolean validateSubject(String jwtSubject, String consumerKey) throws OAuthClientAuthnException {

        if (!jwtSubject.trim().equals(consumerKey)) {
            if (log.isDebugEnabled()) {
                String errorMessage = String.format("Invalid Subject '%s' is found in the JWT. It should be equal to the '%s'",
                        jwtSubject, consumerKey);
                log.debug(errorMessage);
            }
            throw new OAuthClientAuthnException("Invalid Subject: " + jwtSubject + " is found in the JWT", OAuth2ErrorCodes.
                    INVALID_REQUEST);
        }
        return true;
    }

    // "REQUIRED. iss. This MUST contain the client_id of the OAuth Client."
    private boolean validateIssuer(String issuer, String consumerKey) throws OAuthClientAuthnException {

        String error = String.format("Invalid issuer '%s' is found in the JWT. ", issuer);
        if (!issuer.trim().equals(consumerKey)) {
            if (log.isDebugEnabled()) {
                String errorMessage = String.format("Invalid issuer '%s' is found in the JWT. It should be equal to the '%s'"
                        , issuer, consumerKey);
                log.debug(errorMessage);
            }
            throw new OAuthClientAuthnException(error, OAuth2ErrorCodes.INVALID_REQUEST);
        }
        return true;
    }

    // "The Audience SHOULD be the URL of the Authorization Server's Token Endpoint", if a valid audience is not
    // specified.
    private boolean validateAudience(String expectedAudience, List<String> audience) throws OAuthClientAuthnException {

        for (String aud : audience) {
            if (StringUtils.equals(expectedAudience, aud)) {
                return true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("None of the audience values matched the tokenEndpoint Alias :" + expectedAudience);
        }
        throw new OAuthClientAuthnException(Constants.ErrorMessages.AUDIENCE_MISMATCH_ERROR, OAuth2ErrorCodes.INVALID_REQUEST);
    }

    // "REQUIRED. JWT ID. A unique identifier for the token, which can be used to prevent reuse of the token. A JTI
    // cannot be reused for the given issuer until the older JWT with the same JTI expires."
    private boolean validateJTI(SignedJWT signedJWT, String jti, long currentTimeInMillis,
                                long timeStampSkewMillis, long expTime, String issuer) throws OAuthClientAuthnException {

        if (enableJTICache) {
            JWTCacheEntry entry = jwtCache.getValueFromCache(jti + ":" + issuer);
            if (!validateJTIInCache(jti, issuer, signedJWT, entry, currentTimeInMillis, timeStampSkewMillis,
                    this.jwtCache)) {
                return false;
            }
        }
        // Check JWT ID in DB
        JWTEntry jwtEntry = jwtStorageManager.getJwtFromDB(jti, issuer);
        if (!validateJWTInDataBase(jti, jwtEntry, currentTimeInMillis, timeStampSkewMillis)) {
            return false;
        }
        persistJWTID(jti, issuer, expTime, jwtEntry);
        return true;
    }

    private boolean validateJWTInDataBase(String jti, JWTEntry jwtEntry, long currentTimeInMillis,
                                          long timeStampSkewMillis) throws OAuthClientAuthnException {

        if (jwtEntry == null) {
            if (log.isDebugEnabled()) {
                log.debug("JWT id: " + jti + " not found in the Storage the JWT has been validated successfully.");
            }
            return true;
        } else {
            if (!checkJTIValidityPeriod(jti, jwtEntry.getExp(), currentTimeInMillis, timeStampSkewMillis)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkJTIValidityPeriod(String jti, long jwtExpiryTimeMillis, long currentTimeInMillis,
                                           long timeStampSkewMillis) throws OAuthClientAuthnException {

        if (currentTimeInMillis + timeStampSkewMillis > jwtExpiryTimeMillis) {
            if (log.isDebugEnabled()) {
                log.debug("JWT Token with jti: " + jti + "has been reused after the allowed expiry time: " +
                        getUTCDate(jwtExpiryTimeMillis));
            }
            return true;
        } else {
            String message = "JWT Token with jti: " + jti + " has been replayed before the allowed expiry time: "
                    + getUTCDate(jwtExpiryTimeMillis);
            return logAndThrowException(message);
        }
    }

    private void persistJWTID(final String jti, String issuer, long expiryTime, JWTEntry jwtEntry) throws OAuthClientAuthnException {

        if (jwtEntry != null) {
            jwtStorageManager.updateExp(jti, issuer, expiryTime);
        } else {
            jwtStorageManager.persistJWTIdInDB(jti, issuer, expiryTime);
        }
    }

    private OAuthAppDO getOAuthAppDO(String jwtSubject) throws OAuthClientAuthnException {

        OAuthAppDO oAuthAppDO = null;
        String message = String.format("Error while retrieving OAuth application with provided JWT information with " +
                "subject '%s' ", jwtSubject);
        try {
            oAuthAppDO = OAuth2Util.getAppInformationByClientId(jwtSubject);
            if (oAuthAppDO == null) {
                logAndThrowException(message);
            }
        } catch (InvalidOAuthClientException | IdentityOAuth2Exception e) {
            logAndThrowException(message);
        }
        return oAuthAppDO;
    }

    private boolean logAndThrowException(String detailedMessage) throws OAuthClientAuthnException {

        if (log.isDebugEnabled()) {
            log.debug(detailedMessage);
        }
        throw new OAuthClientAuthnException(detailedMessage, OAuth2ErrorCodes.INVALID_REQUEST);
    }

    private boolean validateJWTWithExpTime(Date expTime, long currentTimeInMillis, long timeStampSkewMillis)
            throws OAuthClientAuthnException {

        long expirationTime = expTime.getTime();
        if (currentTimeInMillis + timeStampSkewMillis > expirationTime) {
            String errorMessage = "JWT Token is expired. Expired Time: " + expTime;
            if (log.isDebugEnabled()) {
                log.debug(errorMessage);
            }
            throw new OAuthClientAuthnException(errorMessage, OAuth2ErrorCodes.INVALID_REQUEST);
        } else {
            return true;
        }
    }

    private boolean isValidSignature(String clientId, SignedJWT signedJWT, String tenantDomain,
                                     String alias) throws OAuthClientAuthnException {

        X509Certificate cert = null;
        String jwksUri = "";
        boolean isValidSignature = false;
        try {
            cert = (X509Certificate) OAuth2Util.getX509CertOfOAuthApp(clientId, tenantDomain);
        } catch (IdentityOAuth2Exception e) {
            if (log.isDebugEnabled()) {
                log.debug(Constants.ErrorMessages.RETRIEVING_CERT_ERROR, e);
            }
        }
        // If cert is null check whether a jwks endpoint is configured for the service provider.
        if (cert == null) {
            try {
                ServiceProviderProperty[] spProperties = OAuth2Util.getServiceProvider(clientId).getSpProperties();
                for (ServiceProviderProperty spProperty : spProperties) {
                    if (Constants.JWKS_URI.equals(spProperty.getName())) {
                        jwksUri = spProperty.getValue();
                        break;
                    }
                }
                // Validate the signature of the assertion using the jwks end point.
                if (StringUtils.isNotBlank(jwksUri)) {
                    if (log.isDebugEnabled()) {
                        String message = "Found jwks end point for service provider " + jwksUri;
                        log.debug(message);
                    }
                    String jwtString = signedJWT.getParsedString();
                    String alg = signedJWT.getHeader().getAlgorithm().getName();
                    Map<String, Object> options = new HashMap<String, Object>();
                    isValidSignature = new JWKSBasedJWTValidator().validateSignature(jwtString, jwksUri, alg, options);
                }
            } catch (IdentityOAuth2Exception e) {
                log.error(Constants.ErrorMessages.VALIDATION_WITH_JWKS_ERROR, e);
                return false;
            }
        }
        // If certificate is not configured in service provider, it will throw an error.
        // For the existing clients need to handle that error and get from truststore.
        if (StringUtils.isBlank(jwksUri) && cert == null) {
            cert = getCertificate(tenantDomain, alias);
        }
        if (StringUtils.isBlank(jwksUri) && cert != null) {
            try {
                isValidSignature = validateSignature(signedJWT, cert);
            } catch (JOSEException e) {
                throw new OAuthClientAuthnException(Constants.ErrorMessages.VALIDATING_SIGNATURE_ERROR, OAuth2ErrorCodes.INVALID_REQUEST, e);
            }
        }
        return isValidSignature;
    }

    private String getValidAudience(String tenantDomain) throws OAuthClientAuthnException {

        if (isNotEmpty(validAudience)) {
            return validAudience;
        }
        String audience = null;
        IdentityProvider residentIdP;
        try {
            residentIdP = IdentityProviderManager.getInstance()
                    .getResidentIdP(tenantDomain);
            FederatedAuthenticatorConfig oidcFedAuthn = IdentityApplicationManagementUtil
                    .getFederatedAuthenticator(residentIdP.getFederatedAuthenticatorConfigs(),
                            IdentityApplicationConstants.Authenticator.OIDC.NAME);
            Property idpEntityId = IdentityApplicationManagementUtil.getProperty(oidcFedAuthn.getProperties(),
                    Constants.IDP_ENTITY_ID);
            if (idpEntityId != null) {
                audience = idpEntityId.getValue();
            }
        } catch (IdentityProviderManagementException e) {
            String message = "Error while loading OAuth2TokenEPUrl of the resident IDP of tenant: " + tenantDomain;
            if (log.isDebugEnabled()) {
                log.debug(message);
            }
            throw new OAuthClientAuthnException(message, OAuth2ErrorCodes.INVALID_REQUEST);
        }

        if (isEmpty(audience)) {
            audience = IdentityUtil.getProperty(Constants.PROP_ID_TOKEN_ISSUER_ID);
        }
        return audience;
    }

    /**
     * To retreive the processed JWT claimset.
     *
     * @param signedJWT signedJWT
     * @return JWT claim set
     * @throws IdentityOAuth2Exception
     */
    public JWTClaimsSet getClaimSet(SignedJWT signedJWT) throws OAuthClientAuthnException {

        JWTClaimsSet claimsSet;
        if (signedJWT == null) {
            throw new OAuthClientAuthnException(Constants.ErrorMessages.NO_VALID_ASSERTION_ERROR, OAuth2ErrorCodes.INVALID_REQUEST);
        }
        try {
            claimsSet = signedJWT.getJWTClaimsSet();
            if (claimsSet == null) {
                throw new OAuthClientAuthnException(Constants.ErrorMessages.EMPTY_CLAIMS_ERROR, OAuth2ErrorCodes.INVALID_REQUEST);
            }
        } catch (ParseException e) {
            if (log.isDebugEnabled()) {
                log.debug(Constants.ErrorMessages.RETRIEVING_CLAIMS_SET_ERROR);
            }
            throw new OAuthClientAuthnException(Constants.ErrorMessages.RETRIEVING_CLAIMS_SET_ERROR, OAuth2ErrorCodes.INVALID_REQUEST);
        }
        return claimsSet;
    }

    /**
     * The default implementation which creates the subject from the 'sub' attribute.
     *
     * @param claimsSet all the JWT claims
     * @return The subject, to be used
     */
    public String resolveSubject(JWTClaimsSet claimsSet) {

        return claimsSet.getSubject();
    }

    private static X509Certificate getCertificate(String tenantDomain, String alias) throws OAuthClientAuthnException {

        int tenantId;
        try {
            tenantId = JWTServiceComponent.getRealmService().getTenantManager().getTenantId(tenantDomain);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String errorMsg = "Error getting the tenant ID for the tenant domain : " + tenantDomain;
            throw new OAuthClientAuthnException(errorMsg, OAuth2ErrorCodes.INVALID_REQUEST);
        }

        KeyStoreManager keyStoreManager;
        // get an instance of the corresponding Key Store Manager instance
        keyStoreManager = KeyStoreManager.getInstance(tenantId);
        KeyStore keyStore;
        try {
            if (tenantId != MultitenantConstants.SUPER_TENANT_ID) {// for tenants, load key from their generated key store
                keyStore = keyStoreManager.getKeyStore(generateKSNameFromDomainName(tenantDomain));
            } else {
                // for super tenant, load the default pub. cert using the config. in carbon.xml
                keyStore = keyStoreManager.getPrimaryKeyStore();
            }
            return (X509Certificate) keyStore.getCertificate(alias);

        } catch (KeyStoreException e) {
            String errorMsg = "Error instantiating an X509Certificate object for the certificate alias: " + alias +
                    " in tenant:" + tenantDomain;
            if (log.isDebugEnabled()) {
                log.debug(errorMsg);
            }
            throw new OAuthClientAuthnException(errorMsg, OAuth2ErrorCodes.INVALID_REQUEST);
        } catch (Exception e) {
            String message = "Unable to load key store manager for the tenant domain: " + tenantDomain;
            //keyStoreManager throws Exception
            if (log.isDebugEnabled()) {
                log.debug(message);
            }
            throw new OAuthClientAuthnException(message, OAuth2ErrorCodes.INVALID_REQUEST);
        }
    }

    private static String generateKSNameFromDomainName(String tenantDomain) {

        String ksName = tenantDomain.trim().replace(Constants.FULLSTOP_DELIMITER, Constants.DASH_DELIMITER);
        return ksName + Constants.KEYSTORE_FILE_EXTENSION;
    }

    private boolean validateSignature(SignedJWT signedJWT, X509Certificate x509Certificate)
            throws JOSEException, OAuthClientAuthnException {

        JWSVerifier verifier;
        JWSHeader header = signedJWT.getHeader();
        if (x509Certificate == null) {
            throw new OAuthClientAuthnException("Unable to locate certificate for JWT " + header.toString(),
                    OAuth2ErrorCodes.INVALID_REQUEST);
        }

        String alg = signedJWT.getHeader().getAlgorithm().getName();
        if (isEmpty(alg)) {
            throw new OAuthClientAuthnException(Constants.ErrorMessages.NO_ALGORITHM_FOUND_ERROR,
                    OAuth2ErrorCodes.INVALID_REQUEST);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Signature Algorithm found in the JWT Header: " + alg);
            }
            if (alg.indexOf(Constants.RS) == 0 || alg.indexOf(Constants.PS) == 0) {
                // At this point 'x509Certificate' will never be null.
                PublicKey publicKey = x509Certificate.getPublicKey();
                if (publicKey instanceof RSAPublicKey) {
                    verifier = new RSASSAVerifier((RSAPublicKey) publicKey);
                } else {
                    throw new OAuthClientAuthnException(Constants.ErrorMessages.KEY_NOT_RSA_ERROR,
                            OAuth2ErrorCodes.INVALID_REQUEST);
                }
            } else {
                throw new OAuthClientAuthnException("Signature Algorithm not supported : " + alg,
                        OAuth2ErrorCodes.INVALID_REQUEST);
            }
        }
        // At this point 'verifier' will never be null.
        return signedJWT.verify(verifier);
    }

    private boolean validateAgeOfTheToken(Date issuedAtTime, Date expAtTime, long currentTimeInMillis,
                                          long timeStampSkewMillis) throws OAuthClientAuthnException {
        long exp = expAtTime.getTime();
        if (issuedAtTime != null) {
            long iat = issuedAtTime.getTime();
            if (iat > exp) {
                return logAndThrowException(Constants.ErrorMessages.INVALID_IAT_CLAIM);
            }
            if (exp - iat > jwtExpiry) {
                return logAndThrowException(Constants.ErrorMessages.TOKEN_EXP_TOO_LONG_ERROR);
            }
        } else {
            if (exp - currentTimeInMillis + timeStampSkewMillis > jwtExpiry) {
                return logAndThrowException(Constants.ErrorMessages.TOKEN_EXP_TOO_LONG_ERROR);
            }
        }
        return true;
    }

    private boolean validateJTIInCache(String jti, String issuer, SignedJWT signedJWT, JWTCacheEntry entry,
                                       long currentTimeInMillis, long timeStampSkewMillis, JWTCache jwtCache)
            throws OAuthClientAuthnException {

        if (entry == null) {
            // Update the cache with the new JWT for the same JTI.
            jwtCache.addToCache(jti + ":" + issuer, new JWTCacheEntry(signedJWT));
        } else {
            try {
                SignedJWT cachedJWT = entry.getJwt();
                long cachedJWTExpiryTimeMillis = cachedJWT.getJWTClaimsSet().getExpirationTime().getTime();
                if (checkJTIValidityPeriod(jti, cachedJWTExpiryTimeMillis, currentTimeInMillis, timeStampSkewMillis)) {
                    // Update the cache with the new JWT for the same JTI.
                    jwtCache.addToCache(jti+ ":" + issuer, new JWTCacheEntry(signedJWT));
                } else {
                    return false;
                }
            } catch (ParseException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to parse the cached jwt assertion : " + entry.getEncodedJWt());
                }
                throw new OAuthClientAuthnException(Constants.ErrorMessages.JTI_VALIDATION_FAILED_ERROR,
                        OAuth2ErrorCodes.INVALID_REQUEST);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("JWT id: " + jti + " not found in the cache and the JWT has been validated " +
                    "successfully in cache.");
        }
        return true;
    }

    private boolean validateHeaders(SignedJWT signedJWT) throws OAuthClientAuthnException {

        JWSHeader header = signedJWT.getHeader();
        String algo = header.getAlgorithm() != null ? header.getAlgorithm().getName() : "";
        String type = header.getType() != null ? header.getType().getType() : "";
        String kid = header.getKeyID();
        if (StringUtils.isEmpty(algo) || StringUtils.isEmpty(type) || StringUtils.isEmpty(kid)) {
            return logAndThrowException(Constants.ErrorMessages.MISSING_HEADERS_ERROR);
        }
        if (!(algo.equals(Constants.RS_384) || algo.equals(Constants.ES_384))) {
            return logAndThrowException(Constants.ErrorMessages.UNSUPPORTED_SIGNATURE_ALGORITHM);
        }
        if(!type.equals(Constants.JWT_TYPE)) {
            return logAndThrowException(Constants.ErrorMessages.INVALID_KEY_TYPE);
        }
        return true;
    }

    private ZonedDateTime getUTCDate(long millis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.of(Constants.UTC));
    }
}
