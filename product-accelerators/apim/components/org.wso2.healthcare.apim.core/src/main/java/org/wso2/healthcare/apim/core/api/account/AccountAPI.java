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
//package org.wso2.healthcare.apim.core.api.account;
//
//import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
//import org.wso2.healthcare.apim.core.OpenHealthcareException;
//import org.wso2.healthcare.apim.core.account.saas.SaaSAccountManager;
//import org.wso2.healthcare.apim.core.executor.AccountCreator;
//import org.wso2.healthcare.apim.core.model.account.Account;
//import org.wso2.healthcare.apim.core.model.account.Callback;
//
///**
// * Internal java Account API
// */
//public class AccountAPI {
//
//    /**
//     * Reserve and account in the platform by creating and entry
//     * NOTE : This is will not provision the account (entry is added for tracking), it just reserve an account since
//     *        it's created asynchronously.
//     *
//     * @param account
//     * @throws OpenHealthcareException
//     */
//    public static void reserveAccount(Account account) throws OpenHealthcareException {
//        SaaSAccountManager.getInstance().reserveAccount(account);
//    }
//
//    /**
//     * API function to create (provision) Account. Actual Account creation process will be triggered and
//     * asynchronously account get created
//     *
//     * @param account Account details to create
//     * @param callback callback details to notify once account is created
//     * @throws OpenHealthcareException
//     */
//    public static void createAccount(Account account, Callback callback) throws OpenHealthcareException {
//        AccountCreator accountCreator = new AccountCreator(account, callback);
//        OpenHealthcareEnvironment.getInstance().getAsyncExecutor().executeAsync(accountCreator);
//    }
//
//}
