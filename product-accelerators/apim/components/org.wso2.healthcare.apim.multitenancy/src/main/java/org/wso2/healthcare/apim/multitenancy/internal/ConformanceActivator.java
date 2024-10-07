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

package org.wso2.healthcare.apim.multitenancy.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;
import org.wso2.healthcare.apim.multitenancy.OpenHealthcareTenantServiceCreator;

/**
 * This Class will register the tenant service creator as an OSGI bundle
 */
// TODO: Move to SCR annotations
public class ConformanceActivator implements BundleActivator {

    public void start(BundleContext bundleContext) {

        bundleContext.registerService(Axis2ConfigurationContextObserver.class, new OpenHealthcareTenantServiceCreator(),
                null);
    }

    public void stop(BundleContext bundleContext) {

    }
}
