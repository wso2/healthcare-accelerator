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

package org.wso2.healthcare.apim.consentmgt.mediation.util;

public class Constants {

    public static final String ALL_RESOURCE_FIELDS = "ALL_RESOURCE_FIELDS";
    public static final String X_WSO2_CONSENT_DECISION = "X-WSO2-ConsentDecision";
    public static final String API_AUTH_CONTEXT_PROP_NAME = "__API_AUTH_CONTEXT";
    public static final String FHIR_RESOURCE = "FHIR_RESOURCE";
    public static final String FHIR_PATH = "FHIR_PATH";
    public static final String VALID_UNTIL = "VALID_UNTIL";
    public static final String INDEFINITE = "INDEFINITE";

    public static class Policy {
        public static final String DEFAULT_POLICY_TYPE = "OPTOUT";
        public static final String DEFAULT_OPT_IN = "https://terminology.hl7.org/CodeSystem/v3-ActCode#OPTIN";
        public static final String DEFAULT_OPT_OUT = "https://terminology.hl7.org/CodeSystem/v3-ActCode#OPTOUT";
        public static final String PERMIT = "PERMIT";
        public static final String DENY = "DENY";
    }

    public static class PII {
        public static final String TYPE_IDENTIFIER = "--TYPE:";
        public static final String VALUE_IDENTIFIER = "--VALUE:";
    }

    public static final String PERMIT = "PERMIT";
    public static final String DENY = "DENY";
    public static final String VALUE = "VALUE:";
}
