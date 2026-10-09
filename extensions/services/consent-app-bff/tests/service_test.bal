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
import ballerina/jwt;
import ballerina/test;

// HTTP client that calls the BFF under test (same port as production, configured in tests/Config.toml)
final http:Client bffClient = check new ("http://localhost:9092");

// ─── Mock functions ───────────────────────────────────────────────────────────

// Mocks callIdp — intercepts OauthConsentKey calls based on the session key in the path.
@test:Mock {functionName: "callIdp"}
function mockCallIdp(string path) returns json|error {
    if path.includes("practitioner-key") {
        return {
            "application": "TestApp",
            "scope": "patient/Observation.read patient/Patient.read openid OH_launch/launch-abc",
            "loggedInUser": "practitioner@example.com",
            "mandatoryClaims": "0_Email"
        };
    }
    // Default: non-practitioner scope flow
    return {
        "application": "TestApp",
        "scope": "patient/Observation.read patient/Patient.read openid",
        "loggedInUser": "testuser@example.com",
        "mandatoryClaims": "0_Email"
    };
}

// Mocks getScimUser — returns practitioner or regular user based on userId.
@test:Mock {functionName: "getScimUser"}
function mockGetScimUser(string userId) returns ScimUserInfo|error {
    if userId == "practitioner@example.com" {
        return {
            id: "prac-uuid-001",
            displayName: "Dr. Jane Smith",
            email: "practitioner@example.com",
            fhirUser: "Practitioner/prac-001"
        };
    }
    return {
        id: "user-uuid-001",
        displayName: "John Smith",
        email: "testuser@example.com",
        fhirUser: ""
    };
}

// Mocks getScimPatients — returns a fixed patient list.
@test:Mock {functionName: "getScimPatients"}
function mockGetScimPatients() returns ConsentPatient[]|error {
    return [
        {id: "patient-001", name: "Jane Doe", fhirUser: "Patient/patient-001", mrn: "MRN-001"},
        {id: "patient-002", name: "Bob Brown", fhirUser: "Patient/patient-002"}
    ];
}

// ─── Helper: generate a valid consent token for submit-consent tests ──────────

function generateTestConsentToken(string loggedInUser, string application, string sdkc, string clientId = "test-client-id") returns string|error {
    jwt:IssuerConfig issuerConfig = {
        issuer: "consent-app-bff",
        audience: "consent-app",
        username: loggedInUser,
        expTime: 600,
        signatureConfig: {
            algorithm: jwt:HS256,
            config: "test-secret-key-must-be-at-least-32-chars"
        },
        customClaims: {"app": application, "cid": clientId, "sdkc": sdkc}
    };
    return check jwt:issue(issuerConfig);
}

// ─── Test: get-consent-data (scope flow, non-practitioner) ───────────────────

@test:Config {}
function testGetConsentDataScopeFlow() returns error? {
    http:Response response = check bffClient->get(
        "/get-consent-data?sessionDataKeyConsent=test-session-key&spId=test-sp"
    );

    test:assertEquals(response.statusCode, 200);
    json body = check response.getJsonPayload();
    map<json> bodyMap = check body.ensureType();

    test:assertEquals(bodyMap["flow"], "scope");
    test:assertEquals(bodyMap["spId"], "test-sp");
    test:assertNotEquals(bodyMap["consentToken"], ());
    test:assertEquals(bodyMap["isPractitioner"], false);
    test:assertEquals(bodyMap["mandatoryClaims"], "0_Email");

    // openid should be filtered from visible scopes (alwaysAllowedScopes)
    json[] scopes = check (bodyMap["scopes"] ?: []).ensureType();
    foreach json s in scopes {
        test:assertNotEquals(s.toString(), "openid");
    }

    // OH_launch scopes should not be in visible scopes
    foreach json s in scopes {
        test:assertFalse(s.toString().startsWith("OH_launch/"));
    }
}

// ─── Test: get-consent-data (scope flow, practitioner with patients) ──────────

