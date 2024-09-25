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

package org.wso2.healthcare.apim.core.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.healthcare.apim.core.OpenHealthcareException;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;

/**
 * Contains HTTP communication related utility functions
 */
public class HttpUtil {

    private static final Log LOG = LogFactory.getLog(HttpUtil.class);
    public static final String HOST_NAME_VERIFIER = "httpclient.hostnameVerifier";
    public static final String STRICT = "Strict";
    public static final String ALLOW_ALL = "AllowAll";

    /**
     * Return a http client instance
     *
     * @param protocol- service endpoint protocol http/https
     * @param maxTotal maximum number of total open connections
     * @param defaultMaxPerRoute maximum number of concurrent connections per route
     * @return
     */
    public static HttpClient getHttpClient(String protocol, int maxTotal, int defaultMaxPerRoute,
                                           int soTimeout, int connectTimeout) {
        PoolingHttpClientConnectionManager pool = null;
        try {
            pool = getPoolingHttpClientConnectionManager(protocol);
            pool.setMaxTotal(maxTotal);
            pool.setDefaultMaxPerRoute(defaultMaxPerRoute);

            SocketConfig socketConfig =
                    SocketConfig.copy(SocketConfig.DEFAULT).
                            setSoTimeout(soTimeout).
                            setSoKeepAlive(false).
                            build();
            pool.setDefaultSocketConfig(socketConfig);
        } catch (OpenHealthcareException e) {
            LOG.error("Error while creating http client connection manager", e);
        }
        RequestConfig params =
                RequestConfig.copy(RequestConfig.DEFAULT).
                        setSocketTimeout(soTimeout).
                        setConnectTimeout(connectTimeout).
                        setConnectionRequestTimeout(connectTimeout).
                        setStaleConnectionCheckEnabled(true).
                        build();
        return HttpClients.custom().setConnectionManager(pool).setDefaultRequestConfig(params).build();
    }

    /**
     * Return a PoolingHttpClientConnectionManager instance
     *
     * @param protocol- service endpoint protocol. It can be http/https
     * @return PoolManager
     */
    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(String protocol)
            throws OpenHealthcareException {
        PoolingHttpClientConnectionManager poolManager;
        if (APIConstants.HTTPS_PROTOCOL.equals(protocol)) {
            SSLConnectionSocketFactory socketFactory = createSocketFactory();
            org.apache.http.config.Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register(APIConstants.HTTPS_PROTOCOL, socketFactory).build();
            poolManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        } else {
            poolManager = new PoolingHttpClientConnectionManager();
        }
        return poolManager;
    }

    private static SSLConnectionSocketFactory createSocketFactory() throws OpenHealthcareException {
        SSLContext sslContext;

        String keyStorePath = CarbonUtils.getServerConfiguration()
                .getFirstProperty(APIConstants.TRUST_STORE_LOCATION);
        String keyStorePassword = CarbonUtils.getServerConfiguration()
                .getFirstProperty(APIConstants.TRUST_STORE_PASSWORD);
        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
            sslContext = SSLContexts.custom().loadTrustMaterial(trustStore).build();

            X509HostnameVerifier hostnameVerifier;
            String hostnameVerifierOption = System.getProperty(HOST_NAME_VERIFIER);

            if (ALLOW_ALL.equalsIgnoreCase(hostnameVerifierOption)) {
                hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
            } else if (STRICT.equalsIgnoreCase(hostnameVerifierOption)) {
                hostnameVerifier = SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
            } else {
                hostnameVerifier = SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
            }

            return new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        } catch (KeyStoreException e) {
            throw  new OpenHealthcareException("Failed to read from Key Store", e);
        } catch (IOException e) {
            throw  new OpenHealthcareException("Key Store not found in " + keyStorePath, e);
        } catch (CertificateException e) {
            throw  new OpenHealthcareException("Failed to read Certificate", e);
        } catch (NoSuchAlgorithmException e) {
            throw  new OpenHealthcareException("Failed to load Key Store from " + keyStorePath, e);
        } catch (KeyManagementException e) {
            throw  new OpenHealthcareException("Failed to load key from" + keyStorePath, e);
        }
    }

}
