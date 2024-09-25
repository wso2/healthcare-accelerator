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

package org.wso2.healthcare.apim.core.dao;

/**
 * Constants related to SQL Queries used in DAOs
 */
public class SQLConstants {

    public static final String INSERT_REGISTRATION =
            " INSERT INTO REGISTRATION (REG_ID, REG_STATUS, REG_EMAIL, REG_FNAME, REG_LNAME, REG_USERNAME, REG_PASSWORD, " +
                    "REG_ORG, REG_TELEPHONE, REG_COUNTRY, REG_STATE, REG_ACCEPT_POLICY, REG_BUSINESS_NAME)" +
                    " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static final String SELECT_REG_SUMMARY_BY_ORG =
            "SELECT REG_ORG, REG_ID, REG_STATUS, REG_EMAIL, REG_FNAME, REG_LNAME, REG_USERNAME, REG_TELEPHONE, " +
                    "REG_COUNTRY, REG_STATE, REG_CREATED_TIME, REG_BUSINESS_NAME " +
                    "FROM REGISTRATION " +
                    "WHERE REG_ORG = ? LIMIT 1";

    public static final String SELECT_REG_ID_BY_ORG = "SELECT REG_ID FROM REGISTRATION WHERE REG_ORG = ? LIMIT 1";

    public static final String SELECT_REG_ID_BY_EMAIL = "SELECT REG_ID FROM REGISTRATION WHERE REG_EMAIL = ? LIMIT 1";

    public static final String SELECT_REGISTRATION_BY_ID =
            "SELECT REG_ORG, REG_ID, REG_STATUS, REG_EMAIL, REG_FNAME, REG_LNAME, REG_USERNAME, REG_PASSWORD, " +
                    "REG_TELEPHONE, REG_COUNTRY, REG_STATE, REG_CREATED_TIME, REG_UPDATED_TIME, REG_ACCEPT_POLICY, REG_BUSINESS_NAME " +
                    "FROM REGISTRATION " +
                    "WHERE REG_ID=? LIMIT 1";

    public static final String SELECT_REG_SUMMARY_BY_ID =
            "SELECT REG_ORG, REG_ID, REG_STATUS, REG_EMAIL, REG_FNAME, REG_LNAME, REG_USERNAME, " +
                    "REG_TELEPHONE, REG_COUNTRY, REG_STATE, REG_CREATED_TIME, REG_BUSINESS_NAME " +
                    "FROM REGISTRATION " +
                    "WHERE REG_ID=? LIMIT 1";

    public static final String DELETE_REGISTRATION_BY_ORG = "DELETE FROM REGISTRATION WHERE REG_ORG=?";

    public static final String INSERT_ACCOUNT =
            " INSERT INTO ACCOUNT (ID, ORG, STATUS, OWNER_USERNAME, OWNER_TEMP_PASSWORD, TENANT_ADMIN, TENANT_ADMIN_PASS)" +
                    " VALUES (?,?,?,?,?,?,?)";

    public static final String SELECT_ACCOUNT_BY_ID =
            "SELECT ID, ORG, STATUS, OWNER_PII_REF, OWNER_USERNAME, CREATED_TIME, UPDATED_TIME, TENANT_ADMIN " +
                    "FROM ACCOUNT " +
                    "WHERE ID=? LIMIT 1";

    public static final String SELECT_ACCOUNT_BY_ORG =
            "SELECT ID, ORG, STATUS, OWNER_PII_REF, OWNER_USERNAME, CREATED_TIME, UPDATED_TIME, TENANT_ADMIN " +
                    "FROM ACCOUNT " +
                    "WHERE ORG=? LIMIT 1";

    public static final String SELECT_ACC_ID_BY_ORG = "SELECT ID FROM ACCOUNT WHERE ORG=? LIMIT 1";

    public static final String UPDATE_ACCOUNT_ACTIVATION_STATUS = "UPDATE ACCOUNT SET STATUS=?, OWNER_PII_REF=? WHERE ID=?";

    public static final String INSERT_PII_DATA =
            " INSERT INTO PII_DATA (ID, ORG, EMAIL, FNAME, LNAME, TELEPHONE, COUNTRY, STATE, ACCEPT_POLICY, BUSINESS_NAME)" +
                    " VALUES (?,?,?,?,?,?,?,?,?,?)";

    public static final String SELECT_PII_ID_BY_EMAIL = "SELECT ID FROM PII_DATA WHERE EMAIL=? LIMIT 1";

    public static final String REG_CLM_ORG = "REG_ORG";
    public static final String REG_CLM_ID = "REG_ID";
    public static final String REG_CLM_BUSINESS_NAME = "REG_BUSINESS_NAME";
    public static final String REG_CLM_STATUS = "REG_STATUS";
    public static final String REG_CLM_EMAIL = "REG_EMAIL";
    public static final String REG_CLM_FNAME = "REG_FNAME";
    public static final String REG_CLM_LNAME = "REG_LNAME";
    public static final String REG_CLM_USERNAME = "REG_USERNAME";
    public static final String REG_CLM_PASSWORD = "REG_PASSWORD";
    public static final String REG_CLM_TELEPHONE = "REG_TELEPHONE";
    public static final String REG_CLM_COUNTRY = "REG_COUNTRY";
    public static final String REG_CLM_STATE = "REG_STATE";
    public static final String REG_CLM_ACCEPT_POLICY = "REG_ACCEPT_POLICY";
    public static final String REG_CLM_CREATED_TIME = "REG_CREATED_TIME";
    public static final String REG_CLM_UPDATED_TIME = "REG_UPDATED_TIME";

    public static final String ACC_CLM_ID = "ID";
    public static final String ACC_CLM_ORG = "ORG";
    public static final String ACC_CLM_STATUS = "STATUS";
    public static final String ACC_CLM_OWNER_PII_REF = "OWNER_PII_REF";
    public static final String ACC_CLM_OWNER_USERNAME = "OWNER_USERNAME";
    public static final String ACC_CLM_CREATED_TIME = "CREATED_TIME";
    public static final String ACC_CLM_UPDATED_TIME = "UPDATED_TIME";
    public static final String ACC_CLM_TENANT_ADMIN = "TENANT_ADMIN";

    public static final String PII_CLM_ID = "ID";
    public static final String PII_CLM_ORG = "ORG";
    public static final String PII_CLM_EMAIL = "EMAIL";
    public static final String PII_CLM_FNAME = "FNAME";
    public static final String PII_CLM_LNAME = "LNAME";
    public static final String PII_CLM_TELEPHONE = "TELEPHONE";
    public static final String PII_CLM_COUNTRY = "COUNTRY";
    public static final String PII_CLM_STATE = "STATE";
    public static final String PII_CLM_ACCEPT_POLICY = "ACCEPT_POLICY";
    public static final String PII_CLM_CREATED_TIME = "CREATED_TIME";
    public static final String PII_CLM_UPDATED_TIME = "UPDATED_TIME";
}
