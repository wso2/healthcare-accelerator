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
 * Holds mapping between consent purpose and PII categories
 */
public class PIICategoryMapping {

    private int id;
    private String type;
    private int piiCategoryId;

    public PIICategoryMapping(int id, String type, int piiCategoryId) {
        this.id = id;
        this.type = type;
        this.piiCategoryId = piiCategoryId;
    }

    public PIICategoryMapping() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPiiCategoryId() {
        return piiCategoryId;
    }

    public void setPiiCategoryId(int piiCategoryId) {
        this.piiCategoryId = piiCategoryId;
    }
}
