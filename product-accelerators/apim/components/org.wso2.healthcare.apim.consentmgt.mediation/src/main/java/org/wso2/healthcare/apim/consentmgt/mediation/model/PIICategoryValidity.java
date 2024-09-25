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

import com.google.gson.annotations.SerializedName;

public class PIICategoryValidity {
    @SerializedName("piiCategoryId")
    private Integer id;
    private String validity;
    @SerializedName("piiCategoryName")
    private String name;
    @SerializedName("piiCategoryDisplayName")
    private String displayName;

    public PIICategoryValidity(Integer id, String validity) {
        this.id = id;
        this.validity = validity;
    }

    public PIICategoryValidity(String name, String validity, int id, String displayName) {
        this.id = id;
        this.validity = validity;
        this.name = name;
        this.displayName = displayName;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValidity() {
        return this.validity;
    }

    public void setValidity(String validity) {
        this.validity = validity;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}