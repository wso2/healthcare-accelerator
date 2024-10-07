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

import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.authz.OAuthAuthzReqMessageContext;
import org.wso2.carbon.identity.oauth2.authz.handlers.CodeResponseTypeHandler;
import org.wso2.healthcare.apim.scopemgt.util.ScopeMgtUtil;

/**
 * Class extends  CodeResponseTypeHandler to support SMART-on-FHIR
 */
public class OpenHealthcareExtendedCodeResponseTypeHandler extends CodeResponseTypeHandler {

    /**
     * The method to extend scope validation ant auth code generation
     *
     * @param oauthAuthzMsgCtx Auth Msg Context
     * @return true, false
     * @throws IdentityOAuth2Exception Identity Oauth exception
     */
    @Override
    public boolean validateScope(OAuthAuthzReqMessageContext oauthAuthzMsgCtx) throws IdentityOAuth2Exception {
        //pre-process scopes before internal validation. Converting "patient/" and "user/" to "patient-" and "user-"
        String[] requestedScopes = ScopeMgtUtil.checkAndHandleMissingPatientLaunchScope(oauthAuthzMsgCtx.getAuthorizationReqDTO().getScopes());
        String[] internalCompatibleScopes = ScopeMgtUtil.convertClientFacingFHIRScopesToInternalFHIRScopes(requestedScopes);
        String[] scopes = ScopeMgtUtil.expandComplexScopes(internalCompatibleScopes);
        oauthAuthzMsgCtx.getAuthorizationReqDTO().setScopes(scopes);

        boolean validatedResult = superValidateScope(oauthAuthzMsgCtx);

        if (!validatedResult) { // if validation fails
            return false;
        }
        //post validation processing. Converting "patient-" and "user-" to "patient/" and "user/"
        String[] convertedScopes = oauthAuthzMsgCtx.getApprovedScope();
        String[] clientFacingScopes = ScopeMgtUtil.convertInternalFHIRScopesToClientFacingFHIRScopes(convertedScopes);
        oauthAuthzMsgCtx.setApprovedScope(clientFacingScopes);

        return true;
    }

    private boolean superValidateScope(OAuthAuthzReqMessageContext oauthAuthzMsgCtx) throws IdentityOAuth2Exception {

        return super.validateScope(oauthAuthzMsgCtx);
    }
}