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

package org.wso2.healthcare.consentmgt.core;

public class OHConsentConstants {

    public enum ErrorMessages {
        ERROR_CODE_DATABASE_CONNECTION("OH_CM_00001", "Error when getting a database connection object from the Consent"
                + " data source."), ERROR_CODE_MAPPING_ID_UNAVAILABLE("OH_CM_00002",
                "App to purpose mapping id is not available."), ERROR_CODE_RETRIEVE_CONSENT_PURPOSES("OH_CM_00003",
                "Error occurred while fetching consent purposes."), ERROR_CODE_ADD_CONSENT_PURPOSE("OH_CM_00004",
                "Error occurred while adding consent purpose."), ERROR_CODE_RETRIEVE_PII_CATEGORIES("OH_CM_00005",
                "Error occurred while fetching pii categories."), ERROR_CODE_UPDATE_PII_CATEGORIES("OH_CM_00006",
                "Error occurred while updating pii categories."), ERROR_CODE_ADD_APP_SPECIFIC_PURPOSE("OH_CM_00007",
                "Error occurred while adding app specific consent purpose."), ERROR_CODE_PURPOSE_IS_ASSOCIATED("OH_CM_00008",
                "Consent purpose is associated with one or more receipts.");

        private final String code;
        private final String message;

        ErrorMessages(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return code + " : " + message;
        }
    }
}
