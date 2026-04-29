// Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the
// License at http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific
// language governing permissions and limitations under the License.

import ballerina/http;
import ballerina/test;

final http:Client bffClient = check new ("http://localhost:9095");

// Mock callAsgardeo to return predictable test data without making real network calls.
@test:Mock {
    functionName: "callAsgardeo"
}
isolated function mockCallAsgardeo(string path) returns json|error {
    return {
        "application": "TestApp",
        "scope": "openid profile",
        "loggedInUser": "alice@example.com"
    };
}

// Helper: call get-consent-data and return the consentToken.
isolated function getConsentToken(string sdkc) returns string|error {
    http:Client c = check new ("http://localhost:9095");
    http:Response resp = check c->get(string `/v1/get-consent-data?sessionDataKeyConsent=${sdkc}&spId=sp1`);
    json body = check resp.getJsonPayload();
    json tokenJson = check body.consentToken;
    return tokenJson.toString();
}

// --- get-consent-data ---

@test:Config {}
function testGetConsentDataHappyPath() returns error? {
    http:Response resp = check bffClient->get("/v1/get-consent-data?sessionDataKeyConsent=sdkc1&spId=sp1");
    test:assertEquals(resp.statusCode, 200);

    json body = check resp.getJsonPayload();
    test:assertEquals(check body.appName, "TestApp");
    test:assertEquals(check body.loggedInUser, "alice@example.com");

    string consentToken = check body.consentToken;
    test:assertTrue(consentToken.length() > 0, "consentToken must be non-empty");
}

// --- submit-consent ---

@test:Config {}
function testSubmitConsentApproveWithValidToken() returns error? {
    string consentToken = check getConsentToken("sdkc-approve");

    json payload = {
        sessionDataKeyConsent: "sdkc-approve",
        spId: "sp1",
        approved: true,
        consentedPurposes: [{purposeName: "All Health Data Access", consentedElements: ["Patient", "Observation"]}],
        consentToken: consentToken
    };
    http:Response resp = check bffClient->post("/v1/submit-consent", payload);
    test:assertEquals(resp.statusCode, 201);

    json body = check resp.getJsonPayload();
    test:assertEquals(check body.status, "success");
}

@test:Config {}
function testSubmitConsentDenyWithValidToken() returns error? {
    string consentToken = check getConsentToken("sdkc-deny");

    json payload = {
        sessionDataKeyConsent: "sdkc-deny",
        spId: "sp1",
        approved: false,
        consentedPurposes: [],
        consentToken: consentToken
    };
    http:Response resp = check bffClient->post("/v1/submit-consent", payload);
    test:assertEquals(resp.statusCode, 201);

    json body = check resp.getJsonPayload();
    test:assertEquals(check body.status, "success");
}

@test:Config {}
function testSubmitConsentRejectsInvalidToken() returns error? {
    json payload = {
        sessionDataKeyConsent: "sdkc1",
        spId: "sp1",
        approved: true,
        consentedPurposes: [],
        consentToken: "invalid.jwt.token"
    };
    http:Response resp = check bffClient->post("/v1/submit-consent", payload);
    test:assertEquals(resp.statusCode, 500, "Invalid consent token must be rejected");
}

@test:Config {}
function testSubmitConsentRejectsSessionMismatch() returns error? {
    string consentToken = check getConsentToken("sdkc-A");

    json payload = {
        sessionDataKeyConsent: "sdkc-B",
        spId: "sp1",
        approved: true,
        consentedPurposes: [],
        consentToken: consentToken   // token bound to sdkc-A, not sdkc-B
    };
    http:Response resp = check bffClient->post("/v1/submit-consent", payload);
    test:assertEquals(resp.statusCode, 500, "Session mismatch must be rejected");
}

// --- bind-consent-to-token ---

@test:Config {}
function testBindConsentToTokenMissingSessionKey() returns error? {
    json payload = {
        actionType: "PRE_ISSUE_ACCESS_TOKEN",
        event: {
            session: {}
        }
    };
    http:Response resp = check bffClient->post("/v1/bind-consent-to-token", payload);
    test:assertEquals(resp.statusCode, 200);

    json body = check resp.getJsonPayload();
    test:assertEquals(check body.actionStatus, "SUCCESS");
    map<json> bodyMap = check body.cloneWithType();
    test:assertFalse(bodyMap.hasKey("operations"), "Expected no operations field when session key is missing");
}

@test:Config {}
function testBindConsentToTokenInjectsConsentId() returns error? {
    json payload = {
        actionType: "PRE_ISSUE_ACCESS_TOKEN",
        event: {
            session: {
                sessionDataKeyConsent: "sdkc1"
            }
        }
    };
    http:Response resp = check bffClient->post("/v1/bind-consent-to-token", payload);
    test:assertEquals(resp.statusCode, 200);

    json body = check resp.getJsonPayload();
    test:assertEquals(check body.actionStatus, "SUCCESS");

    json opsJson = check body.operations;
    json[] operations = check opsJson.cloneWithType();
    test:assertEquals(operations.length(), 1, "Expected exactly one token operation");
    test:assertEquals(check operations[0].op, "add");

    json value = check operations[0].value;
    test:assertEquals(check value.name, "consent_id");
}
