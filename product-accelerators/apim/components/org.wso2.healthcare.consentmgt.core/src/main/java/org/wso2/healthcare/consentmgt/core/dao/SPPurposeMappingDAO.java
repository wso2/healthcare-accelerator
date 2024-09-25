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

package org.wso2.healthcare.consentmgt.core.dao;

import org.wso2.healthcare.consentmgt.core.exception.FHIRConsentMgtException;
import org.wso2.healthcare.consentmgt.core.model.SPApplicationListResponse;
import org.wso2.healthcare.consentmgt.core.model.SPPurposeMapping;

import java.util.List;

/**
 * Perform CRUD operations for {@link SPPurposeMapping}.
 *
 * @since v1.0
 */
public interface SPPurposeMappingDAO {

    /**
     * Adds a {@link SPPurposeMapping}.
     *
     * @param spPurposeMapping {@link SPPurposeMapping} to insert.
     * @return Added {@link SPPurposeMapping}.
     * @throws FHIRConsentMgtException If error occurs while adding {@link SPPurposeMapping}.
     */
    SPPurposeMapping addSPPurpose(SPPurposeMapping spPurposeMapping) throws FHIRConsentMgtException;

    /**
     * Updates a {@link SPPurposeMapping}.
     *
     * @param spPurposeMapping {@link SPPurposeMapping} to update.
     * @return Updated {@link SPPurposeMapping}.
     * @throws FHIRConsentMgtException If error occurs while updating {@link SPPurposeMapping}.
     */
    SPPurposeMapping updateSPPurpose(SPPurposeMapping spPurposeMapping) throws FHIRConsentMgtException;

    /**
     * Returns a {@link SPPurposeMapping} for the given id.
     *
     * @param id ID of the {@link SPPurposeMapping} to retrieve.
     * @return {@link SPPurposeMapping} for the given id.
     * @throws FHIRConsentMgtException If error occurs while retrieving {@link SPPurposeMapping}.
     */
    SPPurposeMapping getSPPurposesById(Integer id) throws FHIRConsentMgtException;

    /**
     * Deletes a {@link SPPurposeMapping} from the id.
     *
     * @param id ID of the {@link SPPurposeMapping} to delete.
     * @throws FHIRConsentMgtException If error occurs while deleting {@link SPPurposeMapping}.
     */
    void deleteSPPurposesById(Integer id) throws FHIRConsentMgtException;

    /**
     * Retrieves a List of {@link SPPurposeMapping} for a given consumer key.
     *
     * @param consumerKey Consumer key value of the app.
     * @return List of {@link SPPurposeMapping}.
     * @throws FHIRConsentMgtException If error occurs while retrieving List of {@link SPPurposeMapping}.
     */
    List<SPPurposeMapping> getSPPurposesByConsumerKey(String consumerKey) throws FHIRConsentMgtException;

    /**
     * Retrieves a List of {@link SPPurposeMapping} for a given consent purpose id.
     *
     * @param purposeId ID od the consent purpose.
     * @return List of {@link SPPurposeMapping}.
     * @throws FHIRConsentMgtException If error occurs while retrieving List of {@link SPPurposeMapping}.
     */
    List<SPPurposeMapping> getSPPurposesByPurposeId(Integer purposeId) throws FHIRConsentMgtException;

    /**
     * Retrieves a List of {@link SPPurposeMapping} for a given app name.
     *
     * @param appName Consumer app name.
     * @return List of {@link SPPurposeMapping}.
     * @throws FHIRConsentMgtException If error occurs while retrieving List of {@link SPPurposeMapping}.
     */
    List<SPPurposeMapping> getSPPurposesByAppName(String appName) throws FHIRConsentMgtException;

    /**
     * Search {@link SPPurposeMapping} for the given parameters.
     *
     * @param purposeId       Consent purpose id.
     * @param consumerKey     Consumer app key.
     * @param limit           Maximum number of results expected.
     * @param offset          Result offset.
     * @param applicationName Consumer app name.
     * @return List of {@link SPPurposeMapping}.
     * @throws FHIRConsentMgtException If error occurs while retrieving list of {@link SPPurposeMapping}.
     */
    List<SPPurposeMapping> searchSPPurposes(Integer purposeId, String consumerKey, Integer limit, Integer offset,
            String applicationName) throws FHIRConsentMgtException;

    /**
     * Gets list of consumer applications in a {@link SPApplicationListResponse}.
     *
     * @param limit       Maximum number of results expected.
     * @param offset      Result offset.
     * @param consumerKey Consumer app key.
     * @param appName     Consumer app name.
     * @return {@link SPApplicationListResponse}.
     * @throws FHIRConsentMgtException If error occurs while retrieving consumer applications.
     */
    SPApplicationListResponse getApplications(Integer limit, Integer offset, String consumerKey, String appName)
            throws FHIRConsentMgtException;
}
