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

/**
 * Represents Captcha related configurations
 */
public class CaptchaConfig {

    private boolean enable = false;
    private String apiUrl;
    private String verifyUrl;
    private String siteKey;
    private char[] secretKey;

    public boolean isEnable() {

        return enable;
    }

    public void setEnable(boolean enable) {

        this.enable = enable;
    }

    public String getApiUrl() {

        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {

        this.apiUrl = apiUrl;
    }

    public String getVerifyUrl() {

        return verifyUrl;
    }

    public void setVerifyUrl(String verifyUrl) {

        this.verifyUrl = verifyUrl;
    }

    public String getSiteKey() {

        return siteKey;
    }

    public void setSiteKey(String siteKey) {

        this.siteKey = siteKey;
    }

    public char[] getSecretKey() {

        return secretKey;
    }

    public void setSecretKey(char[] secretKey) {

        this.secretKey = secretKey;
    }
}
