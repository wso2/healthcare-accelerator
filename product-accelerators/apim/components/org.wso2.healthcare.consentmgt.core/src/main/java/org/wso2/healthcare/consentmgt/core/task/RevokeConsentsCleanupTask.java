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
package org.wso2.healthcare.consentmgt.core.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.consent.mgt.core.exception.ConsentManagementException;
import org.wso2.carbon.consent.mgt.core.model.ReceiptListResponse;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static org.wso2.healthcare.consentmgt.core.util.ServiceUtil.getConsentManager;

public class RevokeConsentsCleanupTask {

    private static final Log log = LogFactory.getLog(RevokeConsentsCleanupTask.class);

    public void execute() {

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(runnable);
                t.setName("Revoked Consents Cleanup Task");
                return t;
            }
        });

        //TODO introduce config to change the frequency
        executor.scheduleAtFixedRate(new CleanupTask(), 120, 60, TimeUnit.SECONDS);
    }

    private static class CleanupTask implements Runnable {

        public void run() {
            try {
                List<org.wso2.carbon.consent.mgt.core.model.ReceiptListResponse> revokedReceipts = getConsentManager()
                        .searchReceipts(0, 0, "*", null, null, "REVOKED");
                for (ReceiptListResponse revokedReceipt : revokedReceipts) {
                    getConsentManager().deleteReceipt(revokedReceipt.getConsentReceiptId());
                }
            } catch (ConsentManagementException e) {
                log.error("error occurred while deleting revoked receipts.", e);
            }
        }
    }

}
