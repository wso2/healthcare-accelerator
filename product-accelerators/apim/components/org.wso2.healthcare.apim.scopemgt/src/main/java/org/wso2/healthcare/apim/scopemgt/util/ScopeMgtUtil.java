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

package org.wso2.healthcare.apim.scopemgt.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.keymgt.SubscriptionDataHolder;
import org.wso2.carbon.apimgt.keymgt.model.SubscriptionDataStore;
import org.wso2.carbon.apimgt.keymgt.model.entity.API;
import org.wso2.carbon.apimgt.keymgt.service.TokenValidationContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.scopemgt.ScopeMgtConstants;
import org.wso2.healthcare.apim.scopemgt.ScopeMgtException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class with scope management util methods
 */
public class ScopeMgtUtil {

    private static final Log log = LogFactory.getLog(ScopeMgtUtil.class);

    /**
     * This method will return if the fhir scope delimiter change is enabled or not on deployment.toml
     *
     * @return bool
     */
    public static boolean isFHIRScopeToWSO2ScopeMappingEnabled() {

        try {
            return OpenHealthcareEnvironment.
                    getInstance().getConfig().getScopeMgtConfig().isFHIRScopeToWSO2ScopeMappingEnabled();
        } catch (OpenHealthcareException e) {
            log.warn("Unable to get scope mgt config", e);
            return false;
        }
    }

    /**
     * Checks whether the scope is an internal FHIR scope (ex:patient-*.r,user-*.r)
     *
     * @param scope Scope string
     * @return true, false
     */
    public static boolean isScopeInternalFHIRScope(String scope) {

        return (scope.startsWith(ScopeMgtConstants.INTERNAL_USER_SCOPE_PREFIX)
                || scope.startsWith(ScopeMgtConstants.INTERNAL_PATIENT_SCOPE_PREFIX))
                && scope.contains(".");
    }

    /**
     * Checks whether the scope is a client facing FHIR scope (ex:patient/*.r,user/*.s)
     *
     * @param scope Scope string
     * @return true, false
     */
    public static boolean isScopeClientFacingFHIRScope(String scope) {

        return (scope.startsWith(ScopeMgtConstants.CLIENT_FACING_PATIENT_SCOPE_PREFIX)
                || scope.startsWith(ScopeMgtConstants.CLIENT_FACING_USER_SCOPE_PREFIX))
                && scope.contains(".");
    }

    /**
     * Checks whether the scope is a client facing FHIR patient scope (ex:patient/*.r)
     *
     * @param scope Scope string
     * @return true, false
     */
    public static boolean isScopeClientFacingPatientScope(String scope) {

        return scope.startsWith(ScopeMgtConstants.CLIENT_FACING_PATIENT_SCOPE_PREFIX)
                && scope.contains(".");
    }

    /**
     * Check if the scope is an internal or a client facing wildcard scope
     *
     * @param scope String scope
     * @return true, false
     */
    public static boolean isScopeWildcardScope(String scope) {

        return (scope.startsWith("patient") || scope.startsWith("user")) && scope.contains("*.");
    }

    /**
     * This method will add launch/patient scope if patient scopes are requested without launch/patient
     *
     * @param scopes scope arr
     * @return scopes arr
     */
    public static String[] checkAndHandleMissingPatientLaunchScope(String[] scopes) {

        List<String> scopesList = new ArrayList<>(Arrays.asList(scopes));
        if (!scopesList.contains(ScopeMgtConstants.PATIENT_LAUNCH_SCOPE)) {
            for (String scope : scopes) {
                if (isScopeClientFacingPatientScope(scope)) {
                    scopesList.add(ScopeMgtConstants.PATIENT_LAUNCH_SCOPE);
                    break;
                }
            }
        }
        String[] scopesArr = new String[scopesList.size()];
        return scopesList.toArray(scopesArr);

    }

    /**
     * This method is used to expand FHIR scope suffixes
     * ex: patient/Observation.rs to patient/Observation.r and patient/Observation.s
     *
     * @param requestedScopes The list of requested scopes in the auth msg ctx
     * @return List of expanded scopes
     */
    public static String[] expandComplexScopes(String[] requestedScopes) {

        List<String> scopesList = new ArrayList<>();
        for (String scope : requestedScopes) {

            if (isScopeInternalFHIRScope(scope) || isScopeClientFacingFHIRScope(scope)) {
                String[] splitScope = scope.split("\\."); // split by '.'

                if (splitScope.length == 2) {
                    String scopePrefix = splitScope[0];
                    char[] scopeSuffixes = splitScope[1].toCharArray();
                    for (char suffix : scopeSuffixes) {
                        if (ScopeMgtConstants.ALLOWED_SCOPE_SUFFIXES.contains(String.valueOf(suffix))) {
                            scopesList.add(scopePrefix.concat(".").concat(String.valueOf(suffix)));
                        }
                    }
                } else {
                    scopesList.add(scope);
                }

            } else {
                scopesList.add(scope);
            }
        }
        String[] scopes = new String[scopesList.size()];
        return scopesList.toArray(scopes);
    }

