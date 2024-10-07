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

import org.wso2.healthcare.apim.notifier.exception.OpenHealthcareNotifierExecutorException;

/**
 * This is the main interface define the core functionalities of the Executor, while registering a new Executor class
 * this interface needs to be implemented
 */
public interface Executor {

    /**
     * This method will do the custom execution logic for create events, like API or application create event
     *
     * @throws OpenHealthcareNotifierExecutorException
     */
    void executeCreateFlow() throws OpenHealthcareNotifierExecutorException;

    /**
     * This method will do the custom execution logic for update events, like API or application update event
     *
     * @throws OpenHealthcareNotifierExecutorException
     */
    void executeUpdateFlow() throws OpenHealthcareNotifierExecutorException;

    /**
     * This method will do the custom execution logic for delete events, like API or application delete event
     *
     * @throws OpenHealthcareNotifierExecutorException
     */
    void executeDeleteFlow() throws OpenHealthcareNotifierExecutorException;

    /**
     * This method will do the custom execution logic for life-cycle change events, like API or application life-cycle change event
     *
     * @throws OpenHealthcareNotifierExecutorException
     */
    void executeLifecycleChangeFlow() throws OpenHealthcareNotifierExecutorException;
}
