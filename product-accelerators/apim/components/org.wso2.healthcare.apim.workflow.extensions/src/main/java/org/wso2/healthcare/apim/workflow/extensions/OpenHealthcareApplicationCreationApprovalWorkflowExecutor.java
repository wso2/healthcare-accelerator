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

package org.wso2.healthcare.apim.workflow.extensions;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.WorkflowResponse;
import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.ApplicationWorkflowDTO;
import org.wso2.carbon.apimgt.impl.dto.WorkflowDTO;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.notification.NotificationDTO;
import org.wso2.carbon.apimgt.impl.notification.NotifierConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.workflow.ApplicationCreationApprovalWorkflowExecutor;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowException;
import org.wso2.carbon.context.CarbonContext;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.wso2.healthcare.apim.core.notification.EmailNotificationConstants.*;

/**
 * Extending the ApplicationCreationApprovalWorkflowExecutor to populate additional properties that could be shown to
 * the workflow approver.
 */
public class OpenHealthcareApplicationCreationApprovalWorkflowExecutor extends
        ApplicationCreationApprovalWorkflowExecutor {

    private static final Log log = LogFactory.getLog(OpenHealthcareApplicationCreationApprovalWorkflowExecutor.class);

    @Override
    public WorkflowResponse execute(WorkflowDTO workflowDTO) throws WorkflowException {

        WorkflowResponse workflowResponse = null;
        if (workflowDTO != null) {
            ApplicationWorkflowDTO applicationWorkflowDTO = (ApplicationWorkflowDTO) workflowDTO;
            Application application = applicationWorkflowDTO.getApplication();
            application.getApplicationAttributes().forEach(applicationWorkflowDTO::setProperties);
            applicationWorkflowDTO.setProperties("description", application.getDescription());
            if (log.isDebugEnabled()) {
                log.debug("Workflow approval data updated for the application : " + application.getName());
            }
            workflowResponse = super.execute(applicationWorkflowDTO);
            try {
                sendAppCreationRequestedNotification(applicationWorkflowDTO.getTenantDomain(),
                        applicationWorkflowDTO.getApplication().getName(),
                        applicationWorkflowDTO.getUserName());
            } catch (OpenHealthcareException | UserStoreException | APIManagementException e) {
                log.error("Error sending app creation request mail notification - [Workflow ID] " +
                        workflowDTO.getExternalWorkflowReference(), e);
            }
        }
        return workflowResponse;
    }

    @Override
    public WorkflowResponse complete(WorkflowDTO workflowDTO) throws WorkflowException {
        String tenantDomain = workflowDTO.getTenantDomain();
        String email = "";
        String approver = CarbonContext.getThreadLocalCarbonContext().getUsername();
        ApiMgtDAO dao = ApiMgtDAO.getInstance();
        Application application = null;
        try {
            if (dao.getApplicationById(Integer.parseInt(workflowDTO.getWorkflowReference())) != null) {
                application = dao.getApplicationById(Integer.parseInt(workflowDTO.getWorkflowReference()));
                String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(application.getOwner());
                RealmService realmService = ServiceReferenceHolder.getInstance().getRealmService();
                int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
                UserRealm realm = (UserRealm) realmService.getTenantUserRealm(tenantId);
                UserStoreManager manager = realm.getUserStoreManager();
                if (manager.isExistingUser(tenantAwareUserName)) {
                    email = manager.getUserClaimValue(tenantAwareUserName, NotifierConstants.EMAIL_CLAIM, null);
                }
            }
        } catch (UserStoreException | APIManagementException e) {
            log.error("Error retrieving application owner information for notification - [Workflow ID] " +
                    workflowDTO.getExternalWorkflowReference(), e);
        }
        WorkflowResponse workflowResponse = super.complete(workflowDTO);
        try {
            if (application != null && StringUtils.isNotEmpty(approver)) {
                sendAppCreationCompletedNotification(tenantDomain, application.getName(), application.getOwner(),
                        workflowDTO.getStatus().toString(), approver);
                if (StringUtils.isNotEmpty(email)) {
                    sendAppCreationCompletedExternalNotification(tenantDomain, email, application.getName(),
                            workflowDTO.getStatus().toString());
                } else {
                    throw new OpenHealthcareException("Recipient email missing.");
                }
            } else {
                throw new OpenHealthcareException("Cannot find application details.");
            }
        } catch (OpenHealthcareException | UserStoreException e) {
            log.error("Error sending app completed mail notification - [Workflow ID] " +
                    workflowDTO.getExternalWorkflowReference(), e);
        }
        return workflowResponse;
    }

    private void sendAppCreationRequestedNotification(String tenantDomain, String appName, String userName)
            throws OpenHealthcareException, UserStoreException, APIManagementException {

        Map<String, MailNotificationConfig> mailNotificationConfigs =
                OpenHealthcareEnvironment.getInstance().getConfig().getNotificationMailConfig();
        if (mailNotificationConfigs.containsKey(NEW_APP_CREATION_REQUESTED_INTERNAL) && mailNotificationConfigs.get(
                NEW_APP_CREATION_REQUESTED_INTERNAL).isEnable()) {
            MailNotificationConfig mailConfig = mailNotificationConfigs.get(NEW_APP_CREATION_REQUESTED_INTERNAL);
            Properties recipientsProp = new Properties();
            recipientsProp.put(RECIPIENTS, (mailConfig.getRecipients() != null ? mailConfig.getRecipients() : ""));
            recipientsProp.put(RECIPIENT_ROLE, (mailConfig.getRecipientRoles() != null ?
                    mailConfig.getRecipientRoles() : ""));
            Map<String, String> placeHolderMap = new HashMap<>();
            placeHolderMap.put(PLACEHOLDER_APP_NAME, appName);
            placeHolderMap.put(PLACEHOLDER_USER_NAME, userName);
            placeHolderMap.put(PLACEHOLDER_SERVER_URL, APIUtil.getServerURL());
            recipientsProp.put(PLACEHOLDERS, placeHolderMap);
            NotificationDTO notificationDTO = new NotificationDTO(recipientsProp, NEW_APP_CREATION_REQUESTED_INTERNAL);
            notificationDTO.setMessage(mailConfig.getEmailBody());
            notificationDTO.setTitle(mailConfig.getEmailSubject());
            int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                    .getTenantId(tenantDomain);
            notificationDTO.setTenantID(tenantId);
            notificationDTO.setTenantDomain(tenantDomain);
            new EmailNotifier().sendNotifications(notificationDTO);
        }
    }

    private void sendAppCreationCompletedNotification(String tenantDomain, String appName, String userName,
                                                      String status, String approver)
            throws OpenHealthcareException, UserStoreException {

        Map<String, MailNotificationConfig> mailNotificationConfigs =
                OpenHealthcareEnvironment.getInstance().getConfig().getNotificationMailConfig();
        if (mailNotificationConfigs.containsKey(NEW_APP_CREATION_COMPLETED_INTERNAL) && mailNotificationConfigs.get(
                NEW_APP_CREATION_COMPLETED_INTERNAL).isEnable()) {
            MailNotificationConfig mailConfig = mailNotificationConfigs.get(NEW_APP_CREATION_COMPLETED_INTERNAL);
            Properties recipientsProp = new Properties();
            recipientsProp.put(RECIPIENTS, (mailConfig.getRecipients() != null ? mailConfig.getRecipients() : ""));
            recipientsProp.put(RECIPIENT_ROLE, (mailConfig.getRecipientRoles() != null ?
                    mailConfig.getRecipientRoles() : ""));
            Map<String, String> placeHolderMap = new HashMap<>();
            placeHolderMap.put(PLACEHOLDER_APP_NAME, appName);
            placeHolderMap.put(PLACEHOLDER_USER_NAME, userName);
            placeHolderMap.put(PLACEHOLDER_STATUS, status.toLowerCase());
            placeHolderMap.put(PLACEHOLDER_APPROVER, approver);
            recipientsProp.put(PLACEHOLDERS, placeHolderMap);
            NotificationDTO notificationDTO = new NotificationDTO(recipientsProp, NEW_APP_CREATION_COMPLETED_INTERNAL);
            notificationDTO.setMessage(mailConfig.getEmailBody());
            notificationDTO.setTitle(mailConfig.getEmailSubject());
            int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                    .getTenantId(tenantDomain);
            notificationDTO.setTenantID(tenantId);
            notificationDTO.setTenantDomain(tenantDomain);
            new EmailNotifier().sendNotifications(notificationDTO);
        }
    }

    private void sendAppCreationCompletedExternalNotification(String tenantDomain, String email, String appName,
                                                              String status)
            throws OpenHealthcareException, UserStoreException {

        Map<String, MailNotificationConfig> mailNotificationConfigs =
                OpenHealthcareEnvironment.getInstance().getConfig().getNotificationMailConfig();
        if (mailNotificationConfigs.containsKey(NEW_APP_CREATION_COMPLETED_EXTERNAL) && mailNotificationConfigs.get(
                NEW_APP_CREATION_COMPLETED_EXTERNAL).isEnable()) {
            MailNotificationConfig mailConfig = mailNotificationConfigs.get(NEW_APP_CREATION_COMPLETED_EXTERNAL);
            Properties recipientsProp = new Properties();
            recipientsProp.put(RECIPIENTS, email);
            Map<String, String> placeHolderMap = new HashMap<>();
            placeHolderMap.put(PLACEHOLDER_APP_NAME, appName);
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
            NotificationDTO notificationDTO = new NotificationDTO(recipientsProp, NEW_APP_CREATION_COMPLETED_EXTERNAL);
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
