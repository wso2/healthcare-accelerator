/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.healthcare.apim.consentmgt.mediation.model;

public class PIICategory {
    private String piiCategoryId;
    private boolean sensitive;
    private String displayName;
    private String description;
    private String piiCategory;

    public String getPiiCategoryId() {
        return piiCategoryId;
    }

    public void setPiiCategoryId(String piiCategoryId) {
        this.piiCategoryId = piiCategoryId;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPiiCategory() {
        return piiCategory;
    }

    public void setPiiCategory(String piiCategory) {
        this.piiCategory = piiCategory;
    }
}
