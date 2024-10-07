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

import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.codec.binary.Base64;
import org.mockito.Mockito;
import org.powermock.reflect.internal.WhiteboxImpl;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.common.testng.WithAxisConfiguration;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.common.testng.WithKeyStore;
import org.wso2.carbon.identity.common.testng.WithRealmService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth2.client.authentication.OAuthClientAuthnException;
import org.wso2.carbon.identity.oauth2.internal.OAuth2ServiceComponentHolder;
import org.wso2.healthcare.apim.clientauth.jwt.internal.JWTServiceComponent;
import org.wso2.healthcare.apim.clientauth.jwt.internal.JWTServiceDataHolder;
import org.wso2.carbon.identity.testutil.ReadCertStoreSampleUtil;
import org.wso2.carbon.idp.mgt.internal.IdpMgtServiceComponentHolder;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;

import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Matchers.anyString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.wso2.healthcare.apim.clientauth.jwt.util.JWTTestUtil.buildJWT;
import static org.wso2.healthcare.apim.clientauth.jwt.util.JWTTestUtil.buildJWTDiffAlgo;
import static org.wso2.healthcare.apim.clientauth.jwt.util.JWTTestUtil.buildJWTInvalidTyp;
import static org.wso2.healthcare.apim.clientauth.jwt.util.JWTTestUtil.buildJWTNoKid;
import static org.wso2.healthcare.apim.clientauth.jwt.util.JWTTestUtil.getJWTValidator;
import static org.wso2.healthcare.apim.clientauth.jwt.util.JWTTestUtil.getKeyStoreFromFile;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_ID;

@WithCarbonHome
@WithAxisConfiguration
@WithH2Database(jndiName = "jdbc/WSO2CarbonDB", files = {"dbscripts/identity.sql"}, dbName = "testdb2")
@WithRealmService(tenantId = SUPER_TENANT_ID, tenantDomain = SUPER_TENANT_DOMAIN_NAME,
        injectToSingletons = {JWTServiceComponent.class})
@WithKeyStore
public class JWTValidatorTest {

    public static final String TEST_CLIENT_ID_1 = "KrVLov4Bl3natUksF2HmWsdw684a";
    public static final String TEST_CLIENT_ID_2 = "O99Y3oCCCluRc0dQASZkkFMvOZEa";
    public static final String ID_TOKEN_ISSUER_ID = "http://localhost:9443/oauth2/token";
    private KeyStore clientKeyStore;
    private KeyStore serverKeyStore;
    private X509Certificate cert;

