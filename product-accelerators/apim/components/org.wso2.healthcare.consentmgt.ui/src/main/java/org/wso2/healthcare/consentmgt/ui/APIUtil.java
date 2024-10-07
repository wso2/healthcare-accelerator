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

package org.wso2.healthcare.consentmgt.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.wso2.carbon.utils.CarbonUtils;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

/**
 * API Utility functions
 */
public class APIUtil {

    private static final Log log = LogFactory.getLog(APIUtil.class);

    public static HttpEntity doGet(String host, String path, Map<String, String> parameters,
            Map<String, String> headers) {
        CloseableHttpClient httpClient = (CloseableHttpClient) getHttpClient();
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("https");
        uriBuilder.setHost(host);
        uriBuilder.setPath(path);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            uriBuilder.addParameter(entry.getKey(), entry.getValue());
        }

        try {
            HttpGet request = new HttpGet(uriBuilder.build().toString());
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
            CloseableHttpResponse response = httpClient.execute(request);
            return response.getEntity();
        } catch (URISyntaxException | IOException e) {
            log.error("Error occured while getting response.", e);
        }
        return null;
    }

    public static HttpClient getHttpClient() {
        PoolingHttpClientConnectionManager pool = getPoolingHttpClientConnectionManager();
        pool.setMaxTotal(100);
        pool.setDefaultMaxPerRoute(50);
        RequestConfig params = RequestConfig.custom().build();
        return HttpClients.custom().setConnectionManager(pool).setDefaultRequestConfig(params).build();
    }

    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager() {

        PoolingHttpClientConnectionManager poolManager;
        SSLConnectionSocketFactory socketFactory = createSocketFactory();
        org.apache.http.config.Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", socketFactory).build();
        poolManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        return poolManager;
    }

    private static SSLConnectionSocketFactory createSocketFactory() {
        SSLContext sslContext;

        String keyStorePath = CarbonUtils.getServerConfiguration()
                .getFirstProperty("Security.TrustStore.Location");
        String keyStorePassword = CarbonUtils.getServerConfiguration()
                .getFirstProperty("Security.TrustStore.Password");
        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
            sslContext = SSLContexts.custom().loadTrustMaterial(trustStore).build();
            X509HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

            return new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        } catch (KeyStoreException e) {
            log.error("Failed to read from Key Store", e);
        } catch (IOException e) {
            log.error("Key Store not found in " + keyStorePath, e);
        } catch (CertificateException e) {
            log.error("Failed to read Certificate", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to load Key Store from " + keyStorePath, e);
        } catch (KeyManagementException e) {
            log.error("Failed to load key from" + keyStorePath, e);
        }
        return null;
    }
}
