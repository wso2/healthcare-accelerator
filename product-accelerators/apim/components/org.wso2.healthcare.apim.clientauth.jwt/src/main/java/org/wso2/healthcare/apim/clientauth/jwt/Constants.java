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

/**
 * Constants are listed here
 */
public class Constants {

    public static final String OAUTH_JWT_BEARER_GRANT_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    public static final String OAUTH_JWT_ASSERTION = "client_assertion";
    public static final String SCOPE = "scope";
    public static final String OAUTH_JWT_ASSERTION_TYPE = "client_assertion_type";
    public static final String DEFAULT_AUDIENCE = "";
    public static final boolean DEFAULT_ENABLE_JTI_CACHE = true;
    public static final String UTC = "UTC";
    public static final String TOKEN_ENDPOINT_ALIAS = "token_endpoint_alias";
    public static final String JWT_ID_CLAIM = "jti";
    public static final String EXPIRATION_TIME_CLAIM = "exp";
    public static final String AUDIENCE_CLAIM = "aud";
    public static final String SUBJECT_CLAIM = "sub";
    public static final String ISSUER_CLAIM = "iss";
    public static final String PRIVATE_KEY_JWT = "signedJWT";
    public static final String JWKS_URI = "jwksURI";
    public static final String RS_384 = "RS384";
    public static final String ES_384 = "ES384";
    public static final String JWT_TYPE = "JWT";
    public static final String FULLSTOP_DELIMITER = ".";
    public static final String DASH_DELIMITER = "-";
    public static final String KEYSTORE_FILE_EXTENSION = ".jks";
    public static final String RS = "RS";
    public static final String PS = "PS";
    public static final String IDP_ENTITY_ID = "IdPEntityId";
    public static final String PROP_ID_TOKEN_ISSUER_ID = "OAuth.OpenIDConnect.IDTokenIssuerID";
    public static final int DEFAULT_JWT_EXP = 300;
    public static final String ALLOWED_JWT_EXPIRY = "max_allowed_jwt_lifetime_seconds";
    public static final int TO_MILLIS = 1000;

    public static class SQLQueries {

        public static final String GET_JWT = "SELECT EXP_TIME FROM OH_IDN_SMART_BE_AUTH WHERE JWT_ID =? AND ISSUER =?";
        public static final String UPDATE_EXP = "UPDATE OH_IDN_SMART_BE_AUTH SET EXP_TIME =? WHERE JWT_ID =? AND " +
                "ISSUER =?";
        public static final String INSERT_JWD_ID = "INSERT INTO OH_IDN_SMART_BE_AUTH (JWT_ID, ISSUER, EXP_TIME)" +
                "VALUES (?,?,?)";
    }
    public static class ErrorMessages {

        public static final String NO_VALID_ASSERTION_ERROR =
                "No Valid JWT Assertion was found for " + Constants.OAUTH_JWT_BEARER_GRANT_TYPE;
        public static final String EMPTY_CLAIMS_ERROR = "Claim values are empty in the given JSON Web Token";
        public static final String RETRIEVING_CLAIMS_SET_ERROR =
                "Error when trying to retrieve claimsSet from the JWT";
        public static final String VALIDATION_WITH_JWKS_ERROR = "Error occurred while validating signature using jwks ";
        public static final String VALIDATING_SIGNATURE_ERROR = "Error while validating the signature";
        public static final String NO_ALGORITHM_FOUND_ERROR =
                "Signature validation failed. No algorithm is found in the JWT header.";
        public static final String KEY_NOT_RSA_ERROR =
                "Signature validation failed. Public key is not an RSA public key.";
        public static final String TOKEN_EXP_TOO_LONG_ERROR = "Token expiry is greater than the allowed expiry time";
        public static final String JTI_VALIDATION_FAILED_ERROR = "JTI validation failed";
        public static final String MISSING_HEADERS_ERROR = "Missing header(s) in the assertion";
        public static final String UNSUPPORTED_SIGNATURE_ALGORITHM = "Unsupported signature algorithm";
        public static final String INVALID_KEY_TYPE = "Invalid key type";
        public static final String RETRIEVING_CERT_ERROR =
                "Unable to retrieve the certificate for the service provider";
        public static final String AUDIENCE_MISMATCH_ERROR = "Failed to match audience values against the token " +
                "endpoint";
        public static final String INVALID_IAT_CLAIM = "IAT value is greater than the expiry value";
    }
}