    /**
     * Method will convert client requested FHIR scopes to internally compatible FHIR scopes
     * ex: patient/Observation.r to patient-Observation.r
     *
     * @param scopes Array of scopes
     * @return processed array of scopes
     */
    public static String[] convertClientFacingFHIRScopesToInternalFHIRScopes(String[] scopes) {

        if (isFHIRScopeToWSO2ScopeMappingEnabled()) {
            List<String> convertedScopes = Arrays.asList(scopes);
            for (String scope : scopes) {
                if (isScopeClientFacingFHIRScope(scope)) {
                    // replacing "/" in scope string character with "-" character
                    String newScope = subStringReplace(scope,
                            org.wso2.healthcare.apim.core.utils.ScopeMgtUtil.CLIENT_FACING_SCOPE_DELIMITER,
                            org.wso2.healthcare.apim.core.utils.ScopeMgtUtil.INTERNAL_SCOPE_DELIMITER);
                    convertedScopes.set(convertedScopes.indexOf(scope), newScope);

                }
            }
            String[] convertedScopesArr = new String[convertedScopes.size()];
            return convertedScopes.toArray(convertedScopesArr);
        }
        return scopes;
    }

    public static List<String> convertClientFacingFHIRScopesToInternalFHIRScopes(List<String> scopes) {

        String[] scopesArr = scopes.toArray(new String[scopes.size()]);
        return Arrays.asList(convertClientFacingFHIRScopesToInternalFHIRScopes(scopesArr));
    }

    /**
     * Method will convert internally compatible FHIR scopes to client facing FHIR scopes
     * ex: patient-Observation.r to patient/Observation.r
     *
     * @param scopes Array of scopes
     * @return processed array of scopes
     */
    public static String[] convertInternalFHIRScopesToClientFacingFHIRScopes(String[] scopes) {

        if (isFHIRScopeToWSO2ScopeMappingEnabled()) {
            List<String> convertedScopes = Arrays.asList(scopes);
            for (String scope : scopes) {
                if (isScopeInternalFHIRScope(scope)) {
                    // replacing "-" character in scope string with "/" character
                    String newScope = subStringReplace(scope,
                            org.wso2.healthcare.apim.core.utils.ScopeMgtUtil.INTERNAL_SCOPE_DELIMITER,
                            org.wso2.healthcare.apim.core.utils.ScopeMgtUtil.CLIENT_FACING_SCOPE_DELIMITER);
                    convertedScopes.set(convertedScopes.indexOf(scope), newScope);
                }
            }
            String[] convertedScopesArr = new String[convertedScopes.size()];
            return convertedScopes.toArray(convertedScopesArr);
        }
        return scopes;

    }

    /**
     * Function to replace substring in string
     *
     * @param text         original string
     * @param searchString substring to replace
     * @param replacement  replacement string
     * @return formatted string
     */
    public static String subStringReplace(String text, String searchString, String replacement) {

        return StringUtils.replace(text, searchString, replacement);
    }

    /**
     * This method will try to identify whether the request is made to a FHIR api and will return the FHIR API name
     *
     * @param validationContext Validation context
     * @return Resource name
     */
    public static String getFHIRResourceTypeFromAPI(TokenValidationContext validationContext) throws ScopeMgtException {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String actualVersion = validationContext.getVersion();

        //Check if the api version has been prefixed with _default_
        if (actualVersion != null && actualVersion.startsWith(APIConstants.DEFAULT_VERSION_PREFIX)) {
            //Remove the prefix from the version.
            actualVersion = actualVersion.split(APIConstants.DEFAULT_VERSION_PREFIX)[1];
        }

        SubscriptionDataStore tenantSubscriptionStore =
                SubscriptionDataHolder.getInstance().getTenantSubscriptionStore(tenantDomain);
        API api = tenantSubscriptionStore.getApiByContextAndVersion(validationContext.getContext(),
                actualVersion);
        APIIdentifier apiIdentifier = new APIIdentifier(
                api.getApiProvider(),
                api.getApiName(),
                api.getApiVersion()
        );

        try {
            return org.wso2.healthcare.apim.core.utils.ScopeMgtUtil.getFHIRResourceTypeFromAPI(apiIdentifier);
        } catch (OpenHealthcareException e) {
            throw new ScopeMgtException("Error occurred while trying to get FHIR resource type from API", e);
        }
    }
}
