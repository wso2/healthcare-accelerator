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

package org.wso2.healthcare.consentmgt.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.consent.mgt.core.ConsentManager;
import org.wso2.carbon.consent.mgt.core.exception.ConsentManagementException;
import org.wso2.carbon.consent.mgt.core.model.PIICategory;
import org.wso2.carbon.consent.mgt.core.model.Purpose;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.util.List;

/**
 * Utility class for using the identity consent management features.
 */
public class ConsentMgtUtils {

    private static final Log log = LogFactory
            .getLog(org.wso2.healthcare.consentmgt.core.util.ConsentMgtUtils.class);

    /**
     * Retrieves consent management OSGi service.
     *
     * @return {@link ConsentManager} service instance
     */
    public static ConsentManager getConsentManager() {

        return (ConsentManager) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getOSGiService(ConsentManager.class, null);
    }

    /**
     * Adds a pii category.
     *
     * @param piiCategory {@link PIICategory} used to populate the PII information
     * @return added PII category
     */
    public static PIICategory addPIICategory(PIICategory piiCategory) {
        try {
            return getConsentManager().addPIICategory(piiCategory);
        } catch (ConsentManagementException e) {
            log.error("Error occurred while adding PII categories.", e);
        }
        return null;
    }

    /**
     * Fetches all PII categories.
     *
     * @return List of {@link PIICategory}
     */
    public static List<PIICategory> listPIICategories() {
        try {
            return getConsentManager().listPIICategories(0, 0);
        } catch (ConsentManagementException e) {
            log.error("Error occurred while fetching PII categories.", e);
        }
        return null;
    }

    /**
     * Adds consent purpose to the OH platform.
     *
     * @param purpose {@link Purpose} used to represent consent purpose(policy)
     * @return Added consent purpose
     */
    public static Purpose addPurpose(Purpose purpose) {
        try {
            return getConsentManager().addPurpose(purpose);
        } catch (ConsentManagementException e) {
            log.error("Error occurred while adding consent purpose.", e);
        }
        return null;
    }

    /**
     * Fetches all the consent purposes.
     *
     * @return List of {@link Purpose}
     */
    public static List<Purpose> listPurposes() {
        try {
            return getConsentManager().listPurposes(0, 0);
        } catch (ConsentManagementException e) {
            log.error("Error occurred while fetching consent purposes.", e);
        }
        return null;
    }
}
