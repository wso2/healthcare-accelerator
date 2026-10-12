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
import ballerina/oauth2;
import ballerina/url;

function buildIdpClientConfig() returns http:ClientConfiguration {
    http:ClientConfiguration config = {};
    if consentContextApiTrustStorePath != "" && consentContextApiTrustStorePassword != "" {
        config.secureSocket = {
            cert: {path: consentContextApiTrustStorePath, password: consentContextApiTrustStorePassword}
        };
    }
    return config;
}

// Plain HTTP clients — auth headers are set manually so 401s can be intercepted.
final http:Client idpClient = check new (idpBaseUrl, buildIdpClientConfig());

// In-memory cache of consent purposes fetched from WSO2 IS at startup.
// Keyed by purpose name. Populated by fetchAndCachePurposes() in init().
isolated map<CachedPurpose> purposeCache = {};

// Calls the WSO2 IS consent management API with a bearer token from the management
// application; retries once with a fresh token on 401. `path` is relative to consentApiBasePath.
isolated function callConsentApi(http:Method method, string path, json? payload = ()) returns http:Response|error {
    string fullPath = consentApiBasePath + path;
    string token = check getIdpToken();
    http:Response response = check idpClient->execute(method, fullPath, payload, {"Authorization": string `Bearer ${token}`});
    if response.statusCode == http:STATUS_UNAUTHORIZED {
        log:printWarn("Received 401 from consent API — retrying with fresh token", path = fullPath);
        token = check getIdpToken();
        response = check idpClient->execute(method, fullPath, payload, {"Authorization": string `Bearer ${token}`});
    }
    log:printDebug("[Consent API] response", method = method, path = fullPath, statusCode = response.statusCode);
    return response;
}

// Loads the purpose cache on first use when it was not populated at startup.
isolated function ensurePurposesCached() returns error? {
    boolean loaded;
    lock {
        loaded = purposeCache.length() > 0;
    }
    if !loaded {
        check fetchAndCachePurposes();
    }
}

// Fetches all configured purpose definitions (and their elements) from WSO2 IS and stores
// them in purposeCache. Called once at module init — fails hard if any purpose is missing.
isolated function fetchAndCachePurposes() returns error? {
    // Collect all distinct purpose names: scope purpose + all purpose-flow purposes
    string[] namesToFetch = [scopeConsent.purposeName];
    foreach PurposeConsentConfig p in purposeConsent {
        boolean alreadyAdded = false;
        foreach string n in namesToFetch {
            if n == p.purposeName {
                alreadyAdded = true;
                break;
            }
        }
        if !alreadyAdded {
            namesToFetch.push(p.purposeName);
        }
    }

    foreach string name in namesToFetch {
        string filter = check url:encode(string `name eq ${name}`, "UTF-8");
        log:printDebug("[Consent API] Fetching consent purpose at startup", purposeName = name);

        http:Response listResp = check callConsentApi(http:GET, string `/purposes?filter=${filter}`);
        if listResp.statusCode != http:STATUS_OK {
            string|error body = listResp.getTextPayload();
            string bodyStr = body is string ? body : "";
            return error(string `Failed to fetch consent purpose '${name}': HTTP ${listResp.statusCode}: ${bodyStr}`);
        }
        IsPurposeListResponse purposes = check (check listResp.getJsonPayload()).cloneWithType();

        string? purposeId = ();
        foreach IsPurposeSummary summary in purposes.Purposes {
            if summary.name == name {
                purposeId = summary.id;
                break;
            }
        }
        if purposeId is () {
            return error(string `Consent purpose '${name}' not found in WSO2 IS`);
        }

        // The purpose resource carries the elements of its latest version
        http:Response purposeResp = check callConsentApi(http:GET, string `/purposes/${purposeId}`);
        if purposeResp.statusCode != http:STATUS_OK {
            string|error body = purposeResp.getTextPayload();
            string bodyStr = body is string ? body : "";
            return error(string `Failed to fetch consent purpose '${name}': HTTP ${purposeResp.statusCode}: ${bodyStr}`);
        }
        IsPurpose fetched = check (check purposeResp.getJsonPayload()).cloneWithType();

        string[] elementNames = [];
        map<string> elementIds = {};
        boolean anyMandatory = false;
        foreach IsPurposeElement e in fetched.elements {
            string? elementId = e.id;
            if elementId is () {
                return error(string `Element '${e.name}' of consent purpose '${name}' has no id`);
            }
            elementNames.push(e.name);
            elementIds[e.name] = elementId;
            if e.mandatory {
                anyMandatory = true;
            }
        }

        CachedPurpose cached = {
            id: purposeId,
            name: fetched.name,
            description: fetched.description,
            elementNames: elementNames.cloneReadOnly(),
            elementIds: elementIds.cloneReadOnly(),
            anyMandatory: anyMandatory
        };
        lock {
            purposeCache[name] = cached;
        }
        log:printDebug("[Consent API] Consent purpose cached", purposeName = name, elementCount = elementNames.length());
    }
}

// Resolves EHR launch context from the configured URL.
// Returns () if ehrContextResolveUrl is not configured or context not found.
isolated function resolveLaunchContext(string launchId) returns EhrLaunchContext?|error {
    if ehrContextResolveUrl == "" {
        return ();
    }
    http:ClientConfiguration ehrClientConfig = {};
    if consentContextApiTrustStorePath != "" && consentContextApiTrustStorePassword != "" {
        ehrClientConfig.secureSocket = {
            cert: {path: consentContextApiTrustStorePath, password: consentContextApiTrustStorePassword}
        };
    }
    http:Client ehrClient = check new (ehrContextResolveUrl, ehrClientConfig);
    http:Response response = check ehrClient->get(string `/launch/${launchId}`);
    if response.statusCode != 200 {
        string|error body = response.getTextPayload();
        string bodyStr = body is string ? body : "";
        return error(string `EHR context resolve returned ${response.statusCode}: ${bodyStr}`);
    }
    json payload = check response.getJsonPayload();
    EhrLaunchContext|error ctx = payload.cloneWithType();
    if ctx is error {
        return error(string `Failed to parse EHR launch context: ${ctx.message()}`);
    }
    return ctx;
}

// Returns a fresh bearer token from the identity provider using client credentials.
// A new ClientOAuth2Provider is created per call to avoid an eager token fetch at
// module init, which would fail in tests before mock listeners are up.
isolated function getIdpToken() returns string|error {
    string tokenUrl = idpTokenEndpoint == "" ? string `${idpBaseUrl}/oauth2/token` : idpTokenEndpoint;
    oauth2:ClientCredentialsGrantConfig grantConfig = {
        tokenUrl: tokenUrl,
        clientId: clientId,
        clientSecret: clientSecret,
        scopes: [
            "internal_user_mgt_view",
            "internal_user_mgt_list",
            "internal_consent_mgt_purpose_view",
            "internal_consent_mgt_element_view",
            "internal_consent_mgt_consent_create",
            "internal_consent_mgt_consent_view",
            "internal_consent_mgt_consent_update"
        ]
    };
    if consentContextApiTrustStorePath != "" && consentContextApiTrustStorePassword != "" {
        grantConfig.clientConfig = {
            secureSocket: {
                cert: {
                    path: consentContextApiTrustStorePath,
                    password: consentContextApiTrustStorePassword
                }
            }
        };
    }
    oauth2:ClientOAuth2Provider provider = new (grantConfig);
    return check provider.generateToken();
}
