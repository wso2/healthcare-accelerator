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
import org.wso2.healthcare.consentmgt.core.dao.ConsentPurposeDAO;
import org.wso2.healthcare.consentmgt.core.exception.FHIRConsentMgtException;
import org.wso2.healthcare.consentmgt.core.util.JDBCUtil;

import java.util.List;

public class ConsentPurposeDAOImpl implements ConsentPurposeDAO {
    @Override
    public String getPurposeNameFromId(Integer id) throws FHIRConsentMgtException {
        if (id == null) {
            throw new FHIRConsentMgtException("Purpose id is missing to fetch consent purpose.");
        }
        JdbcTemplate jdbcTemplate = JDBCUtil.getNewTemplate();
        List<Object> purposeNames;
        try {
           purposeNames = jdbcTemplate.executeQuery(SQLConstants.GET_PURPOSE_NAME_FROM_ID,
                    (resultSet, rowNumber) -> resultSet.getString(1),
                    preparedStatement -> preparedStatement.setInt(1, id));
        } catch (DataAccessException e) {
            throw new FHIRConsentMgtException(
                    "Error occurred while fetching the purpose by id: " + id, e);
        }
        if (purposeNames != null) {
            return (String) purposeNames.get(0);
        }
        return null;
    }
}
