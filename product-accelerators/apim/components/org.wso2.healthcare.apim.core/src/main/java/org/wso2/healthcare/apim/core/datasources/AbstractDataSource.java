/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.healthcare.apim.core.datasources;

import org.wso2.healthcare.apim.core.OpenHealthcareException;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Abstract class to hold and represent data source
 */
public abstract class AbstractDataSource {

    /**
     * Function to retrieve data source
     * @return
     */
    protected abstract DataSource getDataSource();

    /**
     * Initializes the data source
     *
     * @param dataSourceName  data source name for lookup
     * @throws OpenHealthcareException if an error occurs while loading DB configuration
     */
    public abstract void initialize(String dataSourceName) throws OpenHealthcareException;

    /**
     * Utility method to get a new database connection
     *
     * @return Connection
     * @throws java.sql.SQLException if failed to get Connection
     */
    public Connection getConnection() throws SQLException {
        if (getDataSource() != null) {
            return getDataSource().getConnection();
        }
        throw new SQLException("Data source is not configured properly.");
    }
}
