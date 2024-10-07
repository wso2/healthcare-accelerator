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

package org.wso2.healthcare.apim.consentmgt.mediation.model;

import java.util.ArrayList;
import java.util.List;

public class Purpose {
    private Integer purposeId;
    private String purpose;
    private String description;
    private String group;
    private String groupType;
    private List<PiiCategory> piiCategories;

    public Purpose() {
        piiCategories = new ArrayList<>();
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public List<PiiCategory> getPiiCategories() {
        return piiCategories;
    }

    public void setPiiCategories(List<PiiCategory> piiCategories) {
        this.piiCategories = piiCategories;
    }

    public Integer getPurposeId() {
        return purposeId;
    }

    public void setPurposeId(Integer purposeId) {
        this.purposeId = purposeId;
    }

    public static class PiiCategory {
        private int piiCategoryId;
        private boolean mandatory;

        public int getPiiCategoryId() {
            return piiCategoryId;
        }

        public void setPiiCategoryId(int piiCategoryId) {
            this.piiCategoryId = piiCategoryId;
        }

        public boolean isMandatory() {
            return mandatory;
        }

        public void setMandatory(boolean mandatory) {
            this.mandatory = mandatory;
        }
    }
}
