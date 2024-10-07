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

package org.wso2.healthcare.apim.notifier.executors;

import org.apache.commons.lang3.StringUtils;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.impl.notifier.events.APIEvent;
import org.wso2.carbon.apimgt.rest.api.common.RestApiCommonUtil;
import org.wso2.healthcare.apim.notifier.exception.OpenHealthcareNotifierExecutorException;
import org.wso2.healthcare.apim.notifier.utils.NotifierUtils;

import static org.wso2.healthcare.apim.notifier.Constants.API_DELETE_EVENT;

/**
 * This is abstract class implements the Executor interface and provide the common implementation logics for
 * CDS executor and the FHIR executor classes
 */
public abstract class AbstractExecutor implements Executor {

    private final APIProvider apiProvider;
    private final int tenantId;
    private final String apiId;
    private API api;

    public AbstractExecutor(APIEvent event) throws OpenHealthcareNotifierExecutorException {
        this.apiId = event.getUuid();
        try {
            this.apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
        } catch (APIManagementException ex) {
            String msg = "Error occurred while retrieve the logged in user provider";
            throw new OpenHealthcareNotifierExecutorException(msg, ex);
        }
        this.tenantId = NotifierUtils.getTenantId();

        // For delete API event we receive the event notification after the specific API got deleted
        // so, we can retrieve the API object for API delete event
        if (!StringUtils.equals(event.getType(), API_DELETE_EVENT)) {
            api = NotifierUtils.retrieveAPI(apiId);
        }
    }

    public APIProvider getApiProvider() {
        return apiProvider;
    }

    public int getTenantId() {
        return tenantId;
    }

    public String getApiId() {
        return apiId;
    }

    public API getApi() {
        return api;
    }

}
