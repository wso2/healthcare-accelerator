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

package org.wso2.healthcare.apim.clientauth.jwt.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.healthcare.apim.clientauth.jwt.Constants;
import org.wso2.healthcare.apim.clientauth.jwt.validator.JWTValidator;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class JWTTestUtil {

    public static String buildJWT(String issuer, String subject, String jti, String audience, long lifetimeInMillis,
                                  Key privateKey)
            throws IdentityOAuth2Exception {

        long curTimeInMillis = Calendar.getInstance().getTimeInMillis();

        // Set claims to jwt token.
        JWTClaimsSet.Builder jwtClaimsSetBuilder = new JWTClaimsSet.Builder();
        jwtClaimsSetBuilder.issuer(issuer);
        jwtClaimsSetBuilder.subject(subject);
        jwtClaimsSetBuilder.audience(Arrays.asList(audience));
        jwtClaimsSetBuilder.jwtID(jti);
        jwtClaimsSetBuilder.expirationTime(new Date(curTimeInMillis + lifetimeInMillis));

        JWTClaimsSet jwtClaimsSet = jwtClaimsSetBuilder.build();

        JWSAlgorithm signatureAlgorithm = new JWSAlgorithm(JWSAlgorithm.RS384.getName());
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(signatureAlgorithm);
        headerBuilder.type(JOSEObjectType.JWT);
        headerBuilder.keyID("1");
        JWSHeader jwsHeader = headerBuilder.build();

        return signJWTWithRSA(jwsHeader, jwtClaimsSet, privateKey);
    }

    public static String buildJWT(String issuer, String subject, String jti, String audience, long lifetimeInMillis,
                                  long issuedTime, Key privateKey)
            throws IdentityOAuth2Exception {

        long curTimeInMillis = Calendar.getInstance().getTimeInMillis();

        // Set claims to jwt token.
        JWTClaimsSet.Builder jwtClaimsSetBuilder = new JWTClaimsSet.Builder();
        jwtClaimsSetBuilder.issuer(issuer);
        jwtClaimsSetBuilder.subject(subject);
        jwtClaimsSetBuilder.audience(Arrays.asList(audience));
        jwtClaimsSetBuilder.jwtID(jti);
        jwtClaimsSetBuilder.expirationTime(new Date(curTimeInMillis + lifetimeInMillis));
        jwtClaimsSetBuilder.issueTime(new Date(curTimeInMillis + issuedTime));

        JWTClaimsSet jwtClaimsSet = jwtClaimsSetBuilder.build();
        JWSAlgorithm signatureAlgorithm = new JWSAlgorithm(JWSAlgorithm.RS384.getName());
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(signatureAlgorithm);
        headerBuilder.type(JOSEObjectType.JWT);
        headerBuilder.keyID("1");
        JWSHeader jwsHeader = headerBuilder.build();

        return signJWTWithRSA(jwsHeader, jwtClaimsSet, privateKey);
    }

    public static String buildJWTDiffAlgo(String issuer, String subject, String jti, String audience,
                                       long lifetimeInMillis, Key privateKey)
            throws IdentityOAuth2Exception {

        long curTimeInMillis = Calendar.getInstance().getTimeInMillis();

        // Set claims to jwt token.
        JWTClaimsSet.Builder jwtClaimsSetBuilder = new JWTClaimsSet.Builder();
        jwtClaimsSetBuilder.issuer(issuer);
        jwtClaimsSetBuilder.subject(subject);
        jwtClaimsSetBuilder.audience(Arrays.asList(audience));
        jwtClaimsSetBuilder.jwtID(jti);
        jwtClaimsSetBuilder.expirationTime(new Date(curTimeInMillis + lifetimeInMillis));

        JWTClaimsSet jwtClaimsSet = jwtClaimsSetBuilder.build();

        JWSAlgorithm signatureAlgorithm = new JWSAlgorithm(JWSAlgorithm.RS256.getName());
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(signatureAlgorithm);
        headerBuilder.type(JOSEObjectType.JWT);
        headerBuilder.keyID("1");
        JWSHeader jwsHeader = headerBuilder.build();

        return signJWTWithRSA(jwsHeader, jwtClaimsSet, privateKey);
    }

    public static String buildJWTInvalidTyp(String issuer, String subject, String jti, String audience,
                                       long lifetimeInMillis,
                                  Key privateKey)
            throws IdentityOAuth2Exception {

        long curTimeInMillis = Calendar.getInstance().getTimeInMillis();

        // Set claims to jwt token.
        JWTClaimsSet.Builder jwtClaimsSetBuilder = new JWTClaimsSet.Builder();
        jwtClaimsSetBuilder.issuer(issuer);
        jwtClaimsSetBuilder.subject(subject);
        jwtClaimsSetBuilder.audience(Arrays.asList(audience));
        jwtClaimsSetBuilder.jwtID(jti);
        jwtClaimsSetBuilder.expirationTime(new Date(curTimeInMillis + lifetimeInMillis));

        JWTClaimsSet jwtClaimsSet = jwtClaimsSetBuilder.build();

        JWSAlgorithm signatureAlgorithm = new JWSAlgorithm(JWSAlgorithm.RS384.getName());
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(signatureAlgorithm);
        headerBuilder.type(JOSEObjectType.JOSE);
        headerBuilder.keyID("1");
        JWSHeader jwsHeader = headerBuilder.build();

        return signJWTWithRSA(jwsHeader, jwtClaimsSet, privateKey);
    }

    public static String buildJWTNoKid(String issuer, String subject, String jti, String audience,
                                       long lifetimeInMillis, Key privateKey)
            throws IdentityOAuth2Exception {

        long curTimeInMillis = Calendar.getInstance().getTimeInMillis();

        // Set claims to jwt token.
        JWTClaimsSet.Builder jwtClaimsSetBuilder = new JWTClaimsSet.Builder();
        jwtClaimsSetBuilder.issuer(issuer);
        jwtClaimsSetBuilder.subject(subject);
        jwtClaimsSetBuilder.audience(Arrays.asList(audience));
        jwtClaimsSetBuilder.jwtID(jti);
        jwtClaimsSetBuilder.expirationTime(new Date(curTimeInMillis + lifetimeInMillis));

        JWTClaimsSet jwtClaimsSet = jwtClaimsSetBuilder.build();

        JWSAlgorithm signatureAlgorithm = new JWSAlgorithm(JWSAlgorithm.RS384.getName());
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(signatureAlgorithm);
        headerBuilder.type(JOSEObjectType.JWT);
        JWSHeader jwsHeader = headerBuilder.build();

        return signJWTWithRSA(jwsHeader, jwtClaimsSet, privateKey);
    }

    /**
     * sign JWT token from RSA algorithm
     *
     * @param jwtClaimsSet contains JWT body
     * @param privateKey
     * @return signed JWT token
     * @throws IdentityOAuth2Exception
     */
    public static String signJWTWithRSA(JWSHeader header, JWTClaimsSet jwtClaimsSet, Key privateKey)
            throws IdentityOAuth2Exception {

        try {
            JWSSigner signer = new RSASSASigner((RSAPrivateKey) privateKey);
            SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IdentityOAuth2Exception("Error occurred while signing JWT", e);
        }
    }

    /**
     * Read Keystore from the file identified by given keystorename, password
     *
     * @param keystoreName
     * @param password
     * @param home
     * @return
     * @throws Exception
     */
    public static KeyStore getKeyStoreFromFile(String keystoreName, String password,
                                               String home) throws Exception {

        Path tenantKeystorePath = Paths.get(home, "repository",
                "resources", "security", keystoreName);
        FileInputStream file = new FileInputStream(tenantKeystorePath.toString());
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(file, password.toCharArray());
        return keystore;
    }

    /**
     * Create and return a JWTValidator instance with expiry time
     *
     * @return JWTValidator instance
     */
    public static JWTValidator getJWTValidator(int jwtExpiry) {

        return new JWTValidator(null, populateMandatoryClaims(), true, jwtExpiry);
    }

    /**
     * Create and return a JWTValidator instance
     *
     * @return JWTValidator instance
     */
    public static JWTValidator getJWTValidator() {

        return new JWTValidator(null, populateMandatoryClaims(), true, Constants.DEFAULT_JWT_EXP);
    }

    private static List<String> populateMandatoryClaims() {

        List<String> mandatoryClaims = new ArrayList<>();
        mandatoryClaims.add(Constants.ISSUER_CLAIM);
        mandatoryClaims.add(Constants.SUBJECT_CLAIM);
        mandatoryClaims.add(Constants.AUDIENCE_CLAIM);
        mandatoryClaims.add(Constants.EXPIRATION_TIME_CLAIM);
        mandatoryClaims.add(Constants.JWT_ID_CLAIM);
        return mandatoryClaims;
    }
}
