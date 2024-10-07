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
import org.wso2.healthcare.consentmgt.core.model.PIICategoryMapping;

import java.util.List;

/**
 * Perform CRUD operations for {@link PIICategoryMapping}.
 *
 * @since v1.0
 */
public interface ConsentPIICategoryMappingDAO {

    /**
     * Adds a {@link PIICategoryMapping}.
     *
     * @param mapping {@link PIICategoryMapping} data object.
     * @return Added {@link PIICategoryMapping}.
     * @throws FHIRConsentMgtException If error occurs while adding {@link PIICategoryMapping}.
     */
    PIICategoryMapping addPIICategoryMapping(PIICategoryMapping mapping) throws FHIRConsentMgtException;

    /**
     * Retrieves a {@link PIICategoryMapping} from the mapping id given.
     *
     * @param id Category mapping id of {@link PIICategoryMapping}.
     * @return {@link PIICategoryMapping}.
     * @throws FHIRConsentMgtException If error occurs while retrieving {@link PIICategoryMapping}
     */
    PIICategoryMapping getPIICategoryMappingFromId(Integer id) throws FHIRConsentMgtException;

    /**
     * Returns list of {@link PIICategoryMapping} according to the search parameters.
     *
     * @param limit      Maximum number of results expected.
     * @param offset     Result offset.
     * @param categoryId Id of the {@link PIICategoryMapping}.
     * @param type       Type of the {@link PIICategoryMapping}.
     * @return list of {@link PIICategoryMapping}
     * @throws FHIRConsentMgtException If error occurs while retrieving {@link PIICategoryMapping}
     */
    List<PIICategoryMapping> listPIICategoryMapping(Integer limit, Integer offset, Integer categoryId, String type)
            throws FHIRConsentMgtException;
}
