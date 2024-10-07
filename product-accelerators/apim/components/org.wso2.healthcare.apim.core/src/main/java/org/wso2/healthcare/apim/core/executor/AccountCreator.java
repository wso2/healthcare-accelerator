///*
// * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
// *
// * WSO2 Inc. licenses this file to you under the Apache License,
// * Version 2.0 (the "License"); you may not use this file except
// * in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied. See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//
//package org.wso2.healthcare.apim.core.executor;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.wso2.carbon.context.PrivilegedCarbonContext;
//import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
//import org.wso2.healthcare.apim.core.OpenHealthcareException;
//import org.wso2.healthcare.apim.core.account.saas.SaaSAccountManager;
//import org.wso2.healthcare.apim.core.alert.AlertCategory;
//import org.wso2.healthcare.apim.core.alert.AlertManager;
//import org.wso2.healthcare.apim.core.dao.RegistrationDAO;
//import org.wso2.healthcare.apim.core.model.account.Account;
//import org.wso2.healthcare.apim.core.model.account.Callback;
//import org.wso2.healthcare.apim.core.workflow.Workflow;
//import org.wso2.healthcare.apim.core.workflow.WorkflowConstants;
//import org.wso2.healthcare.apim.core.workflow.WorkflowData;
//
///**
// * Runnable implementation that creates an account
// */
//public class AccountCreator implements Runnable {
//
//    private static final Log LOG = LogFactory.getLog(AccountCreator.class);
//    private final Account account;
//    private final String tenantDomain;
//    private final int tenantId;
//    private final Callback callback;
//
//    /**
//     * Constructor
//     *
//     * @param account account details
//     * @param callback callback information
//     */
//    public AccountCreator(Account account, Callback callback) throws OpenHealthcareException {
//        if (account == null || callback == null) {
//            // Account and callback is mandatory
//            throw new OpenHealthcareException("Account and callback cannot be null");
//        }
//        this.account = account;
//        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
//        this.tenantDomain = carbonContext.getTenantDomain();
//        this.tenantId = carbonContext.getTenantId();
//        this.callback = callback;
//    }
//
//    @Override
//    public void run() {
//        //Populate tenant information
//        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
//        carbonContext.setTenantId(tenantId);
//        carbonContext.setTenantDomain(tenantDomain);
//
//        try {
//            SaaSAccountManager.getInstance().createAccount(account);
//        } catch (OpenHealthcareException e) {
//            LOG.error("Error occurred while provisioning the account : " + account, e);
//        }
//
//        // Trigger account creation completion workflow
//        try {
//            if (callback.getType() == Callback.CallbackTypeEnum.BPMN) {
//                Workflow workflow =
//                        OpenHealthcareEnvironment.getInstance().getWorkflowHolder().get(WorkflowConstants.WF_ACCOUNT_CREATION_COMPLETE);
//                if (workflow != null) {
//                    WorkflowData workflowData = new WorkflowData();
//                    workflowData.put(WorkflowConstants.WF_DATA_ACCOUNT, account);
//                    workflowData.put(WorkflowConstants.WF_DATA_CALLBACK, callback);
//                    workflow.execute(workflowData);
//                }
//            } else {
//                LOG.warn("Unsupported callback type. Hence callback is not triggered for created account");
//            }
//        } catch (OpenHealthcareException e) {
//            String msg = "Error occurred while triggering account creation completion workflow.";
//            AlertManager.doAlertAndEscalate(msg, AlertCategory.ACCOUNT_CREATION_NOTIFICATION_FAILURE, account, e);
//        }
//
//        // Delete the registration entry
//        try {
//            RegistrationDAO.deleteRegistrationByOrg(account.getOrganization());
//        } catch (OpenHealthcareException e) {
//            LOG.error("Error occurred while deleting registration entry related to : " + account, e);
//        }
//    }
//}
