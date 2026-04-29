// Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).

// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;
import ballerina/jwt;
import ballerina/log;

listener http:Listener consentBffListener = new (9095);

# HTTP 200 response for the bind-consent-to-token endpoint.
# Spreads http:Ok to enforce status 200 with a strongly-typed body.
type BindConsentToTokenResponse record {|
    *http:Ok;
    # The pre-issue-access-token action response payload
    PreIssueTokenActionResponse body;
|};

// Calls the Asgardeo API with a bearer token obtained from the token provider.
isolated function callAsgardeo(string path) returns json|error {
    string token = check getAsgardeoToken();
    http:Response response = check asgardeoClient->get(path, {"Authorization": string `Bearer ${token}`});

    // There will not be an unauthorized response in normal circumstances, since a new token will be fetched each time.
    if response.statusCode == http:STATUS_UNAUTHORIZED {
        log:printWarn("Received 401 from Asgardeo — getting access token and again");
        token = check getAsgardeoToken();
        response = check asgardeoClient->get(path, {"Authorization": string `Bearer ${token}`});
    }

    if response.statusCode != http:STATUS_OK {
        return error(string `Asgardeo API error: HTTP ${response.statusCode}`);
    }

    return response.getJsonPayload();
}

@http:ServiceConfig {
    cors: {
        allowOrigins: [corsAllowedOrigin],
        allowCredentials: false,
        allowHeaders: ["Content-Type", "Authorization"],
        allowMethods: ["GET", "POST", "OPTIONS"]
    }
}
service /v1 on consentBffListener {

    # Returns application info and consent purposes for the given session.
    #
    # + sessionDataKeyConsent - The consent session key from the identity server
    # + spId - The service provider (application) ID requesting consent
    # + return - Application name, scope, and purposes, or an error
    isolated resource function get get\-consent\-data(string sessionDataKeyConsent, string spId)
            returns ConsentPageData|error {

        log:printInfo("Fetching consent data", sessionDataKeyConsent = sessionDataKeyConsent, spId = spId);

        json consentKeyJson = check callAsgardeo(
            string `/api/identity/auth/v1.1/data/OauthConsentKey/${sessionDataKeyConsent}`
        );
        AsgardeoConsentKeyResponse consentKeyData = check consentKeyJson.cloneWithType();

        log:printInfo("Received consent key data", application = consentKeyData.application, scope = consentKeyData.scope);

        string loggedInUser = consentKeyData.loggedInUser;
        string? existingConsentId = ();
        string[] previouslyConsentedPurposeNames = [];
        map<string[]> previouslyConsentedElements = {};

        if singleConsentPerUser {
            do {
                string searchPath = string `/consents?userIds=${loggedInUser}&clientIds=${tppClientId}&consentStatuses=ACTIVE,CREATED&limit=1`;
                log:printDebug("get-consent-data: querying OpenFGC for existing consent", searchPath = searchPath);
                http:Response searchResp = check openfgcClient->get(searchPath, {"org-id": orgId});

                if searchResp.statusCode == http:STATUS_OK {
                    json searchJson = check searchResp.getJsonPayload();
                    OpenFGCConsentSearchResponse searchResult = check searchJson.cloneWithType();

                    if searchResult.data.length() > 0 {
                        OpenFGCConsentSearchRecord existing = searchResult.data[0];
                        existingConsentId = existing.id;
                        log:printInfo("get-consent-data: found existing consent", consentId = existing.id);

                        OpenFGCConsentSearchPurpose[]? existingPurposes = existing.purposes;
                        if existingPurposes != () {
                            string[] approvedNames = [];
                            foreach OpenFGCConsentSearchPurpose p in existingPurposes {
                                string[] approvedElems = [];
                                foreach OpenFGCConsentSearchElement e in p.elements {
                                    if e.isUserApproved {
                                        approvedElems.push(e.name);
                                    }
                                }
                                if approvedElems.length() > 0 {
                                    previouslyConsentedElements[p.name] = approvedElems;

                                    boolean addToPreviouslyConsented;
                                    if showConsentElements {
                                        addToPreviouslyConsented = true;
                                    } else {
                                        // purpose-only mode: only pre-check if ALL configured elements were approved
                                        ConsentPurposeConfig? matchedConfig = ();
                                        foreach ConsentPurposeConfig cp in consentPurpose {
                                            if cp.name == p.name {
                                                matchedConfig = cp;
                                                break;
                                            }
                                        }
                                        if matchedConfig == () {
                                            addToPreviouslyConsented = false;
                                        } else {
                                            ConsentPurposeConfig matchedPurpose = matchedConfig;
                                            boolean allApproved = true;
                                            foreach string configEl in matchedPurpose.elements {
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
                                            addToPreviouslyConsented = allApproved;
                                        }
                                    }

                                    if addToPreviouslyConsented {
                                        approvedNames.push(p.name);
                                        log:printInfo("get-consent-data: purpose was previously consented", purposeName = p.name);
                                    } else {
                                        log:printInfo("get-consent-data: purpose partially consented — not pre-checking in purpose-only mode", purposeName = p.name);
                                    }
                                }
                            }
                            previouslyConsentedPurposeNames = approvedNames;
                        }
                    } else {
                        log:printDebug("get-consent-data: no existing consent found for user", loggedInUser = loggedInUser);
                    }
                } else {
                    log:printWarn("get-consent-data: OpenFGC search returned non-200 — continuing without pre-selection", statusCode = searchResp.statusCode);
                }
            } on fail error e {
                log:printWarn("get-consent-data: OpenFGC consent lookup failed — continuing without pre-selection", e = e.message());
            }
        }

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
                "app": consentKeyData.application,
                "sdkc": sessionDataKeyConsent
            }
        };
        string consentToken = check jwt:issue(issuerConfig);

        Purpose[] purposeList = [];
        foreach ConsentPurposeConfig p in consentPurpose {
            string? desc = p.description;
            purposeList.push({
                purposeName: p.name,
                mandatory: p.mandatory,
                purposeDescription: desc == () || desc == "" ? () : desc,
                elements: showConsentElements ? p.elements : []
            });
        }

        return {
            appName: consentKeyData.application,
            scope: consentKeyData.scope,
            loggedInUser: loggedInUser,
            purposes: purposeList,
            existingConsentId: existingConsentId,
            previouslyConsentedPurposeNames: previouslyConsentedPurposeNames.length() > 0 ? previouslyConsentedPurposeNames : (),
            previouslyConsentedElements: previouslyConsentedElements.length() > 0 ? previouslyConsentedElements : (),
            consentToken: consentToken
        };
    }

    # Submits the user's consent decision to the identity server.
    #
    # + submission - The consent decision payload
    # + return - Submission result, or an error
    isolated resource function post submit\-consent(@http:Payload ConsentSubmission submission)
            returns ConsentSubmissionResponse|error {

        log:printInfo("Consent submitted", submission = submission.toString());

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

        log:printInfo("Consent token validated", loggedInUser = trustedUser, application = trustedApp);

        if submission.approved {
            OpenFGCConsentPurposeItem[] purposeItems = [];
            foreach ConsentPurposeConfig purposeConfig in consentPurpose {
                boolean purposeConsented = submission.consentedPurposes.filter(
                    cp => cp.purposeName == purposeConfig.name
                ).length() > 0;

                OpenFGCConsentElementApproval[] elements = [];
                if showConsentElements {
                    string[] approvedElementNames = [];
                    foreach ConsentedPurpose cp in submission.consentedPurposes {
                        if cp.purposeName == purposeConfig.name {
                            approvedElementNames = cp.consentedElements;
                            break;
                        }
                    }
                    foreach string elName in purposeConfig.elements {
                        boolean isApproved = approvedElementNames.filter(e => e == elName).length() > 0;
                        elements.push({name: elName, isUserApproved: isApproved});
                    }
                } else {
                    foreach string elName in purposeConfig.elements {
                        elements.push({name: elName, isUserApproved: purposeConsented});
                    }
                }
                purposeItems.push({name: purposeConfig.name, elements: elements});
            }

            OpenFGCAuthorization[] authorizations = [
                {
                    userId: trustedUser,
                    'type: "authorisation",
                    status: "APPROVED",
                    resources: {
                        spId: submission.spId,
                        application: trustedApp
                    }
                }
            ];

            OpenFGCConsentCreatePayload consentPayload = {
                'type: consentType,
                purposes: purposeItems,
                authorizations: authorizations,
                attributes: {
                    "sessionDataKeyConsent": submission.sessionDataKeyConsent
                }
            };

            http:Request req = new;
            req.setJsonPayload(consentPayload.toJson());
            req.addHeader("org-id", orgId);
            req.addHeader("TPP-client-id", tppClientId);

            string? existingId = singleConsentPerUser ? submission.existingConsentId : ();
            if existingId != () {
                http:Response updateResp = check openfgcClient->put(string `/consents/${existingId}`, req);
                if updateResp.statusCode != http:STATUS_OK {
                    return error(string `OpenFGC consent update failed: HTTP ${updateResp.statusCode}`);
                }
                log:printInfo("OpenFGC consent updated", consentId = existingId);
            } else {
                http:Response consentResp = check openfgcClient->post("/consents", req);
                if consentResp.statusCode != http:STATUS_CREATED {
                    return error(string `OpenFGC consent creation failed: HTTP ${consentResp.statusCode}`);
                }
                json respJson = check consentResp.getJsonPayload();
                OpenFGCConsentCreatedResponse created = check respJson.cloneWithType();
                log:printInfo("OpenFGC consent created", consentId = created.id ?: "unknown", status = created.status ?: "unknown");
            }
        }

        return {
            status: "success",
            message: string `Consent ${submission.approved ? "approved" : "denied"} successfully`
        };
    }

    # Asgardeo pre-issue-access-token action handler.
    # Looks up the OpenFGC consent ID for the given session and injects it
    # as a `consent_id` claim into the access token.
    #
    # + request - Pre-issue-access-token action payload from Asgardeo
    # + return - Action response with consent_id claim operation, or an error
    isolated resource function post bind\-consent\-to\-token(@http:Payload PreIssueTokenActionRequest request)
            returns BindConsentToTokenResponse|error {

        log:printDebug("bind-consent-to-token: received request", payload = request.toJson().toString());

        string sessionDataKeyConsent = request.event?.session?.sessionDataKeyConsent ?: "";

        log:printDebug("bind-consent-to-token: extracted sessionDataKeyConsent", sessionDataKeyConsent = sessionDataKeyConsent);

        if sessionDataKeyConsent == "" {
            log:printWarn("bind-consent-to-token: sessionDataKeyConsent missing in request — returning SUCCESS with no operations");
            return {body: {actionStatus: "SUCCESS"}};
        }

        map<string|string[]> attrHeaders = {
            "org-id": orgId,
            "TPP-client-id": tppClientId
        };

        string queryPath = string `/consents/attributes?key=sessionDataKeyConsent&value=${sessionDataKeyConsent}`;
        log:printDebug("bind-consent-to-token: querying OpenFGC attributes", queryPath = queryPath);

        http:Response attrResp = check openfgcClient->get(queryPath, attrHeaders);

        log:printDebug("bind-consent-to-token: OpenFGC attributes response", statusCode = attrResp.statusCode);

        if attrResp.statusCode != http:STATUS_OK {
            log:printWarn("bind-consent-to-token: OpenFGC attribute lookup failed — returning SUCCESS with no operations", statusCode = attrResp.statusCode);
            return {body: {actionStatus: "SUCCESS"}};
        }

        json attrJson = check attrResp.getJsonPayload();
        log:printDebug("bind-consent-to-token: OpenFGC attributes payload", payload = attrJson.toString());

        OpenFGCConsentAttributeSearchResponse attrResult = check attrJson.cloneWithType();

        if attrResult.count == 0 || attrResult.consentIds.length() == 0 {
            log:printWarn("bind-consent-to-token: no consent found — returning SUCCESS with no operations", sessionDataKeyConsent = sessionDataKeyConsent);
            return {body: {actionStatus: "SUCCESS"}};
        }

        string consentId = attrResult.consentIds[0];
        log:printDebug("bind-consent-to-token: found consent ID", consentId = consentId, totalMatches = attrResult.count);
        log:printInfo("bind-consent-to-token: injecting consent_id claim", consentId = consentId);

        TokenOperation claimOp = {
            op: "add",
            path: "/accessToken/claims/-",
            value: {name: "consent_id", value: consentId}
        };
        return {body: {actionStatus: "SUCCESS", operations: [claimOp]}};
    }
}
