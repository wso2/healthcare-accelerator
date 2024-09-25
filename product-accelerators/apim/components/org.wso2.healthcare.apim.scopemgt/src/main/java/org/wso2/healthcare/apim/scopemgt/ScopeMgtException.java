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

package org.wso2.healthcare.apim.scopemgt;

import org.wso2.healthcare.apim.core.OpenHealthcareException;

/**
 * Exception class for scope mgt.
 */
public class ScopeMgtException extends OpenHealthcareException {

    public ScopeMgtException(String message) {

        super(message);
    }

    public ScopeMgtException(String message, Throwable cause) {

        super(message, cause);
    }

    public ScopeMgtException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {

        super(message, cause, enableSuppression, writableStackTrace);
    }

}
