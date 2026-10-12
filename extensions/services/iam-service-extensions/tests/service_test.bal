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
import ballerina/test;

final http:Client iamClient = check new ("http://localhost:9093");

// ─── Helper: build a minimal pre-issue-access-token payload ──────────────────

function buildPayload(string[] scopes, string grantType, string sessionKey, string? userId) returns json {
    map<json> sessionMap = {"sessionDataKeyConsent": sessionKey};
    map<json> event = {
        "request": {
            "grantType": grantType,
            "clientId": "test-client",
            "scopes": scopes
        },
        "tenant": {"id": "1", "name": "carbon.super"},
        "accessToken": {
            "tokenType": "JWT",
            "claims": [],
            "scopes": scopes
        },
        "session": sessionMap
    };
    if userId is string {
        event["user"] = {"id": userId};
    }
    return {
        "actionType": "PRE_ISSUE_ACCESS_TOKEN",
        "event": event
    };
}

// ─── Test: scope approval from IS consent management ─────────────────────────

@test:Config {}
function testScopeApprovalFromIsConsent() returns error? {
    // mockApprovedScopes = ["patient/Patient.read", "patient/Observation.read", "openid"]
    // requesting patient/Patient.read → should be in final token
    json payload = buildPayload(
        ["patient/Patient.read", "patient/Observation.read", "openid"],
        "authorization_code",
        "consent-key-001",
        ()
    );

    http:Response response = check iamClient->post("/pre-issue-access-token", payload);
    test:assertEquals(response.statusCode, 200);

    json body = check response.getJsonPayload();
    map<json> bodyMap = check body.ensureType();
    test:assertEquals(bodyMap["actionStatus"], "SUCCESS");

    // Operations should contain add scope entries for the approved scopes
    json[] ops = check (bodyMap["operations"] ?: []).ensureType();
    boolean hasAddScope = false;
    foreach json op in ops {
        map<json> opMap = check op.ensureType();
        if opMap["op"] == "add" && opMap["path"].toString().includes("/accessToken/scopes/") {
            hasAddScope = true;
        }
    }
    test:assertTrue(hasAddScope);
}

// ─── Test: patient claim injected from OH_patient/ in approved scopes ─────────

@test:Config {}
function testPatientClaimInjection() returns error? {
    // Add OH_patient/ to mockApprovedScopes for this test
    mockApprovedScopes = ["patient/Patient.read", "openid", "OH_patient/patient-456"];

    json payload = buildPayload(
        ["patient/Patient.read", "openid"],
        "authorization_code",
        "consent-key-002",
        ()
    );

    http:Response response = check iamClient->post("/pre-issue-access-token", payload);
    test:assertEquals(response.statusCode, 200);

    json body = check response.getJsonPayload();
    map<json> bodyMap = check body.ensureType();
    test:assertEquals(bodyMap["actionStatus"], "SUCCESS");

    json[] ops = check (bodyMap["operations"] ?: []).ensureType();
    boolean hasPatientClaim = false;
    foreach json op in ops {
        map<json> opMap = check op.ensureType();
        if opMap["op"] == "add" && opMap["path"].toString() == "/accessToken/claims/-" {
            json val = opMap["value"] ?: {};
            if val is map<json> && val["name"] == "patient" && val["value"] == "patient-456" {
                hasPatientClaim = true;
            }
        }
    }
    test:assertTrue(hasPatientClaim);

    // Reset
    mockApprovedScopes = ["patient/Patient.read", "patient/Observation.read", "openid"];
}

// ─── Test: system/* blocked for non-client_credentials ────────────────────────

@test:Config {}
function testSystemScopeGrantTypeEnforcement() returns error? {
    mockApprovedScopes = ["system/Patient.read", "openid"];

    json payload = buildPayload(
        ["system/Patient.read", "openid"],
        "authorization_code",  // NOT client_credentials
        "consent-key-003",
        ()
    );

    http:Response response = check iamClient->post("/pre-issue-access-token", payload);
    test:assertEquals(response.statusCode, 200);

    json body = check response.getJsonPayload();
    map<json> bodyMap = check body.ensureType();
    test:assertEquals(bodyMap["actionStatus"], "SUCCESS");

    // system/Patient.read should NOT be added (blocked for authorization_code)
    json[] ops = check (bodyMap["operations"] ?: []).ensureType();
    foreach json op in ops {
        map<json> opMap = check op.ensureType();
        if opMap["op"] == "add" {
            string opVal = opMap["value"].toString();
            test:assertFalse(opVal.startsWith("system/"), string `system/ scope should not be in token: ${opVal}`);
        }
    }

    // Reset
    mockApprovedScopes = ["patient/Patient.read", "patient/Observation.read", "openid"];
}

