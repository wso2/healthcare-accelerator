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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.apim.core.OpenHealthcareException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Holds SaaS Registration database of Open-Healthcare platform
 */
public class RegistrationDS extends AbstractDataSource {

    private static final String DEFAULT_OH_DS_NAME = "jdbc/OHRegistrationDB";
    private static final Log LOG = LogFactory.getLog(RegistrationDS.class);

    private static final RegistrationDS instance = new RegistrationDS();

    private DataSource dataSource = null;

    private RegistrationDS() {
    }

    @Override
    protected DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void initialize(String dsName) throws OpenHealthcareException {
        if (dataSource != null) {
            return;
        }
        synchronized (RegistrationDS.class) {
            if (dataSource == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Initializing Open-Healthcare Account data source");
                }
                if (StringUtils.isEmpty(dsName)) {
                    dsName = DEFAULT_OH_DS_NAME;
                }
                try {
                    Context ctx = new InitialContext();
                    dataSource = (DataSource) ctx.lookup(dsName);
                } catch (NamingException e) {
                    throw new OpenHealthcareException("Error while looking up the data source : " + dsName, e);
                }
            }
        }
    }

    public static AbstractDataSource getInstance() {
        return instance;
    }
}
