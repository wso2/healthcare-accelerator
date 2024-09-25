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

package org.wso2.healthcare.consentmgt.core.model;

/**
 * Model class representing service provider application to consent purpose mapping.
 */
public class SPPurposeMapping {
    private Integer id;
    private String consumerKey;
    private Integer purposeId;
    private String appName;
    private String purposeName;

    public SPPurposeMapping(Integer id, String consumerKey, Integer purposeId) {
        this.id = id;
        this.consumerKey = consumerKey;
        this.purposeId = purposeId;
    }

    public SPPurposeMapping(Integer id, String consumerKey, Integer purposeId, String appName) {
        this.id = id;
        this.consumerKey = consumerKey;
        this.purposeId = purposeId;
        this.appName = appName;
    }

    public SPPurposeMapping(Integer id, String consumerKey, Integer purposeId, String purposeName, String appName) {
        this.id = id;
        this.consumerKey = consumerKey;
        this.purposeId = purposeId;
        this.appName = appName;
        this.purposeName = purposeName;
    }

    public SPPurposeMapping() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getPurposeName() {
        return purposeName;
    }

    public void setPurposeName(String purposeName) {
        this.purposeName = purposeName;
    }

    public Integer getPurposeId() {
        return purposeId;
    }

    public void setPurposeId(Integer purposeId) {
        this.purposeId = purposeId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
