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
import ballerina/log;
import ballerina/url;

listener http:Listener consentBffListener = new (port, {host: hostname});

// Resolves the effective consent flow for a given spId.
// When consentFlow != "auto" the configured value is returned unchanged.
// Returns "scope", "purpose", or "redirect".
isolated function resolveEffectiveFlow(string spId) returns string {
    if consentFlow != "auto" {
        return consentFlow;
    }
    foreach string app in scopeConsentedApps {
        if app == spId {
            return "scope";
        }
    }
    foreach string app in purposeConsentedApps {
        if app == spId {
            return "purpose";
        }
    }
    return "redirect";
}

// Calls the IDP with a Bearer token; retries once on 401.
isolated function callIdp(string path) returns json|error {
    log:printDebug("[IDP] GET request", path = path);
    string token = check getIdpToken();
    http:Response response = check idpClient->get(path, {"Authorization": string `Bearer ${token}`});
    log:printDebug("[IDP] GET response", path = path, statusCode = response.statusCode);

    if response.statusCode == http:STATUS_UNAUTHORIZED {
        log:printWarn("Received 401 from IDP — retrying with fresh token", path = path);
        token = check getIdpToken();
        response = check idpClient->get(path, {"Authorization": string `Bearer ${token}`});
        log:printDebug("[IDP] GET retry response", path = path, statusCode = response.statusCode);
    }

    if response.statusCode != http:STATUS_OK {
        string|error body = response.getTextPayload();
        string bodyStr = body is string ? body : "";
        log:printError("[IDP] GET failed", path = path, statusCode = response.statusCode, body = bodyStr);
        return error(string `IDP API error: HTTP ${response.statusCode}`);
    }
    json result = check response.getJsonPayload();
    log:printDebug("[IDP] GET body", path = path, body = result.toJsonString());
    return result;
}

// Extracts fhirUser from a SCIM resource JSON map — tries enterprise extension then WSO2 schema.
isolated function extractFhirUserFromMap(map<json> resourceMap) returns string {
    json enterpriseExt = resourceMap["urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"] ?: {};
    if enterpriseExt is map<json> {
        string fhirUser = (enterpriseExt["fhirUser"] ?: "").toString();
        if fhirUser != "" {
            return fhirUser;
        }
    }
    json wso2Schema = resourceMap["urn:scim:wso2:schema"] ?: {};
    if wso2Schema is map<json> {
        return (wso2Schema["fhirUser"] ?: "").toString();
    }
    return "";
}

// Looks up a SCIM user by ID (direct GET) and returns user info including fhirUser.
// Asgardeo returns a UUID as loggedInUser in the OauthConsentKey response.
isolated function getScimUser(string loggedInUser) returns ScimUserInfo|error {
    string token = check getIdpToken();
    string path = string `/scim2/Users/${loggedInUser}`;
    log:printDebug("[SCIM] GET user request", path = path);
    http:Response response = check idpClient->get(path, {"Authorization": string `Bearer ${token}`});
    log:printDebug("[SCIM] GET user response", path = path, statusCode = response.statusCode);

    if response.statusCode != http:STATUS_OK {
        string|error body = response.getTextPayload();
        string bodyStr = body is string ? body : "";
        log:printError("[SCIM] GET user failed", path = path, statusCode = response.statusCode, body = bodyStr);
        return error(string `SCIM user lookup failed: HTTP ${response.statusCode}: ${bodyStr}`);
    }

    json userJson = check response.getJsonPayload();
    log:printDebug("[SCIM] GET user body", path = path, body = userJson.toJsonString());
    if userJson !is map<json> {
        return error("Unexpected SCIM user record format");
    }

    string userId = (userJson["id"] ?: loggedInUser).toString();

    // Build displayName from givenName + familyName; fall back to displayName or UUID
    string displayName = loggedInUser;
    json nameField = userJson["name"] ?: {};
    if nameField is map<json> {
        string given = (nameField["givenName"] ?: "").toString();
        string family = (nameField["familyName"] ?: "").toString();
        string fullName = (given + " " + family).trim();
        if fullName != "" {
            displayName = fullName;
        }
    }
    if displayName == loggedInUser {
        string dn = (userJson["displayName"] ?: "").toString();
        if dn != "" {
            displayName = dn;
        }
    }

    string email = "";
    json emailsField = userJson["emails"] ?: [];
    if emailsField is json[] {
        foreach json emailEntry in emailsField {
            if emailEntry is map<json> {
                if (emailEntry["primary"] ?: false).toString() == "true" {
                    email = (emailEntry["value"] ?: "").toString();
                    break;
                }
            }
        }
        if email == "" && emailsField.length() > 0 {
            json first = emailsField[0];
            if first is map<json> {
                email = (first["value"] ?: "").toString();
            }
        }
    }

    string fhirUser = extractFhirUserFromMap(userJson);
    log:printDebug("[SCIM] Parsed user", userId = userId, displayName = displayName, email = email, fhirUser = fhirUser);
    return {id: userId, displayName: displayName, email: email, fhirUser: fhirUser};
}

