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
 * Contains sign-up related configurations
 */
public class SignUpConfig {

    /**
     * This property indicates restriction imposed at registration for creating one account per email.
     * By setting "unique_email_account = true" indicates that restriction is enabled.
     * Default : false
     */
    private boolean uniqueAccountPerEmail = false;

    private WorkflowConfig accountCreationWFConfig;

    public SignUpConfig() {
    }

    public boolean isUniqueAccountPerEmail() {
        return uniqueAccountPerEmail;
    }

    public void setUniqueAccountPerEmail(boolean uniqueAccountPerEmail) {
        this.uniqueAccountPerEmail = uniqueAccountPerEmail;
    }

    public void setAccountCreationWFConfig(WorkflowConfig accountCreationWFConfig) {
        this.accountCreationWFConfig = accountCreationWFConfig;
    }

    public WorkflowConfig getAccountCreationWFConfig() {
        return accountCreationWFConfig;
    }
}