    private static final String CERTIFICATE =
            "MIIDVzCCAj+gAwIBAgIEN+6m4zANBgkqhkiG9w0BAQsFADBcMQswCQYDVQQGEwJGUjEMMAoGA1UE\n" +
                    "CBMDTVBMMQwwCgYDVQQHEwNNUEwxDTALBgNVBAoTBHRlc3QxDTALBgNVBAsTBHRlc3QxEzARBgNV\n" +
                    "BAMMCioudGVzdC5jb20wHhcNMTcxMTIzMTI1MDEyWhcNNDcxMTE2MTI1MDEyWjBcMQswCQYDVQQG\n" +
                    "EwJGUjEMMAoGA1UECBMDTVBMMQwwCgYDVQQHEwNNUEwxDTALBgNVBAoTBHRlc3QxDTALBgNVBAsT\n" +
                    "BHRlc3QxEzARBgNVBAMMCioudGVzdC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB\n" +
                    "AQCITPQCt3fPuVWCLFBnPfslWAIrh8H/sEx+I/mCscfAGMzBr/aKgtSr+6fcCvJgpj31D9Lp5ZY+\n" +
                    "WbpccxeLDDaV4hwAx8P1yi+0xwip8x5UIzjRcJ+n5E/9rjev3QnbynaFzgieyE784BfvO/4fgVTQ\n" +
                    "hAE4ZGdqbm1nD0Ic1qptOs7WCXMyjBy5JvqOD74HD7vSOwC4ySFVTOC8ENyF9i9gtx25+zH2FreJ\n" +
                    "gHkmLoiEUJMoCZ+ShH0tl8LoFVM5CTxWb6iNj28bYqgLAjVkOSO1G2GbOV8XzdaIj1m5ECksdQqf\n" +
                    "770UDjrGNM5VmzxMDDEKjB6/qhs6q4HeCZuzicbhAgMBAAGjITAfMB0GA1UdDgQWBBTiJxYPvJcZ\n" +
                    "3XlcnaZVFpOFbfj5ujANBgkqhkiG9w0BAQsFAAOCAQEAfbtzvY81vgNz5M1MwG78KdOEiSwNU6/y\n" +
                    "RqWBUsa5aB7w6vFdsgZ1D/J2v5VnVwXHrmWHCiIkXk70kD0gFJhDa4gNPsuAs0acMcZumEzjY8P2\n" +
                    "0s4LP5TOfCHraPMElFWmHwZI4/SaR5xGgzRxehqJ+KP6UKHWkhf/NP+SBetVAdXfNFp/hO+67XFe\n" +
                    "aFr3vXKegooXrm58vCvg/J1nJapbhWiTDvgeNF5EhnLDNs04oBsOcjzrGDihv4F+Vl1yx/RelAwv\n" +
                    "W/bQM+jWUllR4Qpwx6R1mVy3pFRl0+4npUr17XOGEoP9Xm/5kMvsiNOTqryR5p3xEPBQcXBJES8K\n" +
                    "oQon6A==";

    @BeforeClass
    public void setUp() throws Exception {

        Map<Integer, Certificate> publicCerts = new ConcurrentHashMap<>();
        publicCerts.put(SUPER_TENANT_ID, ReadCertStoreSampleUtil.createKeyStore(getClass())
                .getCertificate("wso2carbon"));
        clientKeyStore = getKeyStoreFromFile("testkeystore.jks", "wso2carbon",
                System.getProperty(CarbonBaseConstants.CARBON_HOME));
        serverKeyStore = getKeyStoreFromFile("wso2carbon.jks", "wso2carbon",
                System.getProperty(CarbonBaseConstants.CARBON_HOME));

        KeyStoreManager keyStoreManager = Mockito.mock(KeyStoreManager.class);
        ConcurrentHashMap<String, KeyStoreManager> mtKeyStoreManagers = new ConcurrentHashMap();
        mtKeyStoreManagers.put(String.valueOf(SUPER_TENANT_ID), keyStoreManager);
        WhiteboxImpl.setInternalState(KeyStoreManager.class, "mtKeyStoreManagers", mtKeyStoreManagers);
        cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
                new ByteArrayInputStream(Base64.decodeBase64(CERTIFICATE)));
        Mockito.when(keyStoreManager.getDefaultPrimaryCertificate()).thenReturn(cert);
        Mockito.when(keyStoreManager.getPrimaryKeyStore()).thenReturn(serverKeyStore);
        Mockito.when(keyStoreManager.getKeyStore("wso2carbon.jks")).thenReturn(serverKeyStore);

        ServiceProvider mockedServiceProvider = Mockito.mock(ServiceProvider.class);
        Mockito.when(mockedServiceProvider.getCertificateContent()).thenReturn(CERTIFICATE);

        ApplicationManagementService mockedApplicationManagementService = Mockito.mock(ApplicationManagementService
                .class);
        Mockito.when(mockedApplicationManagementService.getServiceProviderByClientId(anyString(), anyString(),
                anyString())).thenReturn(mockedServiceProvider);
        OAuth2ServiceComponentHolder.setApplicationMgtService(mockedApplicationManagementService);

