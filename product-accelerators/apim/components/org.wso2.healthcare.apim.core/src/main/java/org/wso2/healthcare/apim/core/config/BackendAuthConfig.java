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

package org.wso2.healthcare.apim.core.config;

public class BackendAuthConfig {

    private boolean enableBackendAuth;
    private String authEndpoint;
    private String clientId;
    private char[] keystorePassword;
    private char[] truststorePassword;
    private boolean isSSLEnabled;
    private String privateKeyAlias;
    private String sslCertAlias;

    public boolean isEnableBackendAuth() {
        return enableBackendAuth;
    }

    public void setEnableBackendAuth(boolean enableBackendAuth) {
        this.enableBackendAuth = enableBackendAuth;
    }

    public String getAuthEndpoint() {
        return authEndpoint;
    }

    public void setAuthEndpoint(String authEndpoint) {
        this.authEndpoint = authEndpoint;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public char[] getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(char[] keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public char[] getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(char[] truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public boolean isSSLEnabled() {
        return isSSLEnabled;
    }

    public void setSSLEnabled(boolean SSLEnabled) {
        isSSLEnabled = SSLEnabled;
    }

    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    public void setPrivateKeyAlias(String privateKeyAlias) {
        this.privateKeyAlias = privateKeyAlias;
    }

    public String getSslCertAlias() {
        return sslCertAlias;
    }

    public void setSslCertAlias(String sslCertAlias) {
        this.sslCertAlias = sslCertAlias;
    }
}
