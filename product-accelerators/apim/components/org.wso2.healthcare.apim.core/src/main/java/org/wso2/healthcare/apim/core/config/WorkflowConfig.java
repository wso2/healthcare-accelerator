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
 * Contains workflow related config
 */
public class WorkflowConfig {

    private final String type;
    private final String processDefinitionKey;
    private final String serverUrl;
    private final String username;
    private final char[] password;

    public WorkflowConfig(String type, String processDefinitionKey, String serverUrl, String username, char[] password) {

        /**
         * Workflow types : default, BPMN
         *
         * default - In-memory workflow which creates account without involving process
         * BPMN - Triggers external BPMN and trigger account creation process
         */
        this.type = type;

        this.processDefinitionKey = processDefinitionKey;
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
    }

    public String getType() {

        return type;
    }

    public String getProcessDefinitionKey() {

        return processDefinitionKey;
    }

    public String getServerUrl() {

        return serverUrl;
    }

    public String getUsername() {

        return username;
    }

    public char[] getPassword() {

        return password;
    }
}
