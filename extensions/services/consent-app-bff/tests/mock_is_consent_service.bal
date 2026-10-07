// Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;
import ballerina/log;

// In-process mock of WSO2 IS used during bal test: the OAuth2 token endpoint and the
// consent management API v2 (/api/identity/consent-mgt/v2.0) endpoints called by consent-app-bff.

listener http:Listener mockIsListener = new (9196);

boolean mockHasExistingConsent = false;

// Captured by the mock so tests can assert on what the BFF sent.
json? mockLastCreatedConsent = ();
string[] mockRevokedConsentIds = [];

const MOCK_SCOPE_PURPOSE_ID = "purpose-scope-id";
const MOCK_HEALTH_PURPOSE_ID = "purpose-health-id";

service /oauth2 on mockIsListener {

    // POST /oauth2/token — client credentials grant
    resource function post token() returns json {
        return {"access_token": "mock-access-token", "token_type": "Bearer", "expires_in": 3600};
    }
}

service /api/identity/consent\-mgt/v2\.0 on mockIsListener {

    // GET /purposes?filter=name eq <name>
    resource function get purposes(string? filter) returns json {
        string name = filter is string ? re `^name eq `.replace(filter, "") : "";
        if name == "SMART Scope Authorization" {
            return {"totalResults": 1, "Purposes": [{"id": MOCK_SCOPE_PURPOSE_ID, "name": name}]};
        }
        if name == "All Health Data Access" {
            return {"totalResults": 1, "Purposes": [{"id": MOCK_HEALTH_PURPOSE_ID, "name": name}]};
        }
        return {"totalResults": 0, "Purposes": []};
    }

    // GET /purposes/{id} — purpose with the elements of its latest version
    resource function get purposes/[string purposeId]() returns http:Response|json {
        if purposeId == MOCK_SCOPE_PURPOSE_ID {
            return {
                "id": purposeId,
                "name": "SMART Scope Authorization",
                "elements": [{"id": "element-scope-id", "name": "scope-access", "mandatory": true}]
            };
        }
        if purposeId == MOCK_HEALTH_PURPOSE_ID {
            return {
                "id": purposeId,
                "name": "All Health Data Access",
                "description": "Test health data access",
                "elements": [
                    {"id": "element-patient-id", "name": "Patient", "mandatory": false},
                    {"id": "element-observation-id", "name": "Observation", "mandatory": false}
                ]
            };
        }
        http:Response notFound = new;
        notFound.statusCode = 404;
        return notFound;
    }

    // GET /consents?userId=...&relation=SUBJECT&serviceId=...&state=ACTIVE
    resource function get consents(string? userId, string? relation, string? serviceId, string? state, int? 'limit)
            returns json {
        if mockHasExistingConsent {
            return {"totalResults": 1, "Consents": [{"id": "existing-consent-id", "state": "ACTIVE"}]};
        }
        return {"totalResults": 0, "Consents": []};
    }

    // POST /consents — create a consent
    resource function post consents(@http:Payload json payload) returns http:Response {
        log:printInfo("Mock IS: consent created", payload = payload.toString());
        mockLastCreatedConsent = payload;
        http:Response response = new;
        response.statusCode = 201;
        response.setJsonPayload({"id": "new-consent-id", "subjectId": "mock"});
        return response;
    }

    // GET /consents/{id}
    resource function get consents/[string consentId]() returns json {
        return {
            "id": consentId,
            "state": "ACTIVE",
            "purposes": [{
                "id": MOCK_HEALTH_PURPOSE_ID,
                "name": "All Health Data Access",
                "elements": [{"id": "element-patient-id", "name": "Patient"}]
            }],
            "properties": {
                "approvedScopes": "patient/Observation.read patient/Patient.read",
                "consentExpiryOption": "24h"
            }
        };
    }

    // POST /consents/{id}/revoke
    resource function post consents/[string consentId]/revoke() returns http:Response {
        mockRevokedConsentIds.push(consentId);
        http:Response response = new;
        response.statusCode = 204;
        return response;
    }
}
