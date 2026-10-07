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
import ballerina/time;
import ballerina/url;

listener http:Listener consentBffListener = new (port, {host: hostname});

function init() returns error? {
    if fetchPurposesOnStartup {
        check fetchAndCachePurposes();
        log:printInfo("Consent purpose cache initialized successfully");
    }
}

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

// Extracts fhirUser from a SCIM resource JSON map using the custom schema extension.
isolated function extractFhirUserFromMap(map<json> resourceMap) returns string {
    json customSchema = resourceMap["urn:scim:schemas:extension:custom:User"] ?: {};
    if customSchema is map<json> {
        json fhirUser = customSchema[fhirUserAttributeName] ?: "";
        if fhirUser is string && fhirUser != "" {
            return fhirUser;
        }
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
        "filter": string `urn:scim:schemas:extension:custom:User.${fhirUserAttributeName} co Patient`,
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
        json customSchema = r["urn:scim:schemas:extension:custom:User"] ?: {};
        if customSchema is map<json> {
            string mrnVal = (customSchema["mrn"] ?: "").toString();
            if mrnVal != "" {
                mrn = mrnVal;
            }
        }
        string patientName = "";
        json nameField = r["name"] ?: {};
        if nameField is map<json> {
            string given = (nameField["givenName"] ?: "").toString();
            string family = (nameField["familyName"] ?: "").toString();
            patientName = (given + " " + family).trim();
        }
        if patientName == "" {
            patientName = (r["userName"] ?: r["id"] ?: "").toString();
        }
        patients.push({
            id: (r["id"] ?: "").toString(),
            name: patientName,
            fhirUser: fhirUser,
            mrn: mrn
        });
    }
    return patients;
}

// Property keys stored on every consent created by this service. The scope flow keeps the
// approved scopes in `approvedScopes` (space-separated) because the IS consent model has no
// per-authorization resources; iam-service-extensions reads them back by sessionDataKeyConsent.
const string PROP_SESSION_KEY = "sessionDataKeyConsent";
const string PROP_SP_ID = "spId";
const string PROP_APPLICATION = "application";
// OAuth client_id of the requesting app. IS does not pass sessionDataKeyConsent to the token flow, so
// iam-service-extensions finds the consent again with the user id and client_id of the token request.
const string PROP_CLIENT_ID = "clientId";
const string PROP_APPROVED_SCOPES = "approvedScopes";
const string PROP_EXPIRY_OPTION = "consentExpiryOption";

// Lists the ids of the user's ACTIVE consents for this service in WSO2 IS.
isolated function listActiveConsentIds(string userId, int 'limit) returns string[]|error {
    string encodedUser = check url:encode(userId, "UTF-8");
    string encodedService = check url:encode(serviceId, "UTF-8");
    string path = string `/consents?userId=${encodedUser}&relation=SUBJECT&serviceId=${encodedService}&state=ACTIVE&limit=${'limit}`;
    http:Response resp = check callConsentApi(http:GET, path);
    if resp.statusCode != http:STATUS_OK {
        string|error body = resp.getTextPayload();
        string bodyStr = body is string ? body : "";
        return error(string `Consent lookup failed: HTTP ${resp.statusCode}: ${bodyStr}`);
    }
    IsConsentListResponse list = check (check resp.getJsonPayload()).cloneWithType();
    string[] ids = [];
    foreach IsConsentSummary c in list.Consents {
        ids.push(c.id);
    }
    return ids;
}

// Revokes the user's ACTIVE consents for this service, except `keepConsentId` (if given).
// WSO2 IS consents cannot be replaced in place, so re-submission = create new + revoke old.
isolated function revokeActiveConsents(string userId, string? keepConsentId) returns error? {
    string[] ids = check listActiveConsentIds(userId, 100);
    foreach string id in ids {
        if id == keepConsentId {
            continue;
        }
        http:Response resp = check callConsentApi(http:POST, string `/consents/${id}/revoke`);
        if resp.statusCode != http:STATUS_NO_CONTENT {
            string|error body = resp.getTextPayload();
            string bodyStr = body is string ? body : "";
            return error(string `Consent revoke failed: HTTP ${resp.statusCode}: ${bodyStr}`);
        }
        log:printDebug("[Consent API] Revoked previous consent", consentId = id);
    }
}

// Creates a consent in WSO2 IS and returns its id.
isolated function createConsent(IsConsentCreateRequest payload) returns string|error {
    log:printDebug("[Consent API] POST consent request", payload = payload.toJson().toJsonString());
    http:Response resp = check callConsentApi(http:POST, "/consents", payload.toJson());
    if resp.statusCode != http:STATUS_CREATED {
        string|error body = resp.getTextPayload();
        string bodyStr = body is string ? body : "";
        log:printError("Consent creation failed", statusCode = resp.statusCode, body = bodyStr);
        return error(string `Consent creation failed: HTTP ${resp.statusCode}: ${bodyStr}`);
    }
    IsConsentCreatedResponse created = check (check resp.getJsonPayload()).cloneWithType();
    return created.id;
}

