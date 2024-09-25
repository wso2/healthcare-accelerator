/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.healthcare.apim.workflow.extensions;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.WorkflowResponse;
import org.wso2.carbon.apimgt.impl.dto.WorkflowDTO;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.notification.NotificationDTO;
import org.wso2.carbon.apimgt.impl.notification.NotifierConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.workflow.UserSignUpApprovalWorkflowExecutor;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.config.MailNotificationConfig;
import org.wso2.healthcare.apim.core.config.OrganizationConfig;
import org.wso2.healthcare.apim.core.notification.EmailNotifier;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.wso2.healthcare.apim.core.notification.EmailNotificationConstants.*;

/**
 * Extending the UserSignUpApprovalWorkflowExecutor to populate additional properties that could be shown to the
 * workflow approver.
 */
public class OpenHealthcareUserSignUpApprovalWorkflowExecutor extends UserSignUpApprovalWorkflowExecutor {

    private static final Log log = LogFactory.getLog(OpenHealthcareUserSignUpApprovalWorkflowExecutor.class);

    /**
     * Execute the User self sign up workflow approval process.
     * This method sets user profile attributes in the workflow object.
     */
    @Override
    public WorkflowResponse execute(WorkflowDTO workflowDTO) throws WorkflowException {
        String tenantDomain = workflowDTO.getTenantDomain();
        String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(workflowDTO.getWorkflowReference());
        try {

            RealmService realmService = ServiceReferenceHolder.getInstance().getRealmService();
            int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                    .getTenantId(tenantDomain);
            org.wso2.carbon.user.core.UserRealm realm = (UserRealm) realmService.getTenantUserRealm(tenantId);
            UserStoreManager manager = realm.getUserStoreManager();

            if (manager.isExistingUser(tenantAwareUserName)) {
                Claim[] claims = manager.getUserClaimValues(tenantAwareUserName, null);
                Arrays.stream(claims).forEach(claim -> workflowDTO.setProperties(claim.getDisplayTag(),
                        claim.getValue()));
            }

        } catch (UserStoreException use) {
            log.warn("Failed to retrieve user profile information of [workflow id] " +
                    workflowDTO.getWorkflowReference(), use);
        }

        WorkflowResponse workflowResponse = super.execute(workflowDTO);
        String firstName = workflowDTO.getProperties("First Name");
        String lastName = workflowDTO.getProperties("Last Name");
        try {
            if (StringUtils.isNotEmpty(firstName) && StringUtils.isNotEmpty(lastName)) {
                sendSignupRequestedNotification(tenantDomain, firstName, lastName);
            } else {
                throw new OpenHealthcareException("User details missing.");
            }
        } catch (OpenHealthcareException | UserStoreException | APIManagementException e) {
            log.error("Error sending user signup request notification - [Workflow ID] " +
                    workflowDTO.getExternalWorkflowReference(), e);
        }

        return workflowResponse;
    }

    @Override
    public WorkflowResponse complete(WorkflowDTO workflowDTO) throws WorkflowException {
        String tenantDomain = workflowDTO.getTenantDomain();
        String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(workflowDTO.getWorkflowReference());
        String firstName = "", lastName = "", email = "";
        String approver = CarbonContext.getThreadLocalCarbonContext().getUsername();
        RealmService realmService = ServiceReferenceHolder.getInstance().getRealmService();
        try {
            int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
            UserRealm realm = (UserRealm) realmService.getTenantUserRealm(tenantId);
            UserStoreManager manager = realm.getUserStoreManager();
            if (manager.isExistingUser(tenantAwareUserName)) {
                firstName = manager.getUserClaimValue(tenantAwareUserName, FIRST_NAME_CLAIM, null);
                lastName = manager.getUserClaimValue(tenantAwareUserName, LAST_NAME_CLAIM, null);
                email = manager.getUserClaimValue(tenantAwareUserName, NotifierConstants.EMAIL_CLAIM, null);
            }
        } catch (UserStoreException e) {
            log.error("Error retrieving user information for mail notification  - [Workflow ID] " +
                    workflowDTO.getExternalWorkflowReference(), e);
        }
        WorkflowResponse workflowResponse = super.complete(workflowDTO);
        try {
            if (StringUtils.isNotEmpty(firstName) && StringUtils.isNotEmpty(lastName) &&
                    StringUtils.isNotEmpty(approver)) {
                sendSignupCompletedNotification(workflowDTO.getTenantDomain(), workflowDTO.getStatus().toString(),
                        firstName, lastName, approver);
            } else {
                throw new OpenHealthcareException("User/approver details missing.");
            }
            if (StringUtils.isNotEmpty(email)) {
                sendSignupCompletedExternalNotification(workflowDTO.getTenantDomain(),
                        workflowDTO.getStatus().toString(), email);
            } else {
                throw new OpenHealthcareException("Recipient email missing.");
            }
        } catch (OpenHealthcareException | UserStoreException e) {
            log.error("Error sending signup completed mail notification - [Workflow ID] " +
                    workflowDTO.getExternalWorkflowReference(), e);
        }
        return workflowResponse;
    }

