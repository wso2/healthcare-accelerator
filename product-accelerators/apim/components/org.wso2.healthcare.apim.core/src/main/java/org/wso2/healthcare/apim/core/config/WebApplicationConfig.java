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
 * Web Application related configuration
 */
public class WebApplicationConfig {

    private String deploymentName = ConfigConstants.DEFAULT_DEPLOYMENT_NAME;
    private String deploymentNameContainerCSS = ConfigConstants.DEFAULT_DEPLOYMENT_NAME_CONTAINER_CSS;
    private String termsOfUse = ConfigConstants.DEFAULT_WEBAPP_TERMS_OF_USE;
    private String privacyPolicy = ConfigConstants.DEFAULT_WEBAPP_PRIVACY_POLICY;
    private String cookiePolicy = ConfigConstants.DEFAULT_WEBAPP_COOKIE_POLICY;
    private String logo = ConfigConstants.DEFAULT_WEBAPP_LOGO;
    private String logoHeight = ConfigConstants.DEFAULT_WEBAPP_LOGO_HEIGHT;
    private String logoWidth = ConfigConstants.DEFAULT_WEBAPP_LOGO_WIDTH;
    private String logoContainerCSS = ConfigConstants.DEFAULT_WEBAPP_LOGO_CONTAINER_CSS;
    private String favicon = ConfigConstants.DEFAULT_WEBAPP_FAVICON;
    private String title = ConfigConstants.DEFAULT_WEBAPP_TITLE;
    private String footer = ConfigConstants.DEFAULT_WEBAPP_FOOTER;
    private String footerSecondaryHTML = ConfigConstants.DEFAULT_WEBAPP_FOOTER_SECONDARY_HTML;
    private String mainColor = ConfigConstants.DEFAULT_WEBAPP_MAIN_COLOR;
    private AuthConfig authConfig; 
    private RecoveryConfig recoveryConfig;

    public WebApplicationConfig() {
    }

    public String getDeploymentName() {

        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {

        this.deploymentName = deploymentName;
    }

    public String getDeploymentNameContainerCSS() {

        return deploymentNameContainerCSS;
    }

    public void setDeploymentNameContainerCSS(String deploymentNameContainerCSS) {

        this.deploymentNameContainerCSS = deploymentNameContainerCSS;
    }

    public String getTermsOfUse() {

        return termsOfUse;
    }

    public void setTermsOfUse(String termsOfUse) {

        this.termsOfUse = termsOfUse;
    }

    public String getPrivacyPolicy() {

        return privacyPolicy;
    }

    public void setPrivacyPolicy(String privacyPolicy) {

        this.privacyPolicy = privacyPolicy;
    }

    public String getCookiePolicy() {

        return cookiePolicy;
    }

    public void setCookiePolicy(String cookiePolicy) {

        this.cookiePolicy = cookiePolicy;
    }

    public String getLogo() {

        return logo;
    }

    public void setLogo(String logo) {

        this.logo = logo;
    }

    public String getLogoHeight() {

        return logoHeight;
    }

    public void setLogoHeight(String logoHeight) {

        this.logoHeight = logoHeight;
    }

    public String getLogoWidth() {

        return logoWidth;
    }

    public void setLogoWidth(String logoWidth) {

        this.logoWidth = logoWidth;
    }

    public String getLogoContainerCSS() {

        return logoContainerCSS;
    }

    public void setLogoContainerCSS(String logoContainerCSS) {

        this.logoContainerCSS = logoContainerCSS;
    }

    public String getFavicon() {

        return favicon;
    }

    public void setFavicon(String favicon) {

        this.favicon = favicon;
    }

    public String getTitle() {

        return title;
    }

    public void setTitle(String title) {

        this.title = title;
    }

    public String getFooter() {

        return footer;
    }

    public void setFooter(String footer) {

        this.footer = footer;
    }

    public String getFooterSecondaryHTML() {

        return footerSecondaryHTML;
    }

    public void setFooterSecondaryHTML(String footerSecondaryHTML) {

        this.footerSecondaryHTML = footerSecondaryHTML;
    }

    public String getMainColor() {

        return mainColor;
    }

    public void setMainColor(String mainColor) {

        this.mainColor = mainColor;
    }

    public AuthConfig getAuthConfig() {

        return authConfig;
    }

    public void setAuthConfig(AuthConfig authConfig) {

        this.authConfig = authConfig;
    }

    public RecoveryConfig getRecoveryConfig() {

        return recoveryConfig;
    }

    public void setRecoveryConfig(RecoveryConfig recoveryConfig) {

        this.recoveryConfig = recoveryConfig;
    }
}
