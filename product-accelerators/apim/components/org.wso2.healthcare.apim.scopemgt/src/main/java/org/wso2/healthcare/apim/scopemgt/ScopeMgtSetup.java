
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
package org.wso2.healthcare.apim.scopemgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.config.ScopeMgtConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Class is used at server startup to set up healthcare specific roles and scopes
 */
public class ScopeMgtSetup {

    private static final Log log = LogFactory.getLog(
            ScopeMgtSetup.class);

    public static final int SUPER_TENANT_ID = -1234;

    /**
     * This method can be triggered to create healthcare specific roles and shared scopes when tenant is started
     *
     * @param tenantID tenant id
     */
    public void createHealthcareSpecificRolesAndScopes(int tenantID) {

        try {
            String tenantDomain = APIUtil.getTenantDomainFromTenantId(tenantID);
            ScopeMgtConfig roleAndScopeConfig = OpenHealthcareEnvironment.getInstance().getConfig().getScopeMgtConfig();
            List<String> roles = roleAndScopeConfig.getRoles();
            for (String role : roles) {
                APIUtil.createRole(role, null, tenantID);
            }

            ArrayList<Scope> scopes = roleAndScopeConfig.getScopes();

            String tenantAdminName = ServiceReferenceHolder.getInstance().getRealmService()
                    .getTenantUserRealm(tenantID).getRealmConfiguration().getAdminUserName();
            APIProvider provider = APIManagerFactory.getInstance().getAPIProvider(tenantAdminName);

            for (Scope scope : scopes) {
                Scope existingScope = APIUtil.getScopeByName(scope.getKey(), tenantDomain);
                if (existingScope == null) {
                    provider.addSharedScope(scope, tenantDomain);
                }
            }
        } catch (OpenHealthcareException | APIManagementException | UserStoreException e) {
            log.error("Error occurred configuring FHIR roles and scopes.", e);
        }

    }

}
