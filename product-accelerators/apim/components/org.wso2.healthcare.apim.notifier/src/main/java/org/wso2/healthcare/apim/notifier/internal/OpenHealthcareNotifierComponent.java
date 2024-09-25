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

package org.wso2.healthcare.apim.notifier.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.apimgt.impl.notifier.Notifier;
import org.wso2.healthcare.apim.notifier.APIEventReceiver;

@Component(
        name = "org.wso2.healthcare.apim.notifier.OpenHealthcareNotifierComponent",
        immediate = true)
public class OpenHealthcareNotifierComponent {

    private static final Log LOG = LogFactory.getLog(OpenHealthcareNotifierComponent.class);

    protected void activate(ComponentContext componentContext) {
        LOG.info("Activating Open-Healthcare Notifier component ...");
        try {
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext.registerService(Notifier.class.getName(), new APIEventReceiver(), null);
        } catch (Throwable throwable) {
            // Since exceptions are not logged by upstream code we are logging here to help troubleshooting
            LOG.error("Error occurred while activating OpenHealthcareNotifierComponent", throwable);
            throw throwable;
        }
        LOG.info("Open-Healthcare Notifier component successfully activated...");
    }
}
