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
 * Authentication endpoint (Self Signup) related configuration
 */
public class AuthConfig {

    private Boolean selfSignupEnable = true;
    private String selfSignupURL = ConfigConstants.DEFAULT_AUTH_SELFSIGNUP_URL;
    private String signInDisclaimerPortalName = ConfigConstants.DEFAULT_WEBAPP_TITLE;

    public AuthConfig() {
    }

    public Boolean isSelSignupEnable() {

        return selfSignupEnable;
    }

    public void setSelSignupEnable(Boolean selfSignupEnable) {

        this.selfSignupEnable = selfSignupEnable;
    }

    public String getSelfSignupURL() {

        return selfSignupURL;
    }

    public void setSelfSignupURL(String selfSignupURL) {

        this.selfSignupURL = selfSignupURL;
    }

    public String getSignInDisclaimerPortalName() {
        return signInDisclaimerPortalName;
    }

    public void setSignInDisclaimerPortalName(String signInDisclaimerPortalName) {
        this.signInDisclaimerPortalName = signInDisclaimerPortalName;
    }
}
