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

package org.wso2.healthcare.apim.core.notification;

/**
 * Constants class for holding contstants related to email notifications.
 */
public class EmailNotificationConstants {
    public static final String NEW_USER_SIGNUP_REQUESTED_INTERNAL = "new_user_signup_requested_internal";
    public static final String NEW_USER_SIGNUP_COMPLETED_INTERNAL = "new_user_signup_completed_internal";
    public static final String NEW_USER_SIGNUP_COMPLETED_EXTERNAL = "new_user_signup_completed_external";

    public static final String NEW_APP_CREATION_REQUESTED_INTERNAL = "new_app_creation_requested_internal";
    public static final String NEW_APP_CREATION_COMPLETED_INTERNAL = "new_app_creation_completed_internal";
    public static final String NEW_APP_CREATION_COMPLETED_EXTERNAL = "new_app_creation_completed_external";

    public static final String RECIPIENTS = "recipients";
    public static final String RECIPIENT_ROLE = "recipient_role";
    public static final String PLACEHOLDERS = "placeholders";

    public static final String EMAIL_ADAPTER_SERVICE_NAME = "EmailPublisher";

    public static final String FIRST_NAME_CLAIM = "http://wso2.org/claims/givenname";
    public static final String LAST_NAME_CLAIM = "http://wso2.org/claims/lastname";


    public static final String PLACEHOLDER_FIRST_NAME = "first_name";
    public static final String PLACEHOLDER_LAST_NAME = "last_name";
    public static final String PLACEHOLDER_DATE = "date";
    public static final String PLACEHOLDER_TIME = "time";
    public static final String PLACEHOLDER_TIMEZONE = "timezone";
    public static final String PLACEHOLDER_STATUS = "status";
    public static final String PLACEHOLDER_APPROVER = "approver";
    public static final String PLACEHOLDER_APP_NAME = "app_name";
    public static final String PLACEHOLDER_USER_NAME = "user_name";
    public static final String PLACEHOLDER_SERVER_URL = "server_url";
    public static final String PLACEHOLDER_ORG_NAME = "org_name";
    public static final String PLACEHOLDER_CONTACT_MAIL = "contact_email";
}
