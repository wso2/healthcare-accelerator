import ballerina/http;
import ballerina/jwt;
import ballerina/log;
import ballerina/time;
import choreo/mediation;

const string CONSENT_API_PATH = "/api/identity/consent-mgt/v2.0";
const string CONSENT_VIEW_SCOPE = "internal_consent_mgt_consent_view";

http:Client? isClient = ();
string cachedToken = "";
int cachedTokenExpiry = 0;

// Returns a cached client-credentials access token for the WSO2 IS consent management API.
function getIsToken(http:Client isHttp, string clientId, string clientSecret) returns string|error {
    int now = time:utcNow()[0];
    lock {
        if cachedToken != "" && cachedTokenExpiry > now + 30 {
            return cachedToken;
        }
    }
    http:Request tokenReq = new;
    tokenReq.setHeader("Authorization", "Basic " + string:toBytes(clientId + ":" + clientSecret).toBase64());
    tokenReq.setTextPayload("grant_type=client_credentials&scope=" + CONSENT_VIEW_SCOPE, "application/x-www-form-urlencoded");
    http:Response tokenResp = check isHttp->post("/oauth2/token", tokenReq);
    if tokenResp.statusCode != 200 {
        return error("Token endpoint returned " + tokenResp.statusCode.toString());
    }
    json body = check tokenResp.getJsonPayload();
    map<json> bodyMap = check body.ensureType();
    string accessToken = check bodyMap["access_token"].ensureType();
    int expiresIn = bodyMap["expires_in"] is int ? check bodyMap["expires_in"].ensureType() : 300;
    lock {
        cachedToken = accessToken;
        cachedTokenExpiry = now + expiresIn;
    }
    return accessToken;
}

