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

import org.wso2.carbon.apimgt.impl.notifier.events.APIEvent;
import org.wso2.healthcare.apim.notifier.exception.OpenHealthcareNotifierExecutorException;

/**
 * This class will extend the AbstractExecutor abstract class and
 * responsible to handle FHIR conformance or metadata API related operations
 * <p>
 * TODO: Need to implement the FHIR conformance using this native implementation method
 * ISSUE: https://github.com/wso2-enterprise/open-healthcare/issues/1093
 */
public class FHIRConformanceExecutor extends AbstractExecutor {
    public FHIRConformanceExecutor(APIEvent apiId) throws OpenHealthcareNotifierExecutorException {
        super(apiId);
    }

    @Override
    public void executeCreateFlow() {

    }

    @Override
    public void executeUpdateFlow() {

    }

    @Override
    public void executeDeleteFlow() {

    }

    @Override
    public void executeLifecycleChangeFlow() {

    }
}
