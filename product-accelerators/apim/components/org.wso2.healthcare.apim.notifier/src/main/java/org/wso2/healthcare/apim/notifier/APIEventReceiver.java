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

package org.wso2.healthcare.apim.notifier;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.notifier.ApisNotifier;
import org.wso2.carbon.apimgt.impl.notifier.events.APIEvent;
import org.wso2.carbon.apimgt.impl.notifier.events.Event;
import org.wso2.healthcare.apim.notifier.exception.OpenHealthcareNotifierExecutorException;
import org.wso2.healthcare.apim.notifier.exception.OpenHealthcareNotifierExecutorRuntimeException;
import org.wso2.healthcare.apim.notifier.executors.CDSExecutor;
import org.wso2.healthcare.apim.notifier.executors.Executor;
import org.wso2.healthcare.apim.notifier.executors.FHIRConformanceExecutor;
import org.wso2.healthcare.apim.notifier.utils.NotifierUtils;

import static org.wso2.healthcare.apim.notifier.Constants.*;

/**
 * This class will extend the ApisNotifier capabilities and
 * registered to the org.wso2.carbon.apimgt.impl.notifier.Notifier notifier list
 * through org.wso2.healthcare.apim.notifier.OpenHealthcareNotifierComponent OSGi service
 * when an API related eventS occurred this class also will receive that eventS as notificationS
 */
public class APIEventReceiver extends ApisNotifier {

    private static final Log LOG = LogFactory.getLog(org.wso2.healthcare.apim.notifier.APIEventReceiver.class);

    @Override
    public boolean publishEvent(Event event) {

        if (event instanceof APIEvent) {
            try {
                APIEventProcessor.process((APIEvent) event);
            } catch (OpenHealthcareNotifierExecutorException e) {
                String msg = "Something went wrong while process the event: " + event;
                throw new OpenHealthcareNotifierExecutorRuntimeException(msg, e);
            }
        }
        return false;
    }

    /**
     * This class will process the events received by the org.wso2.healthcare.apim.notifier.APIEventReceiver class
     */
    public static class APIEventProcessor {

        public static void process(APIEvent apiEvent) throws OpenHealthcareNotifierExecutorException {

            if (LOG.isDebugEnabled()) {
                LOG.debug("API event received with type " + apiEvent.toString());
            }
            String apiEventType = apiEvent.getType();

            switch (apiEventType) {
                case API_CREATE_EVENT:
                    handleAPICreateEvent(apiEvent);
                    break;
                case API_DELETE_EVENT:
                    handleAPIDeleteEvent(apiEvent);
                    break;
                case API_LIFECYCLE_CHANGE_EVENT:
                    handleAPILifecycleChangeEvent(apiEvent);
                    break;
                case API_UPDATE_EVENT:
                    handleAPIUpdateEvent(apiEvent);
                    break;
            }
        }

        private static void handleAPICreateEvent(APIEvent apiEvent) throws OpenHealthcareNotifierExecutorException {
            //Custom field defined in API swagger file - x-wso2-healthcare-type
            String apiType = NotifierUtils.retrieveAPITypeFromAPIDefinition(apiEvent.getUuid());

            //NONE_API means there is no such custom filed in the swagger file of the API
            if (!StringUtils.equals(apiType, NONE_API_TYPE)) {
                Executor executor = createExecutorInstance(apiType, apiEvent);
                if (executor != null) {
                    executor.executeCreateFlow();
                }
            }
        }

        private static void handleAPIUpdateEvent(APIEvent apiEvent) throws OpenHealthcareNotifierExecutorException {
            String apiId = apiEvent.getUuid();
            if (NotifierUtils.checkExistenceOfAPIMapping(apiId)) {

                // Retrieve the API type from the registry
                String apiType = NotifierUtils.retrieveProperty(apiId);
                Executor executor = createExecutorInstance(apiType, apiEvent);
                executor.executeUpdateFlow();
            } else if (!StringUtils.equals(NotifierUtils.retrieveAPITypeFromAPIDefinition(apiEvent.getUuid()), NONE_API_TYPE)) {
                String apiType = NotifierUtils.retrieveAPITypeFromAPIDefinition(apiEvent.getUuid());
                Executor executor = createExecutorInstance(apiType, apiEvent);
                if (executor != null) {
                    executor.executeCreateFlow();
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("There is no any API type mapping for the API ID " + apiId
                            + " in the registry location "
                            + HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION);
                }
            }
        }

        private static void handleAPIDeleteEvent(APIEvent apiEvent) throws OpenHealthcareNotifierExecutorException {
            String apiId = apiEvent.getUuid();
            if (NotifierUtils.checkExistenceOfAPIMapping(apiId)) {
                String apiType = NotifierUtils.retrieveProperty(apiId);
                Executor executor = createExecutorInstance(apiType, apiEvent);
                executor.executeDeleteFlow();
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("There is no any API type mapping for the API ID " + apiId
                            + " in the registry location "
                            + HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION);
                }
            }
        }

        private static void handleAPILifecycleChangeEvent(APIEvent apiEvent) throws OpenHealthcareNotifierExecutorException {
            String apiId = apiEvent.getUuid();
            if (NotifierUtils.checkExistenceOfAPIMapping(apiId)) {
                String apiType = NotifierUtils.retrieveProperty(apiId);
                Executor executor = createExecutorInstance(apiType, apiEvent);
                executor.executeLifecycleChangeFlow();
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("There is no any API type mapping for the API ID " + apiId
                            + " in the registry location "
                            + HEALTHCARE_REGISTRY_API_TYPE_MAP_LOCATION);
                }
            }
        }

        /*
         * This method will create and return an instance of particular Executor object based on the API types
         */
        private static Executor createExecutorInstance(String apiType, APIEvent apiEvent)
                throws OpenHealthcareNotifierExecutorException {
            Executor executor = null;
            switch (apiType) {
                case CDS_API_TYPE:
                    executor = new CDSExecutor(apiEvent);
                    break;
                case FHIR_API_TYPE:
                    executor = new FHIRConformanceExecutor(apiEvent);
                    break;
                // For all new extensions add new case here
            }
            return executor;
        }
    }
}
