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

import java.util.Arrays;
import java.util.List;

import static org.wso2.healthcare.apim.core.utils.ScopeMgtUtil.CLIENT_FACING_SCOPE_DELIMITER;
import static org.wso2.healthcare.apim.core.utils.ScopeMgtUtil.INTERNAL_SCOPE_DELIMITER;

/**
 * Scope Mgt Constant class
 */
public class ScopeMgtConstants {

    public static final String CLIENT_FACING_PATIENT_SCOPE_PREFIX = "patient".concat(CLIENT_FACING_SCOPE_DELIMITER);
    public static final String CLIENT_FACING_USER_SCOPE_PREFIX = "user".concat(CLIENT_FACING_SCOPE_DELIMITER);
    public static final String INTERNAL_PATIENT_SCOPE_PREFIX = "patient".concat(INTERNAL_SCOPE_DELIMITER);
    public static final String INTERNAL_USER_SCOPE_PREFIX = "user".concat(INTERNAL_SCOPE_DELIMITER);
    public static final List<String> ALLOWED_SCOPE_SUFFIXES = Arrays.asList("c", "r", "u", "d", "s");
    public static final String PATIENT_LAUNCH_SCOPE = "launch/patient";

}