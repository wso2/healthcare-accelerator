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

package org.wso2.healthcare.consentmgt.core;

import org.wso2.healthcare.consentmgt.core.exception.FHIRConsentMgtException;
import org.wso2.healthcare.consentmgt.core.model.SPApplicationListResponse;
import org.wso2.healthcare.consentmgt.core.model.SPPurposeMapping;

import java.util.List;

//todo class comments
public interface ConsentPurposeMapper {

    /**
     * This is used to register a SP app to consent purpose mapping
     *
     * @param spPurposeMapping SPPurposeMapping
     * @return added SPPurposeMapping object
     * @throws FHIRConsentMgtException
     */
    SPPurposeMapping addSPPurpose(SPPurposeMapping spPurposeMapping) throws FHIRConsentMgtException;

    /**
     * This is used to update a SP app to consent purpose mapping
     *
     * @param spPurposeMapping SPPurposeMapping
     * @return updated SPPurposeMapping object
     * @throws FHIRConsentMgtException
     */
    SPPurposeMapping updateSPPurpose(SPPurposeMapping spPurposeMapping) throws FHIRConsentMgtException;

    /**
     * This method is used to get a SP app to consent purpose mapping by the mapping id
     *
     * @param id mapping id
     * @return SPPurposeMapping object
     * @throws FHIRConsentMgtException
     */
    SPPurposeMapping getSPPurposesById(Integer id) throws FHIRConsentMgtException;

    /**
     * This method is used to delete a SP app to consent purpose mapping by it's id
     *
     * @param id mapping id
     * @throws FHIRConsentMgtException
     */
    void deleteSPPurposeById(Integer id) throws FHIRConsentMgtException;

    /**
     * This method is used to retrieve a SP purpose to app mapping by the app consumer key
     *
     * @param consumerKey app consumer key
     * @return list of SPPurposeMapping objects
     * @throws FHIRConsentMgtException
     */
    List<SPPurposeMapping> getSPPurposesByAppId(String consumerKey) throws FHIRConsentMgtException;

    /**
     * This method is used to retrieve a SP purpose to app mapping by the app name
     *
     * @param appName application name
     * @return list of SPPurposeMapping objects
     * @throws FHIRConsentMgtException
     */
    List<SPPurposeMapping> getSPPurposesByAppName(String appName) throws FHIRConsentMgtException;

    /**
     * This method is used to retrieve a SP purpose to app mapping by the purpose id
     *
     * @param purposeId consent purpose id
     * @return list of SPPurposeMapping
     * @throws FHIRConsentMgtException
     */
    List<SPPurposeMapping> getSPPurposesByPurposeId(Integer purposeId) throws FHIRConsentMgtException;

    /**
     * This method is used to search a SP to app purposes mapping
     *
     * @param limit           limit of the search results
     * @param offset          offset of the search results
     * @param consumerKey     app consumer key
     * @param purposeId       consent purpose id
     * @param applicationName SP app name
     * @return list of SPPurposeMapping
     * @throws FHIRConsentMgtException
     */
    List<SPPurposeMapping> searchSPPurposes(Integer limit, Integer offset, String consumerKey, Integer purposeId,
            String applicationName) throws FHIRConsentMgtException;

    /**
     * This method is used to search SP app
     *
     * @param limit       limit of the search results
     * @param offset      offset of the search results
     * @param consumerKey app consumer key
     * @param appName     SP app name
     * @return list of SP applications
     * @throws FHIRConsentMgtException
     */
    SPApplicationListResponse getApplications(Integer limit, Integer offset, String consumerKey, String appName)
            throws FHIRConsentMgtException;
}
