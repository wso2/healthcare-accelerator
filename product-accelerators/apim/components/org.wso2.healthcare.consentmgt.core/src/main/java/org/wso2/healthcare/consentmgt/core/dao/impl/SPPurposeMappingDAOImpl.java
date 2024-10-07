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

import org.wso2.carbon.database.utils.jdbc.JdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.healthcare.consentmgt.core.SQLConstants;
import org.wso2.healthcare.consentmgt.core.dao.SPPurposeMappingDAO;
import org.wso2.healthcare.consentmgt.core.exception.FHIRConsentMgtException;
import org.wso2.healthcare.consentmgt.core.model.Purpose;
import org.wso2.healthcare.consentmgt.core.model.SPApplication;
import org.wso2.healthcare.consentmgt.core.model.SPApplicationListResponse;
import org.wso2.healthcare.consentmgt.core.model.SPPurposeMapping;
import org.wso2.healthcare.consentmgt.core.util.JDBCUtil;

import java.util.ArrayList;
import java.util.List;

public class SPPurposeMappingDAOImpl implements SPPurposeMappingDAO {

    public SPPurposeMapping addSPPurpose(SPPurposeMapping spPurposeMapping) throws FHIRConsentMgtException {
        SPPurposeMapping result;
        int insertedId;
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        try {
            insertedId = jdbcTemplate.executeInsert(SQLConstants.INSERT_SP_PURPOSE_MAPPING_SQL, (preparedStatement -> {
                preparedStatement.setString(1, spPurposeMapping.getConsumerKey());
                preparedStatement.setInt(2, spPurposeMapping.getPurposeId());
                preparedStatement.setString(3, spPurposeMapping.getPurposeName());
                preparedStatement.setString(4, spPurposeMapping.getAppName());
            }), spPurposeMapping, true);
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException("Error occurred while adding the sp to consent purpose mapping.", e);
        }
        result = new SPPurposeMapping(insertedId, spPurposeMapping.getConsumerKey(), spPurposeMapping.getPurposeId(),
                spPurposeMapping.getPurposeName(), spPurposeMapping.getAppName());
        return result;
    }

