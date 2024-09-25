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

package org.wso2.healthcare.apim.scopemgt.observers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.ServerStartupObserver;
import org.wso2.healthcare.apim.scopemgt.ScopeMgtSetup;

/**
 * Open Healthcare gateway server startup observer class.
 */
public class ScopeMgtStartupObserver implements ServerStartupObserver {

    private static final Log log = LogFactory.getLog(
            org.wso2.healthcare.apim.scopemgt.observers.ScopeMgtStartupObserver.class);

    public void completingServerStartup() {
        // do nothing
    }

    /**
     * Method will trigger role and scope creation at server startup
     */
    public void completedServerStartup() {

        log.info("WSO2 OpenHealthcare FHIR Server started.");
        ScopeMgtSetup scopeMgtSetup = new ScopeMgtSetup();
        scopeMgtSetup.createHealthcareSpecificRolesAndScopes(ScopeMgtSetup.SUPER_TENANT_ID);
        log.info("WSO2 OpenHealthcare related roles and scopes created");
    }
}
