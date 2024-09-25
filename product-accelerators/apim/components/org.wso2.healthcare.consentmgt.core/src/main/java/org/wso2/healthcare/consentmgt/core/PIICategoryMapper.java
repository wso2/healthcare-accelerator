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
import org.wso2.healthcare.consentmgt.core.model.PIICategoryMapping;

import java.util.List;

public interface PIICategoryMapper {

    /**
     * This method is used to add pii category to consent purpose mapping
     *
     * @param mapping PIICategoryMapping
     * @return added PIICategoryMapping object
     * @throws FHIRConsentMgtException
     */
    PIICategoryMapping addPIICategoryMapping(PIICategoryMapping mapping) throws FHIRConsentMgtException;

    /**
     * This method is used to get piicatefory mapping from mapping id
     *
     * @param id mapping id
     * @return PIICategoryMapping object
     * @throws FHIRConsentMgtException
     */
    PIICategoryMapping getPIICategoryFromId(Integer id) throws FHIRConsentMgtException;

    /**
     * This method is used to get pii category mapping from the category id
     *
     * @param categoryId pii category id
     * @return PIICategoryMapping object
     * @throws FHIRConsentMgtException
     */
    PIICategoryMapping getPIICategoryFromCategoryId(Integer categoryId) throws FHIRConsentMgtException;

    /**
     * This method is used to search pii category mappings
     *
     * @param limit      limit of the search results
     * @param offset     offset of the search results
     * @param categoryId pii category id
     * @param type       category type
     * @return list of PIICategoryMapping objects
     * @throws FHIRConsentMgtException
     */
    List<PIICategoryMapping> searchPIICategoryMapping(Integer limit, Integer offset, Integer categoryId, String type)
            throws FHIRConsentMgtException;
}
