// Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import ballerina/http;

// Mock of the WSO2 IS consent management API (v2) — serves as isBaseUrl in tests
listener http:Listener mockConsentListener = new (9296);

// Set to a non-empty string to simulate a consent being found for the sessionDataKeyConsent filter
string mockConsentId = "test-consent-id";

// State returned for the consent
string mockConsentState = "ACTIVE";

// HTTP status returned by the consent list call (set to 500 to simulate a consent API failure)
int mockConsentListStatus = 200;

// Query (userId + filter) of the last list call, for asserting how the consent was looked up
string lastConsentListQuery = "";

// Scopes stored in the `approvedScopes` consent property returned by GET /consents/{id}
string[] mockApprovedScopes = ["patient/Patient.read", "patient/Observation.read", "openid"];

service / on mockConsentListener {

    // GET /api/identity/consent-mgt/v2.0/consents?filter=properties.sessionDataKeyConsent eq <value>
    resource function get api/identity/consent\-mgt/v2\.0/consents(string? filter, string? state, string? userId) returns http:Response {
        lastConsentListQuery = string `${userId ?: ""}|${filter ?: ""}`;
        http:Response response = new;
        if mockConsentListStatus != 200 {
            response.statusCode = mockConsentListStatus;
            response.setJsonPayload({"code": "CM_00010", "message": "Internal server error"});
            return response;
        }
        response.statusCode = 200;
        if mockConsentId == "" {
            response.setJsonPayload({"totalResults": 0, "Consents": []});
        } else {
            response.setJsonPayload({
                "totalResults": 1,
                "Consents": [{"id": mockConsentId, "subjectId": "test-user", "serviceId": "smart-on-fhir", "state": mockConsentState}]
            });
        }
        return response;
    }

    // GET /api/identity/consent-mgt/v2.0/consents/{id}
    resource function get api/identity/consent\-mgt/v2\.0/consents/[string consentId]() returns http:Response {
        http:Response response = new;
        response.statusCode = 200;
        response.setJsonPayload({
            "id": consentId,
            "subjectId": "test-user",
            "serviceId": "smart-on-fhir",
            "state": mockConsentState,
            "purposes": [],
            "properties": {
                "sessionDataKeyConsent": "consent-key",
                "approvedScopes": string:'join(" ", ...mockApprovedScopes)
            }
        });
        return response;
    }
}