// Searches SCIM for users whose fhirUser attribute contains "Patient".
isolated function getScimPatients() returns ConsentPatient[]|error {
    string token = check getIdpToken();

    json searchPayload = {
        "schemas": ["urn:ietf:params:scim:api:messages:2.0:SearchRequest"],
        "filter": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User.fhirUser co Patient",
        "startIndex": 1,
        "count": 100
    };

    http:Request req = new;
    req.setJsonPayload(searchPayload);
    req.addHeader("Authorization", string `Bearer ${token}`);

    log:printDebug("[SCIM] POST patient search request", payload = searchPayload.toJsonString());
    http:Response response = check idpClient->post("/scim2/Users/.search", req);
    log:printDebug("[SCIM] POST patient search response", statusCode = response.statusCode);
    if response.statusCode != http:STATUS_OK {
        string|error body = response.getTextPayload();
        string bodyStr = body is string ? body : "";
        log:printError("[SCIM] POST patient search failed", statusCode = response.statusCode, body = bodyStr);
        return error(string `SCIM patient search failed: HTTP ${response.statusCode}`);
    }

    json listJson = check response.getJsonPayload();
    log:printDebug("[SCIM] POST patient search body", body = listJson.toJsonString());
    if listJson !is map<json> {
        return [];
    }

    json resourcesField = listJson["Resources"] ?: [];
    if resourcesField !is json[] {
        return [];
    }

    ConsentPatient[] patients = [];
    foreach json r in resourcesField {
        if r !is map<json> {
            continue;
        }
        string fhirUser = extractFhirUserFromMap(r);
        string? mrn = ();
        json wso2Schema = r["urn:scim:wso2:schema"] ?: {};
        if wso2Schema is map<json> {
            string mrnVal = (wso2Schema["mrn"] ?: "").toString();
            if mrnVal != "" {
                mrn = mrnVal;
            }
        }
        patients.push({
            id: (r["id"] ?: "").toString(),
            name: (r["displayName"] ?: "").toString(),
            fhirUser: fhirUser,
            mrn: mrn
        });
    }
    return patients;
}

