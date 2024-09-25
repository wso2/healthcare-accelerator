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

package org.wso2.healthcare.apim.notifier.exception;

import org.wso2.healthcare.apim.core.OpenHealthcareException;

/**
 * Base Class for capturing any type of exception that occurs in the Healthcare notifier component.
 */
public class OpenHealthcareNotifierExecutorException extends OpenHealthcareException {

    public OpenHealthcareNotifierExecutorException() {
    }

    public OpenHealthcareNotifierExecutorException(String message) {
        super(message);
    }

    public OpenHealthcareNotifierExecutorException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenHealthcareNotifierExecutorException(Throwable cause) {
        super(cause);
    }

    public OpenHealthcareNotifierExecutorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