// ─── Test: no consent found → SUCCESS with no scope operations ───────────────

@test:Config {}
function testNoConsentFound() returns error? {
    mockConsentId = "";  // Simulate no consent in IS

    json payload = buildPayload(
        ["patient/Patient.read"],
        "authorization_code",
        "unknown-key",
        ()
    );

    http:Response response = check iamClient->post("/pre-issue-access-token", payload);
    test:assertEquals(response.statusCode, 200);

    json body = check response.getJsonPayload();
    map<json> bodyMap = check body.ensureType();
    test:assertEquals(bodyMap["actionStatus"], "SUCCESS");

    // When no consent found, operations should be empty (pass-through)
    json[] ops = check (bodyMap["operations"] ?: []).ensureType();
    test:assertEquals(ops.length(), 0);

    // Reset
    mockConsentId = "test-consent-id";
}

// ─── Test: alwaysAllowedScopes (openid) passes through regardless ─────────────

@test:Config {}
function testAlwaysAllowedScopes() returns error? {
    // approved scopes don't include openid — but alwaysAllowedScopes does
    mockApprovedScopes = ["patient/Patient.read"];

    json payload = buildPayload(
        ["patient/Patient.read", "openid"],
        "authorization_code",
        "consent-key-005",
        ()
    );

    http:Response response = check iamClient->post("/pre-issue-access-token", payload);
    test:assertEquals(response.statusCode, 200);

    json body = check response.getJsonPayload();
    map<json> bodyMap = check body.ensureType();
    test:assertEquals(bodyMap["actionStatus"], "SUCCESS");

    json[] ops = check (bodyMap["operations"] ?: []).ensureType();
    boolean hasOpenid = false;
    foreach json op in ops {
        map<json> opMap = check op.ensureType();
        if opMap["op"] == "add" && opMap["value"] == "openid" {
            hasOpenid = true;
        }
    }
    test:assertTrue(hasOpenid);

    // Reset
    mockApprovedScopes = ["patient/Patient.read", "patient/Observation.read", "openid"];
}

// ─── Tests: consent lookup by user id and client_id (IS sends no sessionDataKeyConsent) ──

// Posts a pre-issue-access-token event without a session object and returns the response body.
function postWithoutSession(string grantType, string? userId, string[] scopes) returns [int, map<json>]|error {
    map<json> event = {
        "request": {"grantType": grantType, "clientId": "test-client", "scopes": scopes},
        "tenant": {"id": "1", "name": "carbon.super"},
        "accessToken": {"tokenType": "JWT", "claims": [], "scopes": scopes}
    };
    if userId is string {
        event["user"] = {"id": userId};
    }
    http:Response response = check iamClient->post("/pre-issue-access-token", {"actionType": "PRE_ISSUE_ACCESS_TOKEN", "event": event});
    json body = check response.getJsonPayload();
    return [response.statusCode, check body.ensureType()];
}

function resetConsentMock() {
    mockConsentId = "test-consent-id";
    mockConsentState = "ACTIVE";
    mockConsentListStatus = 200;
    mockApprovedScopes = ["patient/Patient.read", "patient/Observation.read", "openid"];
    lastConsentListQuery = "";
}

// Returns the value of the claim added to the token, or () if it was not added.
function findAddedClaim(map<json> body, string claimName) returns json? {
    json[] ops = <json[]>(body["operations"] ?: []);
    foreach json op in ops {
        map<json> opMap = <map<json>>op;
        if opMap["op"] == "add" && opMap["path"].toString() == "/accessToken/claims/-" {
            json val = opMap["value"] ?: {};
            if val is map<json> && val["name"] == claimName {
                return val["value"];
            }
        }
    }
    return ();
}

function findAddedScopes(map<json> body) returns string[] {
    string[] scopes = [];
    json[] ops = <json[]>(body["operations"] ?: []);
    foreach json op in ops {
        map<json> opMap = <map<json>>op;
        if opMap["op"] == "add" && opMap["path"].toString() == "/accessToken/scopes/-" {
            scopes.push((opMap["value"] ?: "").toString());
        }
    }
    return scopes;
}

@test:Config {}
function testConsentLookupByUserAndClient() returns error? {
    resetConsentMock();
    [int, map<json>] [status, body] = check postWithoutSession("authorization_code", "user-1", ["patient/Patient.read", "openid"]);
    test:assertEquals(status, 200);
    test:assertEquals(lastConsentListQuery, "user-1|properties.clientId eq test-client");
    test:assertEquals(findAddedClaim(body, "consent_id"), "test-consent-id");
}