@test:Config {}
function testGetConsentDataPractitionerFlow() returns error? {
    http:Response response = check bffClient->get(
        "/get-consent-data?sessionDataKeyConsent=practitioner-key&spId=test-sp"
    );

    test:assertEquals(response.statusCode, 200);
    json body = check response.getJsonPayload();
    map<json> bodyMap = check body.ensureType();

    test:assertEquals(bodyMap["flow"], "scope");
    test:assertEquals(bodyMap["isPractitioner"], true);

    // Patients should be included for practitioners
    json[] patients = check (bodyMap["patients"] ?: []).ensureType();
    test:assertTrue(patients.length() > 0);

    // OH_launch scope should be in hiddenScopes
    json[] hiddenScopes = check (bodyMap["hiddenScopes"] ?: []).ensureType();
    boolean hasLaunchScope = false;
    foreach json s in hiddenScopes {
        if s.toString().startsWith("OH_launch/") {
            hasLaunchScope = true;
        }
    }
    test:assertTrue(hasLaunchScope);
}

// ─── Test: get-consent-data (scope flow, singleConsentPerUser with existing) ──

@test:Config {}
function testGetConsentDataSingleConsentPerUser() returns error? {
    mockHasExistingConsent = true;
    http:Response response = check bffClient->get(
        "/get-consent-data?sessionDataKeyConsent=test-session-key&spId=test-sp"
    );
    mockHasExistingConsent = false;

    test:assertEquals(response.statusCode, 200);
    map<json> bodyMap = check (check response.getJsonPayload()).ensureType();
    test:assertEquals(bodyMap["existingConsentId"], "existing-consent-id");
    test:assertEquals(bodyMap["previouslyApprovedScopes"], ["patient/Observation.read", "patient/Patient.read"]);
    test:assertEquals(bodyMap["consentExpiryOption"], "24h");
}

// ─── Test: submit-consent (scope flow, approved) ─────────────────────────────

@test:Config {}
function testSubmitConsentScopeApprove() returns error? {
    string consentToken = check generateTestConsentToken(
        "testuser@example.com", "TestApp", "submit-test-key"
    );
    mockLastCreatedConsent = ();
    mockRevokedConsentIds = [];
    mockHasExistingConsent = true;

    json requestBody = {
        "consentToken": consentToken,
        "sessionDataKeyConsent": "submit-test-key",
        "spId": "test-sp",
        "approved": true,
        "approvedScopes": ["patient/Observation.read", "patient/Patient.read"],
        "hiddenScopes": [],
        "consentExpiryOption": "24h"
    };

    http:Response response = check bffClient->post("/submit-consent", requestBody);
    mockHasExistingConsent = false;
    // POST resources return 201 Created by default
    test:assertEquals(response.statusCode, 201);

    json body = check response.getJsonPayload();
    map<json> bodyMap = check body.ensureType();
    test:assertEquals(bodyMap["status"], "success");

    // Consent sent to WSO2 IS
    map<json> created = check (mockLastCreatedConsent ?: {}).ensureType();
    test:assertEquals(created["subjectId"], "testuser@example.com");
    test:assertEquals(created["serviceId"], "test-service");
    test:assertEquals(created["purposes"], [{"id": MOCK_SCOPE_PURPOSE_ID, "elements": [{"id": "element-scope-id"}]}]);
    map<json> props = check created.properties.ensureType();
    test:assertEquals(props["sessionDataKeyConsent"], "submit-test-key");
    test:assertEquals(props["approvedScopes"], "patient/Observation.read patient/Patient.read");
    test:assertEquals(props["consentExpiryOption"], "24h");
    // client_id is stored so iam-service-extensions can find the consent in the token flow
    test:assertEquals(props["clientId"], "test-client-id");
    test:assertTrue(created["expiryTime"] is int);

    // singleConsentPerUser: previous active consent is revoked, the new one is kept
    test:assertEquals(mockRevokedConsentIds, ["existing-consent-id"]);
}

// ─── Test: submit-consent (purpose flow, approved) ───────────────────────────

