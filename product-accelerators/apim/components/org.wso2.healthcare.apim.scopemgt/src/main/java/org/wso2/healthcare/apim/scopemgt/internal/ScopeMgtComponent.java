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

package org.wso2.healthcare.apim.scopemgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.core.ServerStartupObserver;
import org.wso2.carbon.identity.core.util.IdentityCoreInitializedEvent;
import org.wso2.healthcare.apim.scopemgt.observers.ScopeMgtStartupObserver;


@Component(
        name = "org.wso2.healthcare.apim.scopemgt.internal.ScopeMgtComponent",
        immediate = true
)
public class ScopeMgtComponent {
    private static final Log log = LogFactory.getLog(ScopeMgtComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(ServerStartupObserver.class, new ScopeMgtStartupObserver(),
                    null);
            if (log.isDebugEnabled()) {
                log.debug("Scope Management Component is activated.");
            }
        } catch (Throwable e) {
            log.fatal("Error while activating the scope management component. ", e);
        }
    }

    @Reference(
            name = "identityCoreInitializedEventService",
            service = IdentityCoreInitializedEvent.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityCoreInitializedEventService"
    )
    protected void setIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        /* reference IdentityCoreInitializedEvent service to guarantee that this component will wait until identity core
         is started */
        if (log.isDebugEnabled()) {
            log.debug("Set Identity core Initialized Event Service.");
        }
    }

    protected void unsetIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Unset Identity core Initialized Event Service.");
        }
    }

}