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
import ballerina/log;
import ballerina/oauth2;

final http:Client openfgcClient = check new (openfgcBaseUrl);

// Looks up consentId for the given sessionDataKeyConsent from OpenFGC attributes endpoint.
// Returns () if no consent found (caller should treat as SUCCESS with no operations).
isolated function getConsentIdBySessionKey(string sessionDataKeyConsent) returns string?|error {
    string path = string `/consents/attributes?key=sessionDataKeyConsent&value=${getEncodedUri(sessionDataKeyConsent)}`;
    log:printDebug("[OpenFGC] GET consent by session key", path = path);
    http:Response response = check openfgcClient->get(path, {
        "org-id": orgId,
        "TPP-client-id": tppClientId
    });
    log:printDebug("[OpenFGC] GET consent by session key response", statusCode = response.statusCode);

    if response.statusCode < 200 || response.statusCode >= 300 {
        string|error bodyResult = response.getTextPayload();
        string body = bodyResult is string ? bodyResult : "";
        log:printError("[OpenFGC] GET consent by session key failed", statusCode = response.statusCode, body = body);
        return error(string `OpenFGC attributes lookup returned ${response.statusCode}: ${body}`);
    }

    json payload = check response.getJsonPayload();
    log:printDebug("[OpenFGC] GET consent by session key body", body = payload.toJsonString());
    if payload is map<json> {
        json consentIds = payload["consentIds"] ?: [];
        if consentIds is json[] && consentIds.length() > 0 {
            json first = consentIds[0];
            if first is string && first != "" {
                log:printDebug("[OpenFGC] Resolved consentId", consentId = first);
                return first;
            }
        }
    }
    log:printDebug("[OpenFGC] No consent found for session key", sessionDataKeyConsent = sessionDataKeyConsent);
    return ();
}

// Fetches approved scopes from the OpenFGC consent record.
// Looks in authorizations[].resources.scopes for scope-authorization entries.
isolated function getApprovedScopesByConsentId(string consentId) returns string[]|error {
    string path = string `/consents/${getEncodedUri(consentId)}`;
    log:printDebug("[OpenFGC] GET consent by ID", path = path);
    http:Response response = check openfgcClient->get(path, {
        "org-id": orgId,
        "TPP-client-id": tppClientId
    });
    log:printDebug("[OpenFGC] GET consent by ID response", statusCode = response.statusCode);

    if response.statusCode < 200 || response.statusCode >= 300 {
        string|error bodyResult = response.getTextPayload();
        string body = bodyResult is string ? bodyResult : "";
        log:printError("[OpenFGC] GET consent by ID failed", statusCode = response.statusCode, body = body);
        return error(string `OpenFGC consent lookup returned ${response.statusCode}: ${body}`);
    }

    json payload = check response.getJsonPayload();
    log:printDebug("[OpenFGC] GET consent by ID body", body = payload.toJsonString());
    if !(payload is map<json>) {
        return [];
    }

    json authorizations = payload["authorizations"] ?: [];
    if !(authorizations is json[]) {
        return [];
    }

    string[] scopes = [];
    foreach json auth in authorizations {
        if auth is map<json> {
            json resources = auth["resources"] ?: {};
            if resources is map<json> {
                json rawScopes = resources["scopes"] ?: [];
                if rawScopes is json[] {
                    foreach json s in rawScopes {
                        if s is string {
                            scopes.push(s);
                        }
                    }
                }
            }
        }
    }
    log:printDebug("[OpenFGC] Approved scopes extracted", scopes = scopes.toString());
    return scopes;
}

// Returns a fresh bearer token for SCIM using client credentials.
// A new provider is created per call to avoid eager token fetch at module init.
isolated function getScimToken() returns string|error {
    string tokenUrl = scimTokenEndpoint == "" ? string `${scimApiBaseUrl}/oauth2/token` : scimTokenEndpoint;
    oauth2:ClientOAuth2Provider provider = new ({
        tokenUrl: tokenUrl,
        clientId: scimClientId,
        clientSecret: scimClientSecret,
        scopes: ["internal_user_mgt_view"]
    });
    return check provider.generateToken();
}

// Fetches SCIM user by ID for patient ID resolution.
// Returns () if SCIM not configured.
isolated function fetchScimUser(string userId) returns json|error {
    if scimApiBaseUrl == "" || scimClientId == "" || scimClientSecret == "" {
        log:printDebug("[SCIM] Skipped — not configured");
        return ();
    }

    log:printDebug("[SCIM] Fetching token via client credentials");
    string token = check getScimToken();
    http:Client scimClient = check new (scimApiBaseUrl);
    string path = scimApiPath + "/" + getEncodedUri(userId);
    log:printDebug("[SCIM] GET user request", path = path);
    http:Response response = check scimClient->get(path, {
        "Authorization": string `Bearer ${token}`,
        "Accept": "application/scim+json"
    });
    log:printDebug("[SCIM] GET user response", statusCode = response.statusCode);

    if response.statusCode < 200 || response.statusCode >= 300 {
        string|error bodyResult = response.getTextPayload();
        string body = bodyResult is string ? bodyResult : "";
        log:printError("[SCIM] GET user failed", statusCode = response.statusCode, body = body);
        return error(string `SCIM returned ${response.statusCode}: ${body}`);
    }
    json result = check response.getJsonPayload();
    log:printDebug("[SCIM] GET user body", body = result.toJsonString());
    return result;
}

// Resolves EHR launch context from the configured URL.
isolated function resolveLaunchContext(string launchId) returns EhrLaunchContext?|error {
    if ehrContextResolveUrl == "" {
        log:printDebug("[EHR] Skipped — ehrContextResolveUrl not configured");
        return ();
    }
    http:Client ehrClient = check new (ehrContextResolveUrl);
    string path = string `?launch=${getEncodedUri(launchId)}`;
    log:printDebug("[EHR] GET launch context request", launchId = launchId, path = path);
    http:Response response = check ehrClient->get(path);
    log:printDebug("[EHR] GET launch context response", statusCode = response.statusCode);
    json payload = check response.getJsonPayload();
    log:printDebug("[EHR] GET launch context body", body = payload.toJsonString());
    EhrLaunchContext|error ctx = payload.cloneWithType();
    if ctx is EhrLaunchContext {
        return ctx;
    }
    log:printWarn(string `Failed to parse EHR launch context: ${ctx.message()}`);
    return ();
}
