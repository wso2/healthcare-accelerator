/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.com).
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.apim.core.config;

import java.util.Map;

/**
 * Email notification related configuration.
 */
public class MailNotificationConfig {
    private String name;
    private boolean enable;
    private String recipientRoles;
    private String recipients;
    private String emailSubject;
    private String emailBody;
    private Map<String, String> placeHolders;

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public boolean isEnable() {

        return enable;
    }

    public void setEnable(boolean enable) {

        this.enable = enable;
    }

    public String getRecipientRoles() {

        return recipientRoles;
    }

    public void setRecipientRoles(String recipientRoles) {

        this.recipientRoles = recipientRoles;
    }

    public String getRecipients() {

        return recipients;
    }

    public void setRecipients(String recipients) {

        this.recipients = recipients;
    }

    public String getEmailSubject() {

        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {

        this.emailSubject = emailSubject;
    }

    public String getEmailBody() {

        return emailBody;
    }

    public void setEmailBody(String emailBody) {

        this.emailBody = emailBody;
    }

    public Map<String, String> getPlaceHolders() {

        return placeHolders;
    }

    public void setPlaceHolders(Map<String, String> placeHolders) {

        this.placeHolders = placeHolders;
    }
}