    private void sendSignupRequestedNotification(String tenantDomain, String firstName, String lastName)
            throws OpenHealthcareException, UserStoreException, APIManagementException {

        Map<String, MailNotificationConfig> mailNotificationConfigs =
                OpenHealthcareEnvironment.getInstance().getConfig().getNotificationMailConfig();
        if (mailNotificationConfigs.containsKey(NEW_USER_SIGNUP_REQUESTED_INTERNAL) && mailNotificationConfigs.get(
                NEW_USER_SIGNUP_REQUESTED_INTERNAL).isEnable()) {
            MailNotificationConfig mailConfig = mailNotificationConfigs.get(NEW_USER_SIGNUP_REQUESTED_INTERNAL);
            Properties recipientsProp = new Properties();
            recipientsProp.put(RECIPIENTS, (mailConfig.getRecipients() != null ? mailConfig.getRecipients() : ""));
            recipientsProp.put(RECIPIENT_ROLE, (mailConfig.getRecipientRoles() != null ?
                    mailConfig.getRecipientRoles() : ""));
            Map<String, String> placeHolderMap = new HashMap<>();
            placeHolderMap.put(PLACEHOLDER_FIRST_NAME, firstName);
            placeHolderMap.put(PLACEHOLDER_LAST_NAME, lastName);
            OrganizationConfig organizationConfig =
                    OpenHealthcareEnvironment.getInstance().getConfig().getOrganizationConfig();
            ZoneId zoneId = organizationConfig.getZoneId() != null ? organizationConfig.getZoneId() : ZoneId.of("GMT");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            placeHolderMap.put(PLACEHOLDER_DATE, ZonedDateTime.now(zoneId).toLocalDate().toString());
            placeHolderMap.put(PLACEHOLDER_TIME,
                    ZonedDateTime.now(zoneId).toLocalTime().format(timeFormatter));
            placeHolderMap.put(PLACEHOLDER_TIMEZONE, zoneId.toString());
            placeHolderMap.put(PLACEHOLDER_SERVER_URL, APIUtil.getServerURL());
            recipientsProp.put(PLACEHOLDERS, placeHolderMap);
            NotificationDTO notificationDTO = new NotificationDTO(recipientsProp, NEW_USER_SIGNUP_REQUESTED_INTERNAL);
            notificationDTO.setMessage(mailConfig.getEmailBody());
            notificationDTO.setTitle(mailConfig.getEmailSubject());
            int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                    .getTenantId(tenantDomain);
            notificationDTO.setTenantID(tenantId);
            notificationDTO.setTenantDomain(tenantDomain);
            new EmailNotifier().sendNotifications(notificationDTO);
        }
    }

