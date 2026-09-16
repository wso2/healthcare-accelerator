# Consent Enforce Policy

A Bijira API mediation policy that enforces consent validation on every inbound request. It extracts a `consent_id` from the caller's JWT, calls the openFGC consent service to check validity, and blocks the request with a `403` if the consent is missing, invalid, or the service is unreachable.

---

## How It Works

```
Incoming request
      │
      ▼
Extract token: X-JWT-Assertion header (primary, set by Bijira gateway)
      │  or Authorization: Bearer <token> (fallback for direct calls)
      ▼
Decode JWT → read consent_id claim
      │
      ▼
POST /consents/validate  →  openFGC consent service
      │
      ├─ isValid: true  →  allow request through
      │
      └─ isValid: false / error  →  403 Forbidden
```

The policy runs on the **request flow** only. Response and fault flows are pass-through.

---

## Policy Parameters

| Parameter | Type | Description | Example |
|---|---|---|---|
| `openFgcBaseUrl` | `string` | Base URL of the openFGC consent service | `https://<host>/cms-paas/openfgc-consent-service/v1.0` |
| `orgId` | `string` | Value sent as the `org-id` header to openFGC | `ORG-001` |
| `failOnMissingConsent` | `boolean` | If `true`, block the request when `consent_id` is absent from the JWT. If `false`, allow it through silently. | `true` |

---

## Security & Trust Boundary

### Gateway-Level JWT Validation
In production deployments (such as WSO2 API Manager or Choreo Bijira Gateway):
1. **Gateway as PEP**: The API Gateway acts as the primary Policy Enforcement Point (PEP) for authentication. When incoming requests carry a Bearer JWT, the Gateway cryptographically validates the token against the Identity Provider (JWKS/certificate), validates standard claims (`exp`, `nbf`, `iss`, `aud`), and enforces configured scopes.
2. **Untrusted Token Rejection**: If the token is missing, expired, tampered with, or signed by an unknown authority, the Gateway terminates the request with `401 Unauthorized` before any mediation policy runs.
3. **Backend Assertion Forwarding**: Upon successful validation, the Gateway generates and forwards a trusted assertion via the `X-JWT-Assertion` header to downstream mediation policies and backends.

### Policy Responsibility
Because cryptographic integrity and authentication are guaranteed upstream by the Gateway:
- `consentEnforcePolicy` operates within the trusted security perimeter.
- The policy uses `jwt:decode()` solely to extract the `consent_id` claim needed for consent enforcement; it does not duplicate signature or issuer/audience validation.
- The `Authorization` header fallback exists to support local development and non-gateway test environments. In production, APIs must declare security requirements (e.g., as in `simple-api.yaml`) so that all requests are authenticated by the Gateway prior to policy execution.

---

## JWT Requirements

The policy extracts the token from the `X-JWT-Assertion` header when present (set automatically by the Bijira/WSO2 APIM gateway). For direct calls that bypass the gateway, it falls back to the `Authorization: Bearer <token>` header.

The JWT must contain a `consent_id` claim in its payload:

```json
{
  "sub": "user@example.com",
  "consent_id": "5b35786d-8d12-4dd6-8784-1577d8dccc02",
  ...
}
```

---

## openFGC Validate Request

The policy sends:

```
POST {openFgcBaseUrl}/consents/validate
Content-Type: application/json
Accept: application/json
org-id: {orgId}

{"consentId": "<value from JWT>"}
```

Expected success response (`200 OK`):

```json
{
  "isValid": true
}
```

---

## Error Responses

All error responses are `403 Forbidden` with the following JSON body:

```json
{
  "error": "<error_code>",
  "status": ""
}
```

| `error` value | Cause |
|---|---|
| `missing_consent_id` | `Authorization` header absent, JWT decode failed, or `consent_id` claim not in JWT (only when `failOnMissingConsent=true`) |
| `consent_service_error` | openFGC HTTP client failed to initialise, network error, non-JSON response, or `isValid` field missing/wrong type |
| `consent_not_found` | openFGC returned a non-200 status code |
| `consent_invalid` | openFGC returned `isValid: false` |
| `consent_resource_not_approved` | `isValid` is true but the requested FHIR resource type has `isUserApproved: false` in at least one consent purpose. The `status` field in the response body carries the resource type name. |
| `consent_resource_not_found` | `isValid` is true but the requested FHIR resource type is not listed in any element of any consent purpose. The `status` field carries the resource type name. |

---

## Logging

The policy emits structured logs at two levels:

| Level | Events |
|---|---|
| `INFO` | Missing auth header, JWT decode failure, missing `consent_id`, non-200 from openFGC, consent invalid, consent validated |
| `DEBUG` | consent_id extracted, HTTP client initialised, outgoing validate call, raw openFGC response |
| `ERROR` | HTTP client init failure, network error, JSON parse failure, `isValid` type error |

`DEBUG` logs are suppressed by default. To enable them, set the environment variable:

```
BAL_CONFIG_DATA={"ballerina":{"log":{"level":"DEBUG"}}}
```

---

## Package Info

| Field | Value |
|---|---|
| Org | `wso2healthcare` |
| Name | `consentEnforcePolicy` |
| Version | `1.0.4` |
| Distribution | Ballerina `2201.12.8` |
