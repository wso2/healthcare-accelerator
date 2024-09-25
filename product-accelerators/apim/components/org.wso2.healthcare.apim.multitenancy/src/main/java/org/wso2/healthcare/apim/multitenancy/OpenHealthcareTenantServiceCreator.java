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

package org.wso2.healthcare.apim.multitenancy;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.mediation.initializer.ServiceBusConstants;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Axis2ConfigurationContextObserver implementation for Open-Healthcare to create system Synapse artifacts
 */
public class OpenHealthcareTenantServiceCreator extends AbstractAxis2ConfigurationContextObserver {

    private static final Log LOG = LogFactory.getLog(OpenHealthcareTenantServiceCreator.class);

    @Override
    public void createdConfigurationContext(ConfigurationContext configContext) {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        final Lock lock = getLock(configContext.getAxisConfiguration());

        try {
            lock.lock();
            // creates the synapse configuration directory hierarchy if not exists
            // useful at the initial tenant creation
            File tenantAxis2Repo = new File(configContext.getAxisConfiguration().getRepository().getFile());
            File synapseConfigsDir = new File(tenantAxis2Repo, "synapse-configs");
            if (!synapseConfigsDir.exists() && !synapseConfigsDir.mkdirs()) {
                LOG.fatal("Couldn't create the synapse-config root on the file system " +
                        "for the tenant domain : " + tenantDomain);
                return;
            }

            File defaultDir = new File(synapseConfigsDir.getPath(), "default");
            if (!defaultDir.exists() && !defaultDir.mkdirs()) {
                LOG.fatal("Couldn't create the " + defaultDir.getPath() + " directory on the file system " +
                        "for the tenant domain : " + tenantDomain);
                return;
            }
            File apiDir = new File(defaultDir.getPath(), "api");
            if (!apiDir.exists() && !apiDir.mkdirs()) {
                LOG.fatal("Couldn't create the " + apiDir.getPath() + " directory on the file system " +
                        "for the tenant domain : " + tenantDomain);
                return;
            }

            // Create meta data api
            try {
                createMetadataAPI(tenantDomain, apiDir.getPath());
            } catch (IOException e) {
                LOG.fatal("Error occurred while creating FHIR Metadata API for tenant : " + tenantDomain, e);
            }
            // Create smart api
            try {
                createSmartAPI(tenantDomain, apiDir.getPath());
            } catch (IOException e) {
                LOG.fatal("Error occurred while creating FHIR Smart API for tenant : " + tenantDomain, e);
            }
        } finally {
            lock.unlock();
        }
    }

    private void createMetadataAPI(String tenantDomain, String filePath) throws IOException {

        MetadataAPIBuilder metadataAPIBuilder = new MetadataAPIBuilder();
        SystemAPI metaAPI = metadataAPIBuilder.build(tenantDomain);

        File metaAPIFile = new File(filePath, metaAPI.getName() + ".xml");
        if (!metaAPIFile.exists()) {
            FileUtils.writeByteArrayToFile(metaAPIFile, metaAPI.getConfigDefinition().getBytes());
            LOG.info("FHIR Metadata API created for tenant : " + tenantDomain + " path :" + metaAPIFile.getPath());
        }
    }

    private void createSmartAPI(String tenantDomain, String filePath) throws IOException {

        SMARTConfigAPIBuilder smartConfigAPIBuilder = new SMARTConfigAPIBuilder();
        SystemAPI smartAPI = smartConfigAPIBuilder.build(tenantDomain);

        File smartAPIFile = new File(filePath, smartAPI.getName() + ".xml");
        if (!smartAPIFile.exists()) {
            FileUtils.writeByteArrayToFile(smartAPIFile, smartAPI.getConfigDefinition().getBytes());
            LOG.info("FHIR Smart API created for tenant : " + tenantDomain + " path :" + smartAPIFile.getPath());
        }
    }

    protected Lock getLock(AxisConfiguration axisConfig) {

        Parameter param = axisConfig.getParameter(ServiceBusConstants.SYNAPSE_CONFIG_LOCK);
        if (param != null) {
            return (Lock) param.getValue();
        } else {
            LOG.warn(ServiceBusConstants.SYNAPSE_CONFIG_LOCK + " is null, Recreating a new lock");
            Lock lock = new ReentrantLock();
            try {
                axisConfig.addParameter(ServiceBusConstants.SYNAPSE_CONFIG_LOCK, lock);
                return lock;
            } catch (AxisFault axisFault) {
                LOG.error("Error while setting " + ServiceBusConstants.SYNAPSE_CONFIG_LOCK, axisFault);
            }
        }
        return null;
    }
}