    public SPPurposeMapping updateSPPurpose(SPPurposeMapping spPurposeMapping) throws FHIRConsentMgtException {
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(SQLConstants.UPDATE_SP_PURPOSE_MAPPING_SQL, (preparedStatement -> {
                preparedStatement.setString(1, spPurposeMapping.getConsumerKey());
                preparedStatement.setInt(2, spPurposeMapping.getPurposeId());
                preparedStatement.setString(3, spPurposeMapping.getPurposeName());
                preparedStatement.setString(4, spPurposeMapping.getAppName());
                preparedStatement.setInt(5, spPurposeMapping.getId());
            }));
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException("Error occurred while update the sp to consent purpose mapping.", e);
        }
        return spPurposeMapping;
    }

    public SPPurposeMapping getSPPurposesById(Integer id) throws FHIRConsentMgtException {
        if (id == null) {
            throw new FHIRConsentMgtException("SPPurpose id is missing to fetch consent purpose mapping.");
        }
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        SPPurposeMapping spPurposeMapping;

        try {
            spPurposeMapping = jdbcTemplate.fetchSingleRecord(SQLConstants.GET_SP_PURPOSE_MAPPING_BY_ID_SQL,
                    (resultSet, rowNumber) -> new SPPurposeMapping(resultSet.getInt(1), resultSet.getString(2),
                            resultSet.getInt(3), resultSet.getString(4), resultSet.getString(5)),
                    preparedStatement -> preparedStatement.setInt(1, id));
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException(
                    "Error occurred while fetching the sp to consent purpose mapping by id: " + id, e);
        }
        return spPurposeMapping;
    }

    @Override
    public void deleteSPPurposesById(Integer id) throws FHIRConsentMgtException {
        if (id == null) {
            throw new FHIRConsentMgtException("SPPurpose id is missing to delete consent purpose mapping.");
        }
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(SQLConstants.DELETE_SP_PURPOSE_MAPPING_BY_ID_SQL,
                    preparedStatement -> preparedStatement.setInt(1, id));
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException(
                    "Error occurred while fetching the sp to consent purpose mapping by id: " + id, e);
        }
    }

    public List<SPPurposeMapping> getSPPurposesByConsumerKey(String consumerKey) throws FHIRConsentMgtException {
        if (consumerKey == null) {
            throw new FHIRConsentMgtException("Application id is missing to fetch consent purpose mapping.");
        }
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        List<SPPurposeMapping> spPurposeMappingList;

        try {
            spPurposeMappingList = jdbcTemplate.executeQuery(SQLConstants.GET_SP_PURPOSE_MAPPING_BY_CONSUMER_KEY_SQL,
                    (resultSet, rowNumber) -> new SPPurposeMapping(resultSet.getInt(1), resultSet.getString(2),
                            resultSet.getInt(3), resultSet.getString(4), resultSet.getString(5)),
                    preparedStatement -> preparedStatement.setString(1, consumerKey));
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException(
                    "Error occurred while fetching the sp to consent purpose mapping by app Id: " + consumerKey, e);
        }
        return spPurposeMappingList;
    }

    public List<SPPurposeMapping> getSPPurposesByPurposeId(Integer purposeId) throws FHIRConsentMgtException {
        if (purposeId == null) {
            throw new FHIRConsentMgtException("Purpose id is missing to fetch consent purpose mapping.");
        }
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        List<SPPurposeMapping> spPurposeMappingList;

        try {
            spPurposeMappingList = jdbcTemplate.executeQuery(SQLConstants.GET_SP_PURPOSE_MAPPING_BY_PURPOSE_ID_SQL,
                    (resultSet, rowNumber) -> new SPPurposeMapping(resultSet.getInt(1), resultSet.getString(2),
                            resultSet.getInt(3), resultSet.getString(4), resultSet.getString(5)),
                    preparedStatement -> preparedStatement.setInt(1, purposeId));
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException(
                    "Error occurred while fetching the sp to consent purpose mapping by purpose id: " + purposeId, e);
        }
        return spPurposeMappingList;
    }

    @Override
    public List<SPPurposeMapping> getSPPurposesByAppName(String appName) throws FHIRConsentMgtException {
        if (appName == null) {
            throw new FHIRConsentMgtException("Application name missing to fetch consent purpose mapping.");
        }
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        List<SPPurposeMapping> spPurposeMappingList;

        try {
            spPurposeMappingList = jdbcTemplate.executeQuery(SQLConstants.GET_SP_PURPOSE_MAPPING_BY_APP_NAME_SQL,
                    (resultSet, rowNumber) -> new SPPurposeMapping(resultSet.getInt(1), resultSet.getString(2),
                            resultSet.getInt(3), resultSet.getString(4), resultSet.getString(5)),
                    preparedStatement -> preparedStatement.setString(1, appName));
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException(
                    "Error occurred while fetching the sp to consent purpose mapping by application name: " + appName,
                    e);
        }
        return spPurposeMappingList;
    }

    public List<SPPurposeMapping> searchSPPurposes(Integer purposeId, String consumerKey, Integer limit, Integer offset,
            String applicationName) throws FHIRConsentMgtException {
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        List<SPPurposeMapping> spPurposeMappingList;
        if (limit == null) {
            limit = SQLConstants.DEFAULT_SEARCH_LIMIT;
        }
        if (offset == null) {
            offset = SQLConstants.DEFAULT_SEARCH_OFFSET;
        }
        int finalOffset = offset;
        int finalLimit = limit;
        try {
            if (purposeId != null && consumerKey != null) {
                spPurposeMappingList = jdbcTemplate
                        .executeQuery(SQLConstants.SEARCH_SP_PURPOSE_MAPPING_BY_CONSUMER_KEY_PURPOSE_ID_SQL,
                                (resultSet, rowNumber) -> new SPPurposeMapping(resultSet.getInt(1),
                                        resultSet.getString(2), resultSet.getInt(3), resultSet.getString(4),
                                        resultSet.getString(5)), preparedStatement -> {
                                    preparedStatement.setString(1, consumerKey);
                                    preparedStatement.setInt(2, purposeId);
                                    preparedStatement.setInt(3, finalLimit);
                                    preparedStatement.setInt(4, finalOffset);
                                });
            } else if (purposeId != null && applicationName != null) {
                spPurposeMappingList = jdbcTemplate
                        .executeQuery(SQLConstants.SEARCH_SP_PURPOSE_MAPPING_BY_APP_NAME_PURPOSE_ID_SQL,
                                (resultSet, rowNumber) -> new SPPurposeMapping(resultSet.getInt(1),
                                        resultSet.getString(2), resultSet.getInt(3), resultSet.getString(4),
                                        resultSet.getString(5)), preparedStatement -> {
                                    preparedStatement.setString(1, applicationName);
                                    preparedStatement.setInt(2, purposeId);
                                    preparedStatement.setInt(3, finalLimit);
                                    preparedStatement.setInt(4, finalOffset);
                                });
            } else if (consumerKey != null) {
                spPurposeMappingList = jdbcTemplate
                        .executeQuery(SQLConstants.SEARCH_SP_PURPOSE_MAPPING_BY_CONSUMER_KEY_SQL,
                                (resultSet, rowNumber) -> new SPPurposeMapping(resultSet.getInt(1),
                                        resultSet.getString(2), resultSet.getInt(3), resultSet.getString(4),
                                        resultSet.getString(5)), preparedStatement -> {
                                    preparedStatement.setString(1, consumerKey);
                                    preparedStatement.setInt(2, finalLimit);
                                    preparedStatement.setInt(3, finalOffset);
                                });
            } else if (applicationName != null) {
                spPurposeMappingList = jdbcTemplate.executeQuery(SQLConstants.SEARCH_SP_PURPOSE_MAPPING_BY_APP_NAME_SQL,
                        (resultSet, rowNumber) -> new SPPurposeMapping(resultSet.getInt(1), resultSet.getString(2),
                                resultSet.getInt(3), resultSet.getString(4), resultSet.getString(5)),
                        preparedStatement -> {
                            preparedStatement.setString(1, applicationName);
                            preparedStatement.setInt(2, finalLimit);
                            preparedStatement.setInt(3, finalOffset);
                        });
            } else if (purposeId != null) {
                spPurposeMappingList = jdbcTemplate
                        .executeQuery(SQLConstants.SEARCH_SP_PURPOSE_MAPPING_BY_PURPOSE_ID_SQL,
                                (resultSet, rowNumber) -> new SPPurposeMapping(resultSet.getInt(1),
                                        resultSet.getString(2), resultSet.getInt(3), resultSet.getString(4),
                                        resultSet.getString(5)), preparedStatement -> {
                                    preparedStatement.setInt(1, purposeId);
                                    preparedStatement.setInt(2, finalLimit);
                                    preparedStatement.setInt(3, finalOffset);
                                });
            } else {
                spPurposeMappingList = jdbcTemplate.executeQuery(SQLConstants.GET_ALL_PURPOSE_MAPPING_SQL,
                        (resultSet, rowNumber) -> new SPPurposeMapping(resultSet.getInt(1), resultSet.getString(2),
                                resultSet.getInt(3), resultSet.getString(4), resultSet.getString(5)),
                        preparedStatement -> {
                            preparedStatement.setInt(1, finalLimit);
                            preparedStatement.setInt(2, finalOffset);
                        });
            }
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException(
                    "Error occurred while fetching the sp to consent purpose mapping by purpose id: " + purposeId, e);
        }
        return spPurposeMappingList;
    }

    @Override
    public SPApplicationListResponse getApplications(Integer limit, Integer offset, String consumerKey,
            String appName) throws FHIRConsentMgtException {
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        SPApplicationListResponse spApplicationListResponse = new SPApplicationListResponse();
        if (limit == null) {
            limit = SQLConstants.DEFAULT_SEARCH_LIMIT;
        }
        if (offset == null) {
            offset = SQLConstants.DEFAULT_SEARCH_OFFSET;
        }
        int finalOffset = offset;
        int finalLimit = limit;
        try {
            List<SPApplication> consumerApps = new ArrayList<>();
            if (appName == null && consumerKey == null) {
                consumerApps = jdbcTemplate.executeQuery(SQLConstants.GET_ALL_APPLICATION_KEYS,
                        (resultSet, rowNumber) -> new SPApplication(resultSet.getString(1), resultSet.getString(2)),
                        preparedStatement -> {
                            preparedStatement.setInt(1, finalLimit);
                            preparedStatement.setInt(2, finalOffset);
                        });
            }
            for (SPApplication app : consumerApps) {
                List<Purpose> purposeList = jdbcTemplate.executeQuery(SQLConstants.GET_ALL_PURPOSES_FOR_APP,
                        (resultSet, rowNumber) -> new Purpose(resultSet.getInt(1), resultSet.getString(2)),
                        preparedStatement -> preparedStatement.setString(1, app.getId()));
                app.setPurposes(purposeList);
            }
            spApplicationListResponse.setApplications(consumerApps);
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException("Error occurred while fetching the sp applications.", e);
        }
        return spApplicationListResponse;
    }
}
