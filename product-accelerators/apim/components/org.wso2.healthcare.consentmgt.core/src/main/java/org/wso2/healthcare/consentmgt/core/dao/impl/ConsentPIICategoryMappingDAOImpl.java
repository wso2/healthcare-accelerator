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

package org.wso2.healthcare.consentmgt.core.dao.impl;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.database.utils.jdbc.JdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.healthcare.consentmgt.core.SQLConstants;
import org.wso2.healthcare.consentmgt.core.dao.ConsentPIICategoryMappingDAO;
import org.wso2.healthcare.consentmgt.core.exception.FHIRConsentMgtException;
import org.wso2.healthcare.consentmgt.core.model.PIICategoryMapping;
import org.wso2.healthcare.consentmgt.core.util.JDBCUtil;

import java.util.List;

public class ConsentPIICategoryMappingDAOImpl implements ConsentPIICategoryMappingDAO {
    @Override
    public PIICategoryMapping addPIICategoryMapping(PIICategoryMapping mapping)
            throws FHIRConsentMgtException {
        PIICategoryMapping result;
        int insertedId = 0;
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        try {
            jdbcTemplate.executeInsert(SQLConstants.INSERT_PII_CATEGORY_MAPPING_SQL, (preparedStatement -> {
                preparedStatement.setInt(1, mapping.getPiiCategoryId());
                preparedStatement.setString(2, mapping.getType());
            }), mapping, true);
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException("Error occurred while adding PII Category mapping.", e);
        }
        result = new PIICategoryMapping(insertedId, mapping.getType(), mapping.getPiiCategoryId());
        return result;
    }

    @Override
    public PIICategoryMapping getPIICategoryMappingFromId(Integer id) throws FHIRConsentMgtException {
        if (id == null) {
            throw new FHIRConsentMgtException("Mapping id is not available.");
        }
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        try {
            List<PIICategoryMapping> piiCategoryMappings = jdbcTemplate
                    .executeQuery(SQLConstants.GET_ALL_PII_CATEGORY_MAPPINGS_BY_ID,
                            (resultSet, rowNumber) -> new PIICategoryMapping(resultSet.getInt(1),
                                    resultSet.getString(3), resultSet.getInt(2)),
                            preparedStatement -> preparedStatement.setInt(1, id));
            return piiCategoryMappings.get(0);
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException("Error occurred while retrieving PII Category mappings by id: " + id, e);
        }
    }

    @Override
    public List<PIICategoryMapping> listPIICategoryMapping(Integer limit, Integer offset, Integer categoryId,
            String type) throws FHIRConsentMgtException {
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        if (limit == null) {
            limit = SQLConstants.DEFAULT_SEARCH_LIMIT;
        }
        if (offset == null) {
            offset = SQLConstants.DEFAULT_SEARCH_OFFSET;
        }
        int finalOffset = offset;
        int finalLimit = limit;

        List<PIICategoryMapping> piiCategoryMappings;
        try {
            if (categoryId == null && StringUtils.isBlank(type)) {
                piiCategoryMappings = jdbcTemplate.executeQuery(SQLConstants.GET_ALL_PII_CATEGORY_MAPPINGS,
                        (resultSet, rowNumber) -> new PIICategoryMapping(resultSet.getInt(1), resultSet.getString(3),
                                resultSet.getInt(2)), preparedStatement -> {
                            preparedStatement.setInt(1, finalLimit);
                            preparedStatement.setInt(2, finalOffset);
                        });
            } else if (categoryId != null) {
                piiCategoryMappings = jdbcTemplate.executeQuery(SQLConstants.GET_ALL_PII_CATEGORY_MAPPINGS_FROM_CATEGORY_ID,
                        (resultSet, rowNumber) -> new PIICategoryMapping(resultSet.getInt(1), resultSet.getString(3),
                                resultSet.getInt(2)), preparedStatement -> {
                            preparedStatement.setInt(1, categoryId);
                            preparedStatement.setInt(1, finalLimit);
                            preparedStatement.setInt(2, finalOffset);
                        });
            } else {
                piiCategoryMappings = jdbcTemplate.executeQuery(SQLConstants.GET_ALL_PII_CATEGORY_MAPPINGS_BY_TYPE,
                        (resultSet, rowNumber) -> new PIICategoryMapping(resultSet.getInt(1), resultSet.getString(3),
                                resultSet.getInt(2)), preparedStatement -> {
                            preparedStatement.setString(1, type);
                            preparedStatement.setInt(1, finalLimit);
                            preparedStatement.setInt(2, finalOffset);
                        });
            }
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException("Error occurred while retrieving PII Category mappings.", e);
        }
        return piiCategoryMappings;
    }
}