@mediation:RequestFlow
public function enforceRequestFlowConsent(mediation:Context ctx, http:Request req, string isBaseUrl, string clientId, string clientSecret, boolean failOnMissingConsent)
                                returns http:Response|false|error|() {

    log:printInfo("Request Flow Consent Enforcement Policy invoked");
    string resourcePath = ctx.resourcePath().toString();

    string[] headerNames = req.getHeaderNames();
    log:printInfo("Incoming request headers", headers = headerNames.toString(), resourcePath = resourcePath);

    // Bijira (WSO2 APIM) strips Authorization and forwards the decoded JWT via X-JWT-Assertion.
    // Fall back to Authorization header for direct/non-gateway invocations.
    string token = "";
    string|http:HeaderNotFoundError xJwt = req.getHeader("X-JWT-Assertion");
    if xJwt is string {
        token = xJwt;
    } else {
        string|http:HeaderNotFoundError authHeader = req.getHeader("Authorization");
        if authHeader is http:HeaderNotFoundError {
            log:printInfo("Authorization header not found", resourcePath = resourcePath);
            return failOnMissingConsent ? forbidden("missing_consent_id", "") : ();
        }
        token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }

    // Decode JWT
    [jwt:Header, jwt:Payload]|jwt:Error decoded = jwt:decode(token);
    if decoded is jwt:Error {
        log:printInfo("Failed to decode JWT", resourcePath = resourcePath);
        return failOnMissingConsent ? forbidden("missing_consent_id", "") : ();
    }
    var [_, payload] = decoded;

    // Extract consent_id
    string consentId = (payload["consent_id"] ?: "").toString();
    if consentId == "" {
        log:printInfo("consent_id not found in JWT", resourcePath = resourcePath);
        return failOnMissingConsent ? forbidden("missing_consent_id", "") : ();
    }
    log:printDebug("Extracted consent_id from JWT", consentId = consentId, resourcePath = resourcePath);

    // Lazy-init HTTP client
    http:Client isHttpClient;
    http:Client? existing = isClient;
    if existing is http:Client {
        isHttpClient = existing;
    } else {
        http:Client|http:ClientError newClient = new (isBaseUrl);
        if newClient is http:ClientError {
            log:printError("Failed to init WSO2 IS client", 'error = newClient);
            return forbidden("consent_service_error", "");
        }
        isClient = newClient;
        isHttpClient = newClient;
        log:printDebug("Initialized WSO2 IS HTTP client", baseUrl = isBaseUrl);
    }

    string|error accessToken = getIsToken(isHttpClient, clientId, clientSecret);
    if accessToken is error {
        log:printError("Failed to obtain WSO2 IS access token", 'error = accessToken);
        return forbidden("consent_service_error", "");
    }
    map<string> authHeaders = {"Authorization": "Bearer " + accessToken, "Accept": "application/json"};

    // Call WSO2 IS GET /consents/{consentId}/validate
    log:printDebug("Calling WSO2 IS consent validate endpoint", consentId = consentId, baseUrl = isBaseUrl);
    http:Response|http:ClientError validateResp = isHttpClient->get(CONSENT_API_PATH + "/consents/" + consentId + "/validate", authHeaders);

    if validateResp is http:ClientError {
        log:printError("WSO2 IS consent validate call failed", consentId = consentId, 'error = validateResp);
        return forbidden("consent_service_error", "");
    }

    if validateResp.statusCode != 200 {
        log:printInfo("WSO2 IS returned non-200 for consent validate", consentId = consentId, statusCode = validateResp.statusCode);
        return forbidden("consent_not_found", "");
    }

    // Parse response and check consent state
    json|error body = validateResp.getJsonPayload();
    if body is error {
        log:printError("Failed to parse WSO2 IS validate response", consentId = consentId);
        return forbidden("consent_service_error", "");
    }

    map<json>|error bodyMap = body.ensureType();
    if bodyMap is error {
        log:printError("Unexpected WSO2 IS validate response format", consentId = consentId);
        return forbidden("consent_service_error", "");
    }
    log:printDebug("WSO2 IS validate raw response", consentId = consentId, body = body.toString());

    string|error state = bodyMap["state"].ensureType();
    if state is error {
        log:printError("Failed to read state from WSO2 IS validate response", consentId = consentId, 'error = state);
        return forbidden("consent_service_error", "");
    }
    if state != "ACTIVE" {
        log:printInfo("Consent not active", consentId = consentId, state = state, resourcePath = resourcePath);
        return forbidden("consent_invalid", "");
    }

    // Extract FHIR resource type from first path segment
    string pathStr = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
    int? qIdx = pathStr.indexOf("?");
    if qIdx is int {
        pathStr = pathStr.substring(0, qIdx);
    }
    int? slashIdx = pathStr.indexOf("/");
    string fhirResourceType = slashIdx is int ? pathStr.substring(0, slashIdx) : pathStr;
    log:printDebug("Extracted FHIR resource type", resourceType = fhirResourceType, consentId = consentId);

    // Fetch the consent record to read the approved elements
    http:Response|http:ClientError consentResp = isHttpClient->get(CONSENT_API_PATH + "/consents/" + consentId, authHeaders);
    if consentResp is http:ClientError || consentResp.statusCode != 200 {
        log:printError("Failed to fetch consent record from WSO2 IS", consentId = consentId);
        return forbidden("consent_service_error", "");
    }
    json|error consentJson = consentResp.getJsonPayload();
    map<json>|error consentMap = consentJson is json ? consentJson.ensureType() : consentJson;
    if consentMap is error {
        log:printError("Unexpected WSO2 IS consent response format", consentId = consentId);
        return forbidden("consent_service_error", "");
    }

    json[]|error purposesArr = consentMap["purposes"].ensureType();
    if purposesArr is error {
        log:printError("purposes missing or wrong format", consentId = consentId);
        return forbidden("consent_service_error", "");
    }

    // Elements present in an active consent are the ones the user approved. An element covers
    // the requested resource when its name is the resource type (e.g. "Patient") or a SMART
    // scope for it (e.g. "patient/Patient.rs").
    boolean resourceFound = false;
    foreach json purpose in purposesArr {
        map<json>|error purposeMap = purpose.ensureType();
        if purposeMap is error { continue; }

        json[]|error elementsArr = purposeMap["elements"].ensureType();
        if elementsArr is error { continue; }

        foreach json element in elementsArr {
            map<json>|error elemMap = element.ensureType();
            if elemMap is error { continue; }

            string name = (elemMap["name"] ?: "").toString();
            if name == fhirResourceType || name.includes("/" + fhirResourceType + ".") {
                resourceFound = true;
            }
        }
    }

    if !resourceFound {
        log:printInfo("Resource type not found in consent",
            consentId = consentId, resourceType = fhirResourceType, resourcePath = resourcePath);
        return forbidden("consent_resource_not_found", fhirResourceType);
    }

    log:printInfo("Consent validated", consentId = consentId, resourceType = fhirResourceType, resourcePath = resourcePath);

    return ();
}

// @mediation:ResponseFlow
// public function enforceResponseFlowConsent(mediation:Context ctx, http:Request req, http:Response res, string isBaseUrl, string clientId, string clientSecret, boolean failOnMissingConsent)
//                                 returns http:Response|false|error|() {
//     return ();
// }

// @mediation:FaultFlow
// public function enforceFaultFlowConsent(mediation:Context ctx, http:Request req, http:Response? res, http:Response errFlowRes,
//                                     error e, string isBaseUrl, string clientId, string clientSecret, boolean failOnMissingConsent) returns http:Response|false|error|() {
//     return ();
// }

function forbidden(string reason, string consentStatus) returns http:Response {
    http:Response resp = new;
    resp.statusCode = 403;
    resp.setJsonPayload({"error": reason, "status": consentStatus});
    return resp;
}
