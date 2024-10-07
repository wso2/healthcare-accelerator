
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

package org.wso2.healthcare.apim.clientauth.jwt.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.oauth.common.OAuth2ErrorCodes;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.client.authentication.OAuthClientAuthnException;
import org.wso2.healthcare.apim.clientauth.jwt.Constants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * JWT token persistence is managed by JWTStorageManager
 * It saved JWTEntry instances in Identity Database.
 */
public class JWTStorageManager {

    private static final Log log = LogFactory.getLog(JWTStorageManager.class);

    /**
     * To get persisted JWT for a given JTI.
     *
     * @param jti jti
     * @param issuer issuer of the jwt
     * @return JWTEntry
     * @throws IdentityOAuth2Exception
     */
    public JWTEntry getJwtFromDB(String jti, String issuer) throws OAuthClientAuthnException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        JWTEntry jwtEntry = null;
        try {
            prepStmt = dbConnection.prepareStatement(Constants.SQLQueries.GET_JWT);
            prepStmt.setString(1, jti);
            prepStmt.setString(2, issuer);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                long exp = rs.getTime(1, Calendar.getInstance(TimeZone.getTimeZone(Constants.UTC))).getTime();
                jwtEntry = new JWTEntry(exp);
            }
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error when retrieving the JWT ID: " + jti, e);
            }
            throw new OAuthClientAuthnException("Error occurred while validating the JTI: " + jti + " of the " +
                                                "assertion.", OAuth2ErrorCodes.SERVER_ERROR);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(dbConnection, rs, prepStmt);
        }
        return jwtEntry;
    }

    /**
     * To persist jti and iss in the table.
     *
     * @param jti         jti a unique id
     * @param issuer      issuer of the jwt
     * @param expTime     expiration time
     * @throws IdentityOAuth2Exception
     */
    public void persistJWTIdInDB(String jti, String issuer, long expTime) throws OAuthClientAuthnException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            preparedStatement = connection.prepareStatement(Constants.SQLQueries.INSERT_JWD_ID);
            preparedStatement.setString(1, jti);
            preparedStatement.setString(2, issuer);
            Timestamp expTimestamp = new Timestamp(expTime);
            preparedStatement.setTimestamp(3, expTimestamp, Calendar.getInstance(TimeZone.getTimeZone(Constants.UTC)));
            preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.commit();
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                String error =
                        "Error when storing the JWT ID: " + jti + " with exp: " + expTime + " for the issuer: " + issuer;
                log.debug(error, e);
            }
            throw new OAuthClientAuthnException("Error occurred while validating the JTI: " + jti + " of the " +
                                                "assertion.", OAuth2ErrorCodes.SERVER_ERROR);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }
    }

    /**
     * Update exp of the given jti and iss.
     *
     * @param jti         jti a unique id
     * @param issuer      issuer of the jwt
     * @param expTime     expiration time
     * @throws IdentityOAuth2Exception
     */
    public void updateExp(String jti, String issuer, long expTime) throws OAuthClientAuthnException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            preparedStatement = connection.prepareStatement(Constants.SQLQueries.UPDATE_EXP);
            Timestamp expTimestamp = new Timestamp(expTime);
            preparedStatement.setTimestamp(1, expTimestamp, Calendar.getInstance(TimeZone.getTimeZone(Constants.UTC)));
            preparedStatement.setString(2, jti);
            preparedStatement.setString(3, issuer);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.commit();
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                String error = "Error when updating the expiry time of JWT with JTI: " + jti + "for the issuer: " + issuer;
                log.debug(error, e);
            }
            throw new OAuthClientAuthnException("Error occurred while validating the JTI: " + jti + " of the " +
                    "assertion.", OAuth2ErrorCodes.SERVER_ERROR);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }
    }
}
