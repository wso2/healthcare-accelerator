/*
Copyright (c) $today.year, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

This software is the property of WSO2 LLC. and its suppliers, if any.
Dissemination of any information or reproduction of any material contained
herein is strictly forbidden, unless permitted by WSO2 in accordance with
the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
For specific language governing the permissions and limitations under
this license, please see the license as well as any agreement you’ve
entered into with WSO2 governing the purchase of this software and any
associated services.
 */

package org.wso2.healthcare.apim.scopemgt.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO;
import org.wso2.carbon.apimgt.keymgt.APIKeyMgtException;
import org.wso2.carbon.apimgt.keymgt.handlers.DefaultKeyValidationHandler;
import org.wso2.carbon.apimgt.keymgt.service.TokenValidationContext;
import org.wso2.healthcare.apim.scopemgt.ScopeMgtException;
import org.wso2.healthcare.apim.scopemgt.util.ScopeMgtUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Class extends DefaultKeyValidationHandler to support SMART-on-FHIR scope management
 */
public class OpenHealthcareExtendedKeyValidationHandler extends DefaultKeyValidationHandler {

    private static final Log log = LogFactory.getLog(
            org.wso2.healthcare.apim.scopemgt.handlers.OpenHealthcareExtendedKeyValidationHandler.class);

    /**
     * The method to extend scope validation at JWT bearer token validation
     *
     * @param validationContext Validation context
     * @return true, false
     * @throws APIKeyMgtException APIKey Mgt Exception
     */
    @Override
    public boolean validateScopes(TokenValidationContext validationContext) throws APIKeyMgtException {

        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = validationContext.getValidationInfoDTO();
        if (apiKeyValidationInfoDTO == null) {
            throw new APIKeyMgtException("Key Validation information not set");
        }

        // check if API is FHIR and get the FHIR resource type
        String FHIRResourceType;
        try {
            FHIRResourceType = ScopeMgtUtil.getFHIRResourceTypeFromAPI(validationContext);
        } catch (ScopeMgtException e) {
            log.warn("Failed to get FHIR resource type from API while validating the token", e);
            throw new APIKeyMgtException(e.getMessage());
        }

        //IF FHIR API
        if (FHIRResourceType != null) {
            List<String> scopes = new ArrayList<>(apiKeyValidationInfoDTO.getScopes());
            List<String> internalCompatibleScopes = ScopeMgtUtil.convertClientFacingFHIRScopesToInternalFHIRScopes(scopes);
            List<String> handledScopes = handleWildCardScopes(FHIRResourceType, internalCompatibleScopes);
            apiKeyValidationInfoDTO.setScopes(new HashSet<>(handledScopes));
        }
        return superValidateScopes(validationContext);

    }

    private boolean superValidateScopes(TokenValidationContext validationContext) throws APIKeyMgtException {

        return super.validateScopes(validationContext);
    }

    /**
     * This method will add a resource specific scope if wildcard(*) is present
     * ex: patient-*.s -> patient-Observation.s
     *
     * @param FHIRResourceType resource type string
     * @param scopes           List of scopes in validation context
     * @return List of scopes
     */
    private List<String> handleWildCardScopes(String FHIRResourceType, List<String> scopes) {

        final List<String> handledScopes = new ArrayList<>(scopes);
        for (String scope : scopes) {
            if (ScopeMgtUtil.isScopeWildcardScope(scope)) {
                String newScope = ScopeMgtUtil.subStringReplace(scope, "*", FHIRResourceType);
                handledScopes.add(newScope);
            }
        }

        return handledScopes;
    }

}
