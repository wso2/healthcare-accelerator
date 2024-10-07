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

package org.wso2.healthcare.apim.clientauth.jwt.storage;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.oauth2.client.authentication.OAuthClientAuthnException;
import org.wso2.healthcare.apim.clientauth.jwt.dao.JWTStorageManager;

@WithH2Database(jndiName = "jdbc/WSO2CarbonDB", files = {"dbscripts/identity.sql"}, dbName = "testdb1")
public class JWTStorageManagerTest {

    private JWTStorageManager JWTStorageManager;

    @BeforeClass
    public void setUp() throws Exception {

        JWTStorageManager = new JWTStorageManager();
    }

    @Test()
    public void testPersistJWTIdInDB() throws Exception {

        JWTStorageManager.persistJWTIdInDB("100", "abcd", 10000000);
    }

    @Test(expectedExceptions = OAuthClientAuthnException.class)
    public void testPersistJWTIdInDBExceptionCase() throws Exception {

        JWTStorageManager.persistJWTIdInDB("2000", "client1", 10000001);
    }
}