@test:Config {}
function testSubmitConsentPurposeApprove() returns error? {
    string consentToken = check generateTestConsentToken(
        "testuser@example.com", "TestApp", "purpose-submit-key"
    );

    mockLastCreatedConsent = ();

    json requestBody = {
        "consentToken": consentToken,
        "sessionDataKeyConsent": "purpose-submit-key",
        "spId": "test-sp",
        "approved": true,
        "consentedPurposes": [
            {
                "purposeName": "All Health Data Access",
                "consentedElements": ["Patient", "Observation"]
            }
        ]
    };

    http:Response response = check bffClient->post("/submit-consent", requestBody);
    // POST resources return 201 Created by default
    test:assertEquals(response.statusCode, 201);

    json body = check response.getJsonPayload();
    map<json> bodyMap = check body.ensureType();
    test:assertEquals(bodyMap["status"], "success");

    // Only approved elements are bound, by id
    map<json> created = check (mockLastCreatedConsent ?: {}).ensureType();
    test:assertEquals(created["purposes"], [{
        "id": MOCK_HEALTH_PURPOSE_ID,
        "elements": [{"id": "element-patient-id"}, {"id": "element-observation-id"}]
    }]);
    // client_id is stored for the token-flow lookup in the purpose flow as well
    map<json> props = check created.properties.ensureType();
    test:assertEquals(props["clientId"], "test-client-id");
}

// ─── Test: no clientId property when the consent token carries no client_id ──────

@test:Config {}
function testSubmitConsentWithoutClientId() returns error? {
    string consentToken = check generateTestConsentToken(
        "testuser@example.com", "TestApp", "no-cid-key", ""
    );
    mockLastCreatedConsent = ();

    json requestBody = {
        "consentToken": consentToken,
        "sessionDataKeyConsent": "no-cid-key",
        "spId": "test-sp",
        "approved": true,
        "consentedPurposes": [{"purposeName": "All Health Data Access", "consentedElements": ["Patient"]}]
    };
    http:Response response = check bffClient->post("/submit-consent", requestBody);
    test:assertEquals(response.statusCode, 201);

    map<json> created = check (mockLastCreatedConsent ?: {}).ensureType();
    map<json> props = check created.properties.ensureType();
    test:assertFalse(props.hasKey("clientId"), "an empty client_id must not be stored as a property");
}

// ─── Test: submit-consent (deny — no consent API call, immediate success) ────────

@test:Config {}
function testSubmitConsentDeny() returns error? {
    string consentToken = check generateTestConsentToken(
        "testuser@example.com", "TestApp", "deny-test-key"
    );

    json requestBody = {
        "consentToken": consentToken,
        "sessionDataKeyConsent": "deny-test-key",
        "spId": "test-sp",
        "approved": false
    };

    http:Response response = check bffClient->post("/submit-consent", requestBody);
    // POST resources return 201 Created by default
    test:assertEquals(response.statusCode, 201);

    json body = check response.getJsonPayload();
    map<json> bodyMap = check body.ensureType();
    test:assertEquals(bodyMap["status"], "success");
}

// ─── Test: submit-consent (invalid/tampered JWT) ─────────────────────────────

@test:Config {}
function testSubmitConsentInvalidToken() returns error? {
    json requestBody = {
        "consentToken": "invalid.jwt.token",
        "sessionDataKeyConsent": "some-key",
        "spId": "test-sp",
        "approved": true,
        "approvedScopes": ["patient/Observation.read"]
    };

    http:Response response = check bffClient->post("/submit-consent", requestBody);
    // BFF should return an error status (500 or similar) for invalid JWT
    test:assertTrue(response.statusCode >= 400);
}

// ─── Test: submit-consent (session key mismatch) ─────────────────────────────

@test:Config {}
function testSubmitConsentSessionMismatch() returns error? {
    // Token signed for "real-key" but submitted with "different-key"
    string consentToken = check generateTestConsentToken(
        "testuser@example.com", "TestApp", "real-key"
    );

    json requestBody = {
        "consentToken": consentToken,
        "sessionDataKeyConsent": "different-key",
        "spId": "test-sp",
        "approved": true,
        "approvedScopes": ["patient/Observation.read"]
    };

    http:Response response = check bffClient->post("/submit-consent", requestBody);
    test:assertTrue(response.statusCode >= 400);
}