@test:Config {}
function testLookupByUserAndClientFiltersUnapprovedScopes() returns error? {
    resetConsentMock();
    mockApprovedScopes = ["patient/Patient.r", "openid"];
    [int, map<json>] [status, body] = check postWithoutSession(
        "authorization_code", "user-1", ["patient/Patient.r", "patient/Observation.r", "openid"]);
    test:assertEquals(status, 200);
    string[] scopes = findAddedScopes(body);
    test:assertTrue(scopes.indexOf("patient/Patient.r") is int, "approved scope must be kept");
    test:assertTrue(scopes.indexOf("openid") is int, "approved scope must be kept");
    test:assertTrue(scopes.indexOf("patient/Observation.r") is (), "scope the user did not approve must be dropped");
    resetConsentMock();
}

@test:Config {}
function testLookupByUserAndClientNoConsentPassesThrough() returns error? {
    resetConsentMock();
    mockConsentId = "";
    [int, map<json>] [status, body] = check postWithoutSession("authorization_code", "user-1", ["patient/Patient.read"]);
    test:assertEquals(status, 200);
    test:assertEquals(lastConsentListQuery, "user-1|properties.clientId eq test-client");
    test:assertEquals(body["actionStatus"], "SUCCESS");
    test:assertEquals(findAddedClaim(body, "consent_id"), ());
    test:assertEquals(findAddedScopes(body).length(), 0, "no consent found: token is passed through untouched");
    resetConsentMock();
}

@test:Config {}
function testLookupByUserAndClientIgnoresInactiveConsent() returns error? {
    resetConsentMock();
    mockConsentState = "REVOKED";
    [int, map<json>] [status, body] = check postWithoutSession("authorization_code", "user-1", ["patient/Patient.read"]);
    test:assertEquals(status, 200);
    test:assertEquals(findAddedClaim(body, "consent_id"), ());
    resetConsentMock();
}

@test:Config {}
function testLookupByUserAndClientAppliesToRefreshGrant() returns error? {
    resetConsentMock();
    [int, map<json>] [status, body] = check postWithoutSession("refresh_token", "user-1", ["patient/Patient.read", "openid"]);
    test:assertEquals(status, 200);
    test:assertEquals(lastConsentListQuery, "user-1|properties.clientId eq test-client");
    test:assertEquals(findAddedClaim(body, "consent_id"), "test-consent-id");
}

@test:Config {}
function testNoLookupForClientCredentials() returns error? {
    resetConsentMock();
    [int, map<json>] [status, _] = check postWithoutSession("client_credentials", "user-1", ["openid"]);
    test:assertEquals(status, 200);
    test:assertEquals(lastConsentListQuery, "", "client_credentials tokens are not tied to a user consent");
}

@test:Config {}
function testNoLookupWithoutUser() returns error? {
    resetConsentMock();
    [int, map<json>] [status, body] = check postWithoutSession("authorization_code", (), ["openid"]);
    test:assertEquals(status, 200);
    test:assertEquals(lastConsentListQuery, "", "no user id in the event: nothing to look up");
    test:assertEquals(findAddedClaim(body, "consent_id"), ());
}

@test:Config {}
function testSessionKeyTakesPrecedenceOverUserAndClient() returns error? {
    resetConsentMock();
    json payload = buildPayload(["patient/Patient.read", "openid"], "authorization_code", "key-xyz", "user-1");
    http:Response response = check iamClient->post("/pre-issue-access-token", payload);
    test:assertEquals(response.statusCode, 200);
    test:assertEquals(lastConsentListQuery, "|properties.sessionDataKeyConsent eq key-xyz",
            "when IS does send the session key it is used and the user/client lookup is skipped");
}

@test:Config {}
function testLookupByUserAndClientConsentApiFailure() returns error? {
    resetConsentMock();
    mockConsentListStatus = 500;
    http:Response response = check iamClient->post("/pre-issue-access-token", {
        "actionType": "PRE_ISSUE_ACCESS_TOKEN",
        "event": {
            "request": {"grantType": "authorization_code", "clientId": "test-client", "scopes": ["openid"]},
            "tenant": {"id": "1", "name": "carbon.super"},
            "accessToken": {"tokenType": "JWT", "claims": [], "scopes": ["openid"]},
            "user": {"id": "user-1"}
        }
    });
    test:assertEquals(response.statusCode, 500);
    json body = check response.getJsonPayload();
    map<json> bodyMap = check body.ensureType();
    test:assertEquals(bodyMap["actionStatus"], "ERROR");
    resetConsentMock();
}
