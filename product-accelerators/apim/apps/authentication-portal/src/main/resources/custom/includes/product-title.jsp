<%--
  ~ Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~  WSO2 Inc. licenses this file to you under the Apache License,
  ~  Version 2.0 (the "License"); you may not use this file except
  ~  in compliance with the License.
  ~  You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
--%>

<!-- localize.jsp MUST already be included in the calling script -->

<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.wso2.healthcare.apim.core.api.server.DeploymentConfigAPI" %>
<% 
String headerTitle = DeploymentConfigAPI.getOHDeploymentName();
String titleContainerCSS = DeploymentConfigAPI.getOHDeploymentNameContainerCSS();
String logoFilePath = DeploymentConfigAPI.getWebappLogoFilePath();
String logoContainerCSS = DeploymentConfigAPI.getWebappLogoContainerCSS();
String logoHeight = DeploymentConfigAPI.getWebappLogoHeight();
String logoWidth = DeploymentConfigAPI.getWebappLogoWidth();
%>

<div class="product-title">
    <span class="theme-icon inline auto transparent product-logo" style="<%=logoContainerCSS%>">
        <img src="<%=logoFilePath%>" alt="WSO2 Logo"  height="<%=logoHeight%>" width="<%=logoWidth%>"/>
      </span>
   <span class="product-title-text" style="<%=titleContainerCSS%>"><%=headerTitle%></span>
</div>