// Converts a validity in seconds to an absolute expiry in epoch milliseconds.
isolated function expiryFromSeconds(int seconds) returns int {
    return (time:utcNow()[0] + seconds) * 1000;
}

// Looks up an existing active consent for the user in WSO2 IS.
isolated function getExistingConsent(string userId, string effectiveFlow) returns ExistingConsentData?|error {
    string[]|error ids = listActiveConsentIds(userId, 1);
    if ids is error {
        log:printWarn("[Consent API] Existing consent lookup failed", 'error = ids);
        return ();
    }
    if ids.length() == 0 {
        return ();
    }
    string consentId = ids[0];

    http:Response resp = check callConsentApi(http:GET, string `/consents/${consentId}`);
    if resp.statusCode != http:STATUS_OK {
        string|error body = resp.getTextPayload();
        string bodyStr = body is string ? body : "";
        log:printWarn("[Consent API] GET consent non-200", statusCode = resp.statusCode, body = bodyStr);
        return ();
    }
    json detailJson = check resp.getJsonPayload();
    log:printDebug("[Consent API] GET consent body", body = detailJson.toJsonString());
    IsConsentDetail existing = check detailJson.cloneWithType();
    map<string> props = existing.properties ?: {};

    // Scope flow: approved scopes and chosen expiry are kept in consent properties
    if effectiveFlow == "scope" {
        string[] approvedScopes = [];
        string scopesStr = props[PROP_APPROVED_SCOPES] ?: "";
        foreach string sc in re ` `.split(scopesStr) {
            if sc != "" {
                approvedScopes.push(sc);
            }
        }
        string? consentExpiryOption = props[PROP_EXPIRY_OPTION];
        return {consentId, consentExpiryOption, approvedScopes, consentedPurposeNames: [], consentedElements: {}};
    }

    // Purpose flow: extract previously consented purposes and elements
    string[] consentedPurposeNames = [];
    map<string[]> consentedElements = {};

    foreach IsConsentedPurpose p in existing.purposes {
        string[] approvedElems = [];
        foreach IsConsentedElement e in p.elements {
            approvedElems.push(e.name);
        }
        if approvedElems.length() == 0 {
            continue;
        }
        consentedElements[p.name] = approvedElems;

        if showConsentElements {
            consentedPurposeNames.push(p.name);
        } else {
            // Purpose-only mode: only pre-check if ALL cached elements were approved
            boolean allApproved = true;
            string[] configElements = [];
            lock {
                CachedPurpose? cp = purposeCache[p.name];
                if cp != () {
                    configElements = cp.elementNames.clone();
                }
            }
            foreach string configEl in configElements {
                if approvedElems.indexOf(configEl) is () {
                    allApproved = false;
                    break;
                }
            }
            if allApproved {
                consentedPurposeNames.push(p.name);
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
service / on consentBffListener {

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
        string launchId = extractQueryParam(consentKeyData.spQueryParams ?: "", "launch");
        string mandatoryClaims = consentKeyData.mandatoryClaims ?: "";

        log:printDebug("Consent key resolved", loggedInUser = loggedInUser, application = application, scope = scopeStr);

        // Step 2: SCIM user lookup (both flows) + existing consent (if singleConsentPerUser)
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
                "cid": extractQueryParam(consentKeyData.spQueryParams ?: "", "client_id"),
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
                if s.startsWith("OH_") || s.startsWith("launch") || s == "fhirUser" {
                    log:printDebug("Hiding scope: " + s);
                    hiddenScopes.push(s);
                    continue;
                }
                if s.matches(re `^system/.*`) {
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
                    if s.matches(re `^(patient|user)/(\*|[A-Za-z]*)\.(cruds|c?r?u?d?s?)$`) {
                        string[] parts = re `\.`.split(s);
                        if parts.length() == 2 && parts[1].length() > 1 {
                            string resourceStr = parts[0];
                            string opsStr = parts[1];
                            foreach int i in 0 ..< opsStr.length() {
                                visibleScopes.push(resourceStr + "." + opsStr.substring(i, i + 1));
                            }
                        } else {
                            visibleScopes.push(s);
                        }
                    } else {
                        visibleScopes.push(s);
                    }
                }
            }

            // If the launch context already carries a patient, skip the patient picker
            boolean launchHasPatient = false;
            if launchId != "" {
                EhrLaunchContext?|error launchCtxResult = resolveLaunchContext(launchId);
                if launchCtxResult is EhrLaunchContext {
                    string? launchPatientId = launchCtxResult.patientId;
                    if launchPatientId is string && launchPatientId != "" {
                        launchHasPatient = true;
                        hiddenScopes.push(string `OH_patient/${launchPatientId}`);
                        log:printDebug("[EHR] Patient resolved from launch context — skipping patient picker",
                            patientId = launchPatientId);
                    }
                } else if launchCtxResult is error {
                    log:printError("[EHR] Failed to resolve launch context", launchId = launchId,
                        'error = launchCtxResult);
                }
            }

            // Step 3: SCIM patient search only if practitioner and no patient from launch context
            ConsentPatient[] patients = [];
            if isPractitioner && !launchHasPatient {
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
            if existingConsentInfo != () {
                scopeData.existingConsentId = existingConsentInfo.consentId;
                if existingConsentInfo.approvedScopes.length() > 0 {
                    scopeData.previouslyApprovedScopes = existingConsentInfo.approvedScopes;
                }
                string? prevExpiry = existingConsentInfo.consentExpiryOption;
                if prevExpiry is string {
                    scopeData.consentExpiryOption = prevExpiry;
                }
            }

            return scopeData;

        } else {
            // Purpose flow
            check ensurePurposesCached();
            ConsentPurpose[] purposeList = [];
            foreach PurposeConsentConfig p in purposeConsent {
                string[] elementNames = [];
                string? description = ();
                boolean mandatory = false;
                lock {
                    CachedPurpose? cached = purposeCache[p.purposeName];
                    if cached != () {
                        elementNames = cached.elementNames.clone();
                        description = cached.description;
                        mandatory = !showConsentElements && cached.anyMandatory;
                    }
                }
                purposeList.push({
                    purposeName: p.purposeName,
                    mandatory: mandatory,
                    purposeDescription: description,
                    elements: showConsentElements ? elementNames : []
                });
            }

            string[] allRequestedScopes = [];
            foreach string s in re ` `.split(scopeStr) {
                if s != "" { allRequestedScopes.push(s); }
            }

            PurposeConsentData purposeData = {
                flow: "purpose",
                sessionDataKeyConsent: sessionDataKeyConsent,
                spId: spId,
                appName: application,
                user: consentUser,
                purposes: purposeList,
                scopes: allRequestedScopes,
                mandatoryClaims: mandatoryClaims,
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

    # Validates the consent token and stores the user's decision in WSO2 IS.
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

        string|() sub = tokenPayload.sub;
        if sub is () || sub == "" {
            return error("Consent token missing subject");
        }
        string trustedUser = sub;
        string trustedApp = (tokenPayload["app"] ?: "").toString();
        string tokenSdkc = (tokenPayload["sdkc"] ?: "").toString();
        string trustedClientId = (tokenPayload["cid"] ?: "").toString();

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
            // Scope flow: store all approved + hidden scopes in the consent properties
            log:printInfo("Processing consent submission for scope flow");
            int? scopeValidityTime = ();
            string? expiryOpt = submission.consentExpiryOption;
            if expiryOpt == "24h" {
                scopeValidityTime = 86400;
            } else if expiryOpt == "3months" {
                scopeValidityTime = 7776000;
            } else if expiryOpt != "never" {
                scopeValidityTime = scopeConsentValidityTime;
            }
            log:printDebug(`Setting consent validity to ${scopeValidityTime} seconds`);

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

            // Resolve EHR launch context and add OH_patient / OH_encounter as approved scopes
            foreach string s in (hiddenScopes ?: []) {
                if s.startsWith("OH_launch/") && s.length() > 10 {
                    string launchId = s.substring(10);
                    EhrLaunchContext?|error launchCtxResult = resolveLaunchContext(launchId);
                    if launchCtxResult is EhrLaunchContext {
                        string? patientId = launchCtxResult.patientId;
                        string? encounterId = launchCtxResult.encounterId;
                        if patientId is string && patientId != "" {
                            scopesToStore.push(string `OH_patient/${patientId}`);
                            log:printDebug("[EHR] Added patient scope from launch context", patientId = patientId);
                        }
                        if encounterId is string && encounterId != "" {
                            scopesToStore.push(string `OH_encounter/${encounterId}`);
                            log:printDebug("[EHR] Added encounter scope from launch context", encounterId = encounterId);
                        }
                    } else if launchCtxResult is error {
                        log:printError("[EHR] Failed to resolve launch context", launchId = launchId, 'error = launchCtxResult);
                    }
                    break;
                }
            }

            check ensurePurposesCached();
            // Bind every element of the scope purpose (the SMART scopes themselves are kept in properties)
            string scopePurposeId = "";
            string[] scopeElementIds = [];
            lock {
                CachedPurpose? cached = purposeCache[scopeConsent.purposeName];
                if cached != () {
                    scopePurposeId = cached.id;
                    scopeElementIds = cached.elementIds.toArray().clone();
                }
            }
            IsConsentElementRef[] scopeElements = from string elId in scopeElementIds select {id: elId};
            if scopePurposeId == "" || scopeElements.length() == 0 {
                return error(string `Consent purpose '${scopeConsent.purposeName}' has no elements to bind`);
            }

            map<string> properties = {
                [PROP_SESSION_KEY]: submission.sessionDataKeyConsent,
                [PROP_SP_ID]: submission.spId,
                [PROP_APPLICATION]: trustedApp,
                [PROP_APPROVED_SCOPES]: string:'join(" ", ...scopesToStore)
            };
            if expiryOpt is string {
                properties[PROP_EXPIRY_OPTION] = expiryOpt;
            }
            if trustedClientId != "" {
                properties[PROP_CLIENT_ID] = trustedClientId;
            }

            IsConsentCreateRequest payload = {
                subjectId: trustedUser,
                serviceId: serviceId,
                purposes: [{id: scopePurposeId, elements: scopeElements}],
                properties: properties
            };
            if scopeValidityTime is int {
                payload.expiryTime = expiryFromSeconds(scopeValidityTime);
            }

            string newConsentId = check createConsent(payload);
            log:printDebug("Scope consent stored in WSO2 IS", consentId = newConsentId);
            if singleConsentPerUser {
                check revokeActiveConsents(trustedUser, newConsentId);
            }

        } else if consentedPurposes != () {
            check ensurePurposesCached();
            // Purpose flow: bind only the approved elements of each purpose (the IS consent model
            // records approvals by presence — an element not listed is not consented)
            IsConsentPurposeBinding[] bindings = [];
            foreach PurposeConsentConfig purposeConfig in purposeConsent {
                string purposeName = purposeConfig.purposeName;

                string purposeId = "";
                string[] cachedElementNames = [];
                map<string> cachedElementIds = {};
                lock {
                    CachedPurpose? cached = purposeCache[purposeName];
                    if cached != () {
                        purposeId = cached.id;
                        cachedElementNames = cached.elementNames.clone();
                        cachedElementIds = cached.elementIds.clone();
                    }
                }

                string[] approvedElementNames = [];
                foreach ConsentedPurpose cp in consentedPurposes {
                    if cp.purposeName == purposeName {
                        approvedElementNames = cp.consentedElements;
                        break;
                    }
                }
                boolean purposeConsented = false;
                foreach ConsentedPurpose cp in consentedPurposes {
                    if cp.purposeName == purposeName {
                        purposeConsented = true;
                        break;
                    }
                }

                IsConsentElementRef[] elements = [];
                foreach string elName in cachedElementNames {
                    boolean isApproved = showConsentElements
                        ? approvedElementNames.indexOf(elName) !is ()
                        : purposeConsented;
                    string? elId = cachedElementIds[elName];
                    if isApproved && elId is string {
                        elements.push({id: elId});
                    }
                }
                if elements.length() > 0 {
                    bindings.push({id: purposeId, elements: elements});
                }
            }

            if bindings.length() == 0 {
                log:printInfo("No purposes consented — no consent record created");
                if singleConsentPerUser {
                    check revokeActiveConsents(trustedUser, ());
                }
                return {status: "success", message: "Consent approved successfully"};
            }

            map<string> purposeProperties = {
                [PROP_SESSION_KEY]: submission.sessionDataKeyConsent,
                [PROP_SP_ID]: submission.spId,
                [PROP_APPLICATION]: trustedApp
            };
            if trustedClientId != "" {
                purposeProperties[PROP_CLIENT_ID] = trustedClientId;
            }
            IsConsentCreateRequest payload = {
                subjectId: trustedUser,
                serviceId: serviceId,
                purposes: bindings,
                expiryTime: expiryFromSeconds(scopeConsentValidityTime),
                properties: purposeProperties
            };

            string newConsentId = check createConsent(payload);
            log:printDebug("Purpose consent created in WSO2 IS", consentId = newConsentId);
            if singleConsentPerUser {
                check revokeActiveConsents(trustedUser, newConsentId);
            }
        }

        return {status: "success", message: "Consent approved successfully"};
    }
}

// Extracts a single query parameter value from a URL-encoded query string.
isolated function extractQueryParam(string queryString, string key) returns string {
    if queryString == "" {
        return "";
    }
    foreach string pair in re `&`.split(queryString) {
        int? eqIdx = pair.indexOf("=");
        if eqIdx is () {
            continue;
        }
        string pKey = pair.substring(0, eqIdx);
        string pVal = pair.substring(eqIdx + 1);
        if pKey == key {
            string|error decoded = url:decode(pVal, "UTF-8");
            return decoded is string ? decoded : pVal;
        }
    }
    return "";
}
