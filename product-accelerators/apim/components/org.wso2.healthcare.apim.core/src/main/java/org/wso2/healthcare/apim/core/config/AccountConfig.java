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
 * Contains Account related configurations
 */
public class AccountConfig {

    private boolean enable = false;
    private String accountDataSourceName;
    private String registrationDataSourceName;
    private PIIStoreConfig piiStoreConfig;

    public AccountConfig() {
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getAccountDataSourceName() {
        return accountDataSourceName;
    }

    public void setAccountDataSourceName(String accountDataSourceName) {
        this.accountDataSourceName = accountDataSourceName;
    }

    public String getRegistrationDataSourceName() {
        return registrationDataSourceName;
    }

    public void setRegistrationDataSourceName(String registrationDataSourceName) {
        this.registrationDataSourceName = registrationDataSourceName;
    }

    public PIIStoreConfig getPiiStoreConfig() {
        return piiStoreConfig;
    }

    public void setPiiStoreConfig(PIIStoreConfig piiStoreConfig) {
        this.piiStoreConfig = piiStoreConfig;
    }
}
