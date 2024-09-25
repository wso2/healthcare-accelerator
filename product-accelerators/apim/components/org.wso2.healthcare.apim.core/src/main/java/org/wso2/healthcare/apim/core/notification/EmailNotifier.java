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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.notification.NotificationDTO;
import org.wso2.carbon.apimgt.impl.notification.Notifier;
import org.wso2.carbon.apimgt.impl.notification.NotifierConstants;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wso2.healthcare.apim.core.notification.EmailNotificationConstants.EMAIL_ADAPTER_SERVICE_NAME;
import static org.wso2.healthcare.apim.core.notification.EmailNotificationConstants.PLACEHOLDERS;
import static org.wso2.healthcare.apim.core.notification.EmailNotificationConstants.RECIPIENTS;
import static org.wso2.healthcare.apim.core.notification.EmailNotificationConstants.RECIPIENT_ROLE;

/**
 * A generic class for sending mail notifications.
 */
public class EmailNotifier extends Notifier {
    private static final Log log = LogFactory.getLog(EmailNotifier.class);

    protected String retrieveRecipients(NotificationDTO notificationDTO) throws UserStoreException {
        Properties recipientProp = notificationDTO.getProperties();
        String recipients = recipientProp.getProperty(RECIPIENTS);
        String recipientRoles = recipientProp.getProperty(RECIPIENT_ROLE);
        if (recipientRoles != null && !recipientRoles.isEmpty()) {
            List<String> roles = new ArrayList<>(Arrays.asList(recipientRoles.split(",")));
            RealmService realmService = ServiceReferenceHolder.getInstance().getRealmService();
            int tenantId = realmService.getTenantManager().getTenantId(notificationDTO.getTenantDomain());
            UserRealm realm = (UserRealm) realmService.getTenantUserRealm(tenantId);
            UserStoreManager manager = realm.getUserStoreManager();
            for (String role : roles) {
                String[] users = manager.getUserListOfRole(role);
                if (users.length > 0) {
                    StringBuilder recipientBuilder = new StringBuilder(recipients);
                    for (String user : users) {
                        String mailId = manager.getUserClaimValue(user, NotifierConstants.EMAIL_CLAIM, null);
                        if (mailId != null && !mailId.isEmpty()) {
                            recipientBuilder = recipientBuilder.append(",").append(mailId);
                        }
                    }
                    recipients = recipientBuilder.toString();
                }
            }
        }
        return (recipients.startsWith(",") ? recipients.substring(1) : recipients);
    }

    protected String resolveTemplate(String templatedString, Map<String, String> placeholders,
                                     String notificationType) {
        if (templatedString != null) {
            Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
            Matcher matcher = pattern.matcher(templatedString);
            while (matcher.find()) {
                String s = matcher.group(1);
                try {
                    templatedString = templatedString.replaceAll("\\$\\{" + s + "}", placeholders.get(s));
                } catch (NullPointerException ne) {
                    log.error("Cannot retrieve value of [" + s + "] for email notification [" +
                            notificationType + "]", ne);
                }
            }
        }
        return templatedString;
    }

    @Override
    public void sendNotifications(NotificationDTO notificationDTO) {
        Properties notificationProperties = notificationDTO.getProperties();
        Map<String, String> placeholders = (Map<String, String>) notificationProperties.get(PLACEHOLDERS);
        String message = null, subject = null, recipients = null;
        if (placeholders != null) {
            message = resolveTemplate(notificationDTO.getMessage(), placeholders, notificationDTO.getType());
            subject = resolveTemplate(notificationDTO.getTitle(), placeholders, notificationDTO.getType());
        }
        try {
            recipients = retrieveRecipients(notificationDTO);
        } catch (UserStoreException use) {
            log.error("Error retrieving recipients for mail notification - " + notificationDTO.getType(), use);
        }
        Map<String, String> emailProperties = new HashMap<>();
        emailProperties.put(NotifierConstants.EMAIL_ADDRESS_KEY, recipients);
        emailProperties.put(NotifierConstants.EMAIL_SUBJECT_KEY, subject);
        emailProperties.put(NotifierConstants.EMAIL_TYPE_KEY, NotifierConstants.EMAIL_FORMAT_HTML);
        ServiceReferenceHolder.getInstance().getOutputEventAdapterService().publish(EMAIL_ADAPTER_SERVICE_NAME,
                emailProperties, message);
        if (log.isDebugEnabled()) {
            log.debug("[" + notificationDTO.getType() + "] Notification sent to Email Adapter ");
        }
    }
}
