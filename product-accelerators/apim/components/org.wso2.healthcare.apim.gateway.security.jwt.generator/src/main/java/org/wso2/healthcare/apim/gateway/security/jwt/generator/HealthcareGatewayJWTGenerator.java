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

package org.wso2.healthcare.apim.gateway.security.jwt.generator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTInfoDto;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.APIMgtGatewayJWTGeneratorImpl;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.AbstractAPIMgtGatewayJWTGenerator;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.config.ClaimMgtConfig;

import java.util.Map;
import java.util.SortedMap;

@Component(
        enabled = true,
        service = AbstractAPIMgtGatewayJWTGenerator.class,
        name = "healthcareGatewayJWTGenerator"
)

/**
 * This class will extend the default jwt generator and add Patient ID as an additional claim
 */
public class HealthcareGatewayJWTGenerator extends APIMgtGatewayJWTGeneratorImpl {

    private static final Log LOG = LogFactory.getLog(HealthcareGatewayJWTGenerator.class);

    @Override
    public Map<String, Object> populateCustomClaims(JWTInfoDto jwtInfoDto) {

        Map<String, Object> claims = super.populateCustomClaims(jwtInfoDto);
        SortedMap<String, String> userClaims;
        String patientIdClaimURI;
        try {
            ClaimMgtConfig claimMgtConfig = OpenHealthcareEnvironment.getInstance().getConfig().getClaimMgtConfig();
            patientIdClaimURI = claimMgtConfig.getClaimUri();
            userClaims = APIUtil.getClaims(jwtInfoDto.getEndUser(), jwtInfoDto.getEndUserTenantId(),
                    getDialectURI());
            if (userClaims.containsKey(patientIdClaimURI)) {
                claims.put(patientIdClaimURI, userClaims.get(patientIdClaimURI));
            }
        } catch (APIManagementException e) {
            LOG.error("Error occurred when populating healthcare related claims", e);
        } catch (OpenHealthcareException e) {
            LOG.error("Error occurred when fetching configs from OpenHealthcare Environment", e);
        }
        return claims;
    }
}