        RealmService realmService = IdentityTenantUtil.getRealmService();
        UserRealm userRealm = realmService.getTenantUserRealm(SUPER_TENANT_ID);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUserRealm(userRealm);
        JWTServiceDataHolder.getInstance().setRealmService(realmService);
        IdpMgtServiceComponentHolder.getInstance().setRealmService(realmService);

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("OAuth.OpenIDConnect.IDTokenIssuerID", ID_TOKEN_ISSUER_ID);
        WhiteboxImpl.setInternalState(IdentityUtil.class, "configuration", configuration);
    }

    @DataProvider(name = "provideJWT")
    public Object[][] createJWT() throws Exception {

        Key key1 = clientKeyStore.getKey("wso2carbon", "wso2carbon".toCharArray());
        String audience = ID_TOKEN_ISSUER_ID;
        String jsonWebToken0 = buildJWT(TEST_CLIENT_ID_1, TEST_CLIENT_ID_1, "4000", audience, 350000, key1);
        String jsonWebToken1 = buildJWT(TEST_CLIENT_ID_1, TEST_CLIENT_ID_1, "4001", audience, 350000, 150000, key1);
        String jsonWebToken2 = buildJWT(TEST_CLIENT_ID_1, TEST_CLIENT_ID_1, "3000", audience, 300000, key1);
        String jsonWebToken3 = buildJWT(TEST_CLIENT_ID_1, TEST_CLIENT_ID_1, "3000", audience, 250000, key1);
        String jsonWebToken4 = buildJWT(TEST_CLIENT_ID_2, TEST_CLIENT_ID_2, "3000", audience, 250000, key1);
        String jsonWebToken5 = buildJWTDiffAlgo(TEST_CLIENT_ID_2, TEST_CLIENT_ID_2, "3000", audience, 250000, key1);
        String jsonWebToken6 = buildJWTInvalidTyp(TEST_CLIENT_ID_2, TEST_CLIENT_ID_2, "3000", audience, 250000, key1);
        String jsonWebToken7 = buildJWTNoKid(TEST_CLIENT_ID_2, TEST_CLIENT_ID_2, "3000", audience, 250000, key1);

        return new Object[][]{
                {jsonWebToken0, false},
                {jsonWebToken1, true},
                {jsonWebToken2, true},
                {jsonWebToken3, false},
                {jsonWebToken4, true},
                {jsonWebToken5, false},
                {jsonWebToken6, false},
                {jsonWebToken7, false},
        };
    }

    @Test(dataProvider = "provideJWT")
    public void testValidateToken(String jwt, boolean expected) throws Exception {

        JWTValidator jwtValidator = getJWTValidator();
        SignedJWT signedJWT = SignedJWT.parse(jwt);
        try {
            assertEquals(jwtValidator.isValidAssertion(signedJWT), expected);
        } catch (OAuthClientAuthnException e) {
            assertFalse(expected);
        }

    }

    @Test
    public void testValidateTokenWithCustomExpiry() throws Exception {
        Key key1 = clientKeyStore.getKey("wso2carbon", "wso2carbon".toCharArray());
        String audience = ID_TOKEN_ISSUER_ID;
        JWTValidator jwtValidator = getJWTValidator(360000);
        String jwt = buildJWT(TEST_CLIENT_ID_1, TEST_CLIENT_ID_1, "4010", audience, 350000, key1);
        SignedJWT signedJWT = SignedJWT.parse(jwt);
        assertTrue(jwtValidator.isValidAssertion(signedJWT));
    }

    @Test(expectedExceptions = OAuthClientAuthnException.class)
    public void testValidateTokenWithInvalidIat() throws Exception {
        Key key1 = clientKeyStore.getKey("wso2carbon", "wso2carbon".toCharArray());
        String audience = ID_TOKEN_ISSUER_ID;
        JWTValidator jwtValidator = getJWTValidator();
        String jwt = buildJWT(TEST_CLIENT_ID_1, TEST_CLIENT_ID_1, "4020", audience, 350000, 400000, key1);
        SignedJWT signedJWT = SignedJWT.parse(jwt);
        jwtValidator.isValidAssertion(signedJWT);
    }
}
