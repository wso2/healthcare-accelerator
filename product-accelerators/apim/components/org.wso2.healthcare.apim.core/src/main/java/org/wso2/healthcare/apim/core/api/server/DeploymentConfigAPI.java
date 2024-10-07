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

package org.wso2.healthcare.apim.core.api.server;

import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;

/**
 * Internal Java API to retrieve deployment configurations
 */
public class DeploymentConfigAPI {

     /**
     * Function to retrieve deployment name 
     * @return
     * @throws OpenHealthcareException
     */
    public static String getOHDeploymentName() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getDeploymentName();
    }

    /**
     * Function to retrieve deployment name container CSS
     * @return
     * @throws OpenHealthcareException
     */
    public static String getOHDeploymentNameContainerCSS() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getDeploymentNameContainerCSS();
    }

    /**
     * Function to retrieve web application terms of use configuration
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappTermsOfUse() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getTermsOfUse();
    }

    /**
     * Function to retrieve web application privacy policy configuration
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappPrivacyPolicy() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getPrivacyPolicy();
    }

    /**
     * Function to retrieve web application cookie policy configuration
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappCookiePolicy() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getCookiePolicy();
    }

    /**
     * Function to retrieve web application logo file path configuration
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappLogoFilePath() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getLogo();
    }

    /**
     * Function to retrieve web application logo height configuration
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappLogoHeight() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getLogoHeight();
    }

    /**
     * Function to retrieve web application logo width configuration
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappLogoWidth() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getLogoWidth();
    }

    /**
     * Function to retrieve web application logo container CSS configuration
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappLogoContainerCSS() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getLogoContainerCSS();
    }

    /**
     * Function to retrieve web application Favicon
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappFavicon() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getFavicon();
    }

    /**
     * Function to retrieve web application Title
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappTitle() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getTitle();
    }

    /**
     * Function to retrieve web application Footer
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappFooter() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getFooter();
    }

    /**
     * Function to retrieve web application secondary footer html
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappFooterSecondaryHTML() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getFooterSecondaryHTML();
    }


    /**
     * Function to retrieve web application main color configuration
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappMainColor() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getMainColor();
    }

    /**
     * Function to retrieve web application self signup enable configuration
     * @return
     * @throws OpenHealthcareException
     */
    public static Boolean isWebappSelSignupEnable() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getAuthConfig().isSelSignupEnable();
    }
    
    /**
     * Function to retrieve web application self signup url configuration
     * @return
     * @throws OpenHealthcareException
     */
    public static String getWebappSelfSignupURL() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getAuthConfig().getSelfSignupURL();
    }

    /**
     * Function to retrieve portal name for Sign-in disclaimer notice
     * @return
     * @throws OpenHealthcareException
     */
    public static String getSigninDisclaimerPortalName() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getAuthConfig().getSignInDisclaimerPortalName();
    }

    /**
     * Function to retrieve web application recovery endpoint username info enable configuration
     * @return
     * @throws OpenHealthcareException
     */
    public static Boolean isWebappUsernameInfoEnable() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getRecoveryConfig().isUsernameInfoEnable();
    }

    /**
     * Function to retrieve recovery endpoint sign-up success message
     * @return
     * @throws OpenHealthcareException
     */
    public static String getSignUpSuccessMessage() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getWebApplicationConfig().getRecoveryConfig().getSignUpSuccessMessage();
    }
}