// Looks up an existing active consent for the user in OpenFGC.
isolated function getExistingConsent(string userId, string effectiveFlow) returns ExistingConsentData?|error {
    string searchPath = string `/consents?userIds=${userId}&clientIds=${tppClientId}&consentStatuses=ACTIVE,CREATED&limit=1`;
    map<string|string[]> headers = {"org-id": orgId, "TPP-client-id": tppClientId};

    log:printDebug("[OpenFGC] GET existing consent request", path = searchPath);
    http:Response searchResp = check openfgcClient->get(searchPath, headers);
    log:printDebug("[OpenFGC] GET existing consent response", statusCode = searchResp.statusCode);
    if searchResp.statusCode != http:STATUS_OK {
        string|error body = searchResp.getTextPayload();
        string bodyStr = body is string ? body : "";
        log:printWarn("[OpenFGC] GET existing consent non-200", statusCode = searchResp.statusCode, body = bodyStr);
        return ();
    }

    json searchJson = check searchResp.getJsonPayload();
    log:printDebug("[OpenFGC] GET existing consent body", body = searchJson.toJsonString());
    OpenFGCSearchResponse searchResult = check searchJson.cloneWithType();

    if searchResult.data.length() == 0 {
        return ();
    }

    OpenFGCSearchRecord existing = searchResult.data[0];
    string consentId = existing.id;

    // Scope flow: extract approved scopes from authorizations[0].resources.scopes
    if effectiveFlow == "scope" {
        string[] approvedScopes = [];
        OpenFGCSearchAuthorization[]? auths = existing.authorizations;
        if auths != () && auths.length() > 0 {
            json scopesField = auths[0].resources?.scopes ?: [];
            if scopesField is json[] {
                foreach json s in scopesField {
                    approvedScopes.push(s.toString());
                }
            }
        }
        return {consentId, approvedScopes, consentedPurposeNames: [], consentedElements: {}};
    }

    // Purpose flow: extract previously consented purposes and elements
    string[] consentedPurposeNames = [];
    map<string[]> consentedElements = {};

    OpenFGCSearchPurpose[]? existingPurposes = existing.purposes;
    if existingPurposes != () {
        foreach OpenFGCSearchPurpose p in existingPurposes {
            string[] approvedElems = [];
            foreach OpenFGCSearchElement e in p.elements {
                if e.isUserApproved {
                    approvedElems.push(e.name);
                }
            }
            if approvedElems.length() > 0 {
                consentedElements[p.name] = approvedElems;

                if showConsentElements {
                    consentedPurposeNames.push(p.name);
                } else {
                    // Purpose-only mode: only pre-check if ALL configured elements were approved
                    boolean allApproved = true;
                    foreach ConsentPurposeConfig cp in consentPurpose {
                        if cp.name == p.name {
                            foreach string configEl in cp.elements {
                                boolean found = false;
                                foreach string approvedEl in approvedElems {
                                    if approvedEl == configEl {
                                        found = true;
                                        break;
                                    }
                                }
                                if !found {
                                    allApproved = false;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    if allApproved {
                        consentedPurposeNames.push(p.name);
                    }
                }
            }
        }
    }

    return {consentId, approvedScopes: [], consentedPurposeNames, consentedElements};
}

@http:ServiceConfig {
    cors: {
        allowOrigins: [corsAllowedOrigin],
        allowCredentials: false,
        allowHeaders: ["Content-Type", "Authorization"],
        allowMethods: ["GET", "POST", "OPTIONS"]
    }
}
service /v2 on consentBffListener {

    # Returns aggregated consent context for the UI in one response.
    # + sessionDataKeyConsent - The consent session key from the identity server
    # + spId - The service provider (application) ID requesting consent
    # + return - Aggregated consent data for the scope or purpose flow, or an error
    isolated resource function get get\-consent\-data(string sessionDataKeyConsent, string spId)
            returns ScopeConsentData|PurposeConsentData|RedirectConsentData|error {

        log:printDebug("Fetching consent data", sessionDataKeyConsent = sessionDataKeyConsent, spId = spId);

        // Resolve flow before any IDP call — avoids consuming the consent session for the redirect case.
        string effectiveFlow = resolveEffectiveFlow(spId);
        log:printDebug("Effective consent flow", spId = spId, effectiveFlow = effectiveFlow);

        if effectiveFlow == "redirect" {
            string sdkcEncoded = check url:encode(sessionDataKeyConsent, "UTF-8");
            string spIdEncoded = check url:encode(spId, "UTF-8");
            string redirectUrl = string `${defaultIdpConsentPage}?sessionDataKeyConsent=${sdkcEncoded}&spId=${spIdEncoded}`;
            log:printDebug("Redirecting to default IDP consent page", redirectUrl = redirectUrl);
            return <RedirectConsentData>{redirectUrl: redirectUrl};
        }

        // Step 1: OauthConsentKey API — get loggedInUser, application, scope
        json consentKeyJson = check callIdp(
            string `/api/identity/auth/v1.1/data/OauthConsentKey/${sessionDataKeyConsent}`
        );
        IdpConsentKeyResponse consentKeyData = check consentKeyJson.cloneWithType();

        string loggedInUser = consentKeyData.loggedInUser;
        string application = consentKeyData.application;
        string scopeStr = consentKeyData.scope;
        string mandatoryClaims = consentKeyData.mandatoryClaims ?: "";

        log:printDebug("Consent key resolved", loggedInUser = loggedInUser, application = application, scope = scopeStr);

        // Step 2: SCIM user lookup (both flows) + OpenFGC existing consent (if singleConsentPerUser)
        future<ScimUserInfo|error> userFuture = start getScimUser(loggedInUser);
        future<ExistingConsentData?|error>? existingConsentFuture = ();
        if singleConsentPerUser {
            existingConsentFuture = start getExistingConsent(loggedInUser, effectiveFlow);
        }

        ScimUserInfo scimUserInfo = check wait userFuture;
        ConsentUser consentUser = {
            id: scimUserInfo.id,
            displayName: scimUserInfo.displayName,
            email: scimUserInfo.email
        };
        boolean isPractitioner = scimUserInfo.fhirUser.includes("Practitioner");

        ExistingConsentData? existingConsentInfo = ();
        if existingConsentFuture != () {
            existingConsentInfo = check wait existingConsentFuture;
        }

        // Step 4: Issue HS256 consent token
        jwt:IssuerConfig issuerConfig = {
            issuer: "consent-app-bff",
            audience: "consent-app",
            username: loggedInUser,
            expTime: 600,
            signatureConfig: {
                algorithm: jwt:HS256,
                config: clientSecret
            },
            customClaims: {
                "app": application,
                "sdkc": sessionDataKeyConsent
            }
        };
        string consentToken = check jwt:issue(issuerConfig);

        if effectiveFlow == "scope" {
            // Parse and partition scopes — avoid lambdas capturing module-level state
            string[] allScopes = re ` `.split(scopeStr);
            string[] hiddenScopes = [];
            string[] visibleScopes = [];

            foreach string s in allScopes {
                if s == "" {
                    continue;
                }
                if s.startsWith("OH_launch/") {
                    hiddenScopes.push(s);
                    continue;
                }
                boolean isAlwaysAllowed = false;
                foreach string allowed in alwaysAllowedScopes {
                    if allowed == s {
                        isAlwaysAllowed = true;
                        break;
                    }
                }
                if !isAlwaysAllowed {
                    visibleScopes.push(s);
                }
            }

            // Step 3: SCIM patient search if user is a practitioner
            ConsentPatient[] patients = [];
            if isPractitioner {
                patients = check getScimPatients();
            }

            ScopeConsentData scopeData = {
                flow: "scope",
                sessionDataKeyConsent: sessionDataKeyConsent,
                spId: spId,
                user: consentUser,
                isPractitioner: isPractitioner,
                scopes: visibleScopes,
                hiddenScopes: hiddenScopes,
                mandatoryClaims: mandatoryClaims,
                consentToken: consentToken
            };

            if patients.length() > 0 {
                scopeData.patients = patients;
            }
            if existingConsentInfo != () && existingConsentInfo.approvedScopes.length() > 0 {
                scopeData.previouslyApprovedScopes = existingConsentInfo.approvedScopes;
            }

            return scopeData;

        } else {
            // Purpose flow
            ConsentPurpose[] purposeList = [];
            foreach ConsentPurposeConfig p in consentPurpose {
                string? desc = p.description;
                purposeList.push({
                    purposeName: p.name,
                    mandatory: p.mandatory,
                    purposeDescription: desc == () || desc == "" ? () : desc,
                    elements: showConsentElements ? p.elements : []
                });
            }

            PurposeConsentData purposeData = {
                flow: "purpose",
                sessionDataKeyConsent: sessionDataKeyConsent,
                spId: spId,
                appName: application,
                user: consentUser,
                purposes: purposeList,
                consentToken: consentToken
            };

            if existingConsentInfo != () {
                purposeData.existingConsentId = existingConsentInfo.consentId;
                if existingConsentInfo.consentedPurposeNames.length() > 0 {
                    purposeData.previouslyConsentedPurposeNames = existingConsentInfo.consentedPurposeNames;
                }
                if existingConsentInfo.consentedElements.length() > 0 {
                    purposeData.previouslyConsentedElements = existingConsentInfo.consentedElements;
                }
            }

            return purposeData;
        }
    }

    # Validates the consent token and stores the user's decision in OpenFGC.
    # The UI form-POSTs directly to the IDP authorize URL after this call succeeds.
    # + submission - The consent decision payload including the JWT consent token
    # + return - Submission status, or an error if the token is invalid
    isolated resource function post submit\-consent(@http:Payload SubmitConsentRequest submission)
            returns SubmitConsentResponse|error {

        log:printDebug("[BFF] submit-consent received", approved = submission.approved, spId = submission.spId,
            sessionDataKeyConsent = submission.sessionDataKeyConsent,
            approvedScopes = (submission.approvedScopes ?: []).toString(),
            hiddenScopes = (submission.hiddenScopes ?: []).toString(),
            consentedPurposes = (submission.consentedPurposes ?: []).toString(),
            existingConsentId = (submission.existingConsentId ?: ""));

        // Validate consent token
        jwt:ValidatorConfig validatorConfig = {
            issuer: "consent-app-bff",
            audience: "consent-app",
            signatureConfig: {secret: clientSecret}
        };
        jwt:Payload tokenPayload = check jwt:validate(submission.consentToken, validatorConfig);

        string trustedUser = tokenPayload.sub ?: "";
        string trustedApp = (tokenPayload["app"] ?: "").toString();
        string tokenSdkc = (tokenPayload["sdkc"] ?: "").toString();

        if tokenSdkc != submission.sessionDataKeyConsent {
            return error("Consent token session mismatch");
        }

        log:printDebug("Consent token validated", loggedInUser = trustedUser, application = trustedApp);

        if !submission.approved {
            return {status: "success", message: "Consent denied"};
        }

        string[]? approvedScopes = submission.approvedScopes;
        string[]? hiddenScopes = submission.hiddenScopes;
        ConsentedPurpose[]? consentedPurposes = submission.consentedPurposes;

        if approvedScopes != () || hiddenScopes != () {
            // Scope flow: store all approved + hidden scopes in authorizations[].resources.scopes
            string[] scopesToStore = [];
            if approvedScopes != () {
                foreach string s in approvedScopes {
                    scopesToStore.push(s);
                }
            }
            if hiddenScopes != () {
                foreach string s in hiddenScopes {
                    scopesToStore.push(s);
                }
            }

            OpenFGCConsentCreatePayload payload = {
                'type: consentType,
                purposes: [{
                    name: scopeConsentPurposeName,
                    elements: [{name: scopeConsentElementName, isUserApproved: true}]
                }],
                authorizations: [{
                    userId: trustedUser,
                    'type: "scope-authorization",
                    status: "APPROVED",
                    resources: {spId: submission.spId, application: trustedApp, scopes: scopesToStore}
                }],
                attributes: {"sessionDataKeyConsent": submission.sessionDataKeyConsent}
            };

            http:Request req = new;
            req.setJsonPayload(payload.toJson());
            req.addHeader("org-id", orgId);
            req.addHeader("TPP-client-id", tppClientId);

            log:printDebug("[OpenFGC] POST scope consent request", payload = payload.toJson().toJsonString());
            http:Response consentResp = check openfgcClient->post("/consents", req);
            log:printDebug("[OpenFGC] POST scope consent response", statusCode = consentResp.statusCode);
            if consentResp.statusCode != http:STATUS_CREATED {
                string|error consentBody = consentResp.getTextPayload();
                string consentBodyStr = consentBody is string ? consentBody : "";
                log:printError("OpenFGC scope consent creation failed", statusCode = consentResp.statusCode, body = consentBodyStr);
                return error(string `OpenFGC consent creation failed: HTTP ${consentResp.statusCode}: ${consentBodyStr}`);
            }
            log:printDebug("Scope consent stored in OpenFGC");

        } else if consentedPurposes != () {
            // Purpose flow: store element-level approvals
            OpenFGCConsentPurposeItem[] purposeItems = [];
            foreach ConsentPurposeConfig purposeConfig in consentPurpose {
                OpenFGCConsentElementApproval[] elements = [];
                string purposeName = purposeConfig.name;

                if showConsentElements {
                    string[] approvedElementNames = [];
                    foreach ConsentedPurpose cp in consentedPurposes {
                        if cp.purposeName == purposeName {
                            approvedElementNames = cp.consentedElements;
                            break;
                        }
                    }
                    foreach string elName in purposeConfig.elements {
                        boolean isApproved = false;
                        foreach string approvedEl in approvedElementNames {
                            if approvedEl == elName {
                                isApproved = true;
                                break;
                            }
                        }
                        elements.push({name: elName, isUserApproved: isApproved});
                    }
                } else {
                    boolean purposeConsented = false;
                    foreach ConsentedPurpose cp in consentedPurposes {
                        if cp.purposeName == purposeName {
                            purposeConsented = true;
                            break;
                        }
                    }
                    foreach string elName in purposeConfig.elements {
                        elements.push({name: elName, isUserApproved: purposeConsented});
                    }
                }

                purposeItems.push({name: purposeConfig.name, elements: elements});
            }

            OpenFGCConsentCreatePayload payload = {
                'type: consentType,
                purposes: purposeItems,
                authorizations: [{
                    userId: trustedUser,
                    'type: "authorisation",
                    status: "APPROVED",
                    resources: {spId: submission.spId, application: trustedApp}
                }],
                attributes: {"sessionDataKeyConsent": submission.sessionDataKeyConsent}
            };

            http:Request req = new;
            req.setJsonPayload(payload.toJson());
            req.addHeader("org-id", orgId);
            req.addHeader("TPP-client-id", tppClientId);

            log:printDebug("[OpenFGC] POST/PUT purpose consent request", payload = payload.toJson().toJsonString());
            string? existingId = singleConsentPerUser ? submission.existingConsentId : ();
            if existingId != () {
                log:printDebug("[OpenFGC] PUT purpose consent (update)", consentId = existingId);
                http:Response updateResp = check openfgcClient->put(string `/consents/${existingId}`, req);
                log:printDebug("[OpenFGC] PUT purpose consent response", statusCode = updateResp.statusCode);
                if updateResp.statusCode != http:STATUS_OK {
                    string|error updateBody = updateResp.getTextPayload();
                    string updateBodyStr = updateBody is string ? updateBody : "";
                    log:printError("OpenFGC consent update failed", statusCode = updateResp.statusCode, body = updateBodyStr);
                    return error(string `OpenFGC consent update failed: HTTP ${updateResp.statusCode}: ${updateBodyStr}`);
                }
                log:printDebug("Purpose consent updated in OpenFGC", consentId = existingId);
            } else {
                log:printDebug("[OpenFGC] POST purpose consent (create)");
                http:Response createResp = check openfgcClient->post("/consents", req);
                log:printDebug("[OpenFGC] POST purpose consent response", statusCode = createResp.statusCode);
                if createResp.statusCode != http:STATUS_CREATED {
                    string|error createBody = createResp.getTextPayload();
                    string createBodyStr = createBody is string ? createBody : "";
                    log:printError("OpenFGC purpose consent creation failed", statusCode = createResp.statusCode, body = createBodyStr);
                    return error(string `OpenFGC consent creation failed: HTTP ${createResp.statusCode}: ${createBodyStr}`);
                }
                log:printDebug("Purpose consent created in OpenFGC");
            }
        }

        return {status: "success", message: "Consent approved successfully"};
    }
}
