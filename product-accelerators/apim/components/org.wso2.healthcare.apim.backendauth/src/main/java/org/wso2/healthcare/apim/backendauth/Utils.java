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
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.wso2.healthcare.apim.core.OpenHealthcareException;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Utility class for the backend authentication.
 */
public class Utils {
    private static final Log log = LogFactory.getLog(Utils.class);
    public static final String ALLOW_ALL = "AllowAll";
    public static final String STRICT = "Strict";
    public static final String HOST_NAME_VERIFIER = "httpclient.hostnameVerifier";
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    private static final String[] SUPPORTED_HTTP_PROTOCOLS = {"TLSv1.2"};

    public static void setErrorResponse(MessageContext messageContext, Throwable e, int errorCode, String msg) {

        messageContext.setProperty(SynapseConstants.ERROR_CODE, errorCode);
        messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, msg);
        messageContext.setProperty(SynapseConstants.ERROR_DETAIL, e.getMessage());
        messageContext.setProperty(SynapseConstants.ERROR_EXCEPTION, e);
        ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty(Constants.HTTP_SC, errorCode);
    }

    /**
     * Get closeable https client.
     *
     * @return Closeable https client
     * @throws OpenHealthcareException exception
     */
    public static CloseableHttpClient getHttpsClient(String trustStorePath, char[] trustStorePass) throws OpenHealthcareException {

        SSLConnectionSocketFactory sslsf = createSSLConnectionSocketFactory(trustStorePath,trustStorePass);

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register(HTTP_PROTOCOL, new PlainConnectionSocketFactory())
                .register(HTTPS_PROTOCOL, sslsf)
                .build();

        final PoolingHttpClientConnectionManager connectionManager = getPoolingHttpClientConnectionManager(socketFactoryRegistry);

        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }

    /**
     * Get the pooling http client connection manager.
     * @param socketFactoryRegistry - Socket factory registry
     * @return PoolingHttpClientConnectionManager
     */
    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(Registry<ConnectionSocketFactory> socketFactoryRegistry) {
        final PoolingHttpClientConnectionManager connectionManager = (socketFactoryRegistry != null) ?
                new PoolingHttpClientConnectionManager(socketFactoryRegistry) :
                new PoolingHttpClientConnectionManager();

        // configuring default maximum connections
        // todo: make these values configurable:
        connectionManager.setMaxTotal(Constants.DEFAULT_MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(Constants.DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
        return connectionManager;
    }

    /**
     * create an SSL Connection Socket Factory.
     *
     * @return SSLConnectionSocketFactory
     * @throws OpenHealthcareException when failed to create the SSL Connection Socket Factory
     */
    private static SSLConnectionSocketFactory createSSLConnectionSocketFactory(String trustStorePath, char[] trustStorePassword)
            throws OpenHealthcareException {

        KeyStore trustStore = null;

        trustStore = loadKeyStore(
                trustStorePath,
                trustStorePassword);

        // Trust own CA and all self-signed certs
        SSLContext sslcontext;
        try {
            sslcontext = SSLContexts.custom().loadTrustMaterial(trustStore, (TrustStrategy) new TrustSelfSignedStrategy()).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new OpenHealthcareException("Unable to create the ssl context", e);
        }

        // Allow TLSv1 protocol only
        return new SSLConnectionSocketFactory(sslcontext, SUPPORTED_HTTP_PROTOCOLS,
                null, getX509HostnameVerifier());

    }


    /**
     * Load the keystore when the location and password is provided.
     *
     * @param keyStoreLocation Location of the keystore
     * @param keyStorePassword Keystore password
     * @return Keystore as an object
     * @throws OpenHealthcareException when failed to load Keystore from given details
     */
    public static KeyStore loadKeyStore(String keyStoreLocation, char[] keyStorePassword)
            throws OpenHealthcareException {

        KeyStore keyStore;

        try (FileInputStream inputStream = new FileInputStream(keyStoreLocation)) {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(inputStream, keyStorePassword);
            return keyStore;
        } catch (KeyStoreException e) {
            throw new OpenHealthcareException("Error while retrieving aliases from keystore", e);
        } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
            throw new OpenHealthcareException("Error while loading keystore", e);
        }
    }

    /**
     * Get the Hostname Verifier property in set in system properties.
     *
     * @return X509HostnameVerifier
     */
    public static X509HostnameVerifier getX509HostnameVerifier() {

        String hostnameVerifierOption = System.getProperty(HOST_NAME_VERIFIER);
        X509HostnameVerifier hostnameVerifier;

        if (ALLOW_ALL.equals(hostnameVerifierOption)) {
            hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
        } else if (STRICT.equals(hostnameVerifierOption)) {
            hostnameVerifier = SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
        } else {
            hostnameVerifier = SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Proceeding with %s : %s", HOST_NAME_VERIFIER,
                    hostnameVerifierOption));
        }
        return hostnameVerifier;

    }

    /**
     * Helper method to add kid claim into to JWT_HEADER.
     *
     * @param cert X509 certificate
     * @return KID
     */
    public static String getKID(X509Certificate cert) {
        String serialNumber = cert.getSerialNumber().toString();
        String issuerName = cert.getIssuerDN().getName();
        String kid = issuerName + "#" + serialNumber;
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(kid.getBytes(StandardCharsets.UTF_8));
    }

}