    private void sendSignupCompletedNotification(String tenantDomain, String status, String firstName,
                                                 String lastName, String approver)
            throws OpenHealthcareException, UserStoreException {

        Map<String, MailNotificationConfig> mailNotificationConfigs =
                OpenHealthcareEnvironment.getInstance().getConfig().getNotificationMailConfig();
        if (mailNotificationConfigs.containsKey(NEW_USER_SIGNUP_COMPLETED_INTERNAL) && mailNotificationConfigs.get(
                NEW_USER_SIGNUP_COMPLETED_INTERNAL).isEnable()) {
            MailNotificationConfig mailConfig = mailNotificationConfigs.get(NEW_USER_SIGNUP_COMPLETED_INTERNAL);
            Properties recipientsProp = new Properties();
            recipientsProp.put(RECIPIENTS, (mailConfig.getRecipients() != null ? mailConfig.getRecipients() : ""));
            recipientsProp.put(RECIPIENT_ROLE, (mailConfig.getRecipientRoles() != null ?
                    mailConfig.getRecipientRoles() : ""));
            Map<String, String> placeHolderMap = new HashMap<>();
            placeHolderMap.put(PLACEHOLDER_FIRST_NAME, firstName);
            placeHolderMap.put(PLACEHOLDER_LAST_NAME, lastName);
            placeHolderMap.put(PLACEHOLDER_STATUS, status.toLowerCase());
            placeHolderMap.put(PLACEHOLDER_APPROVER, approver);
            recipientsProp.put(PLACEHOLDERS, placeHolderMap);
            NotificationDTO notificationDTO = new NotificationDTO(recipientsProp, NEW_USER_SIGNUP_COMPLETED_INTERNAL);
            notificationDTO.setMessage(mailConfig.getEmailBody());
            notificationDTO.setTitle(mailConfig.getEmailSubject());
            int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                    .getTenantId(tenantDomain);
            notificationDTO.setTenantID(tenantId);
            notificationDTO.setTenantDomain(tenantDomain);
            new EmailNotifier().sendNotifications(notificationDTO);
        }
    }

    private void sendSignupCompletedExternalNotification(String tenantDomain, String status, String email)
            throws OpenHealthcareException, UserStoreException {

        Map<String, MailNotificationConfig> mailNotificationConfigs =
                OpenHealthcareEnvironment.getInstance().getConfig().getNotificationMailConfig();
        if (mailNotificationConfigs.containsKey(NEW_USER_SIGNUP_COMPLETED_EXTERNAL) && mailNotificationConfigs.get(
                NEW_USER_SIGNUP_COMPLETED_EXTERNAL).isEnable()) {
            MailNotificationConfig mailConfig = mailNotificationConfigs.get(NEW_USER_SIGNUP_COMPLETED_EXTERNAL);
            Properties recipientsProp = new Properties();
            recipientsProp.put(RECIPIENTS, email);
            Map<String, String> placeHolderMap = new HashMap<>();
            placeHolderMap.put(PLACEHOLDER_STATUS, status.toLowerCase());
            OrganizationConfig organizationConfig =
                    OpenHealthcareEnvironment.getInstance().getConfig().getOrganizationConfig();
            if (organizationConfig.getOrgName() != null && !organizationConfig.getOrgName().isEmpty()) {
                placeHolderMap.put(PLACEHOLDER_ORG_NAME, organizationConfig.getOrgName());
            }
            if (organizationConfig.getContactEmail() != null && !organizationConfig.getContactEmail().isEmpty()) {
                placeHolderMap.put(PLACEHOLDER_CONTACT_MAIL, organizationConfig.getContactEmail());
            }
            recipientsProp.put(PLACEHOLDERS, placeHolderMap);
            NotificationDTO notificationDTO = new NotificationDTO(recipientsProp, NEW_USER_SIGNUP_COMPLETED_EXTERNAL);
            notificationDTO.setMessage(mailConfig.getEmailBody());
            notificationDTO.setTitle(mailConfig.getEmailSubject());
            int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                    .getTenantId(tenantDomain);
            notificationDTO.setTenantID(tenantId);
            notificationDTO.setTenantDomain(tenantDomain);
            new EmailNotifier().sendNotifications(notificationDTO);
        }
    }
}
