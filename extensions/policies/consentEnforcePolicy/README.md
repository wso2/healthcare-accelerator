# Consent Enforce Policy

A Bijira API mediation policy that enforces consent validation on every inbound request. It extracts a `consent_id` from the caller's JWT, calls the WSO2 Identity Server (7.3.0+) consent management API to check validity, and blocks the request with a `403` if the consent is missing, invalid, or the service is unreachable.

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
GET /consents/{consentId}/validate  →  WSO2 IS consent management API
      │
      ├─ state: ACTIVE  →  GET /consents/{consentId}, check the requested FHIR resource
      │                    is covered by an approved element  →  allow request through
      │
      └─ any other state / error  →  403 Forbidden
```

The policy runs on the **request flow** only. Response and fault flows are pass-through.

---

## Policy Parameters

| Parameter | Type | Description | Example |
|---|---|---|---|
| `isBaseUrl` | `string` | Base URL of the WSO2 Identity Server. The consent API is called at `{isBaseUrl}/api/identity/consent-mgt/v2.0` and tokens are obtained from `{isBaseUrl}/oauth2/token` | `https://localhost:9443` |
| `clientId` | `string` | Client ID of the management application (M2M) in WSO2 IS | `<client-id>` |
| `clientSecret` | `string` | Client secret of the management application | `<client-secret>` |
| `failOnMissingConsent` | `boolean` | If `true`, block the request when `consent_id` is absent from the JWT. If `false`, allow it through silently. | `true` |

---

## JWT Requirements

The policy reads the token from the `X-JWT-Assertion` header when present (set automatically by the Bijira/WSO2 APIM gateway). For direct calls that bypass the gateway, it falls back to the `Authorization: Bearer <token>` header.

The JWT must contain a `consent_id` claim in its payload:

```json
{
  "sub": "user@example.com",
  "consent_id": "5b35786d-8d12-4dd6-8784-1577d8dccc02",
  ...
}
```

The policy does **not** verify the JWT signature — it only decodes and reads claims.

---

## WSO2 IS Consent Requests

The policy uses the same management application that the other healthcare accelerator services use. The application must be authorized for the **Consent Management API** with the `internal_consent_mgt_consent_view` scope. The policy obtains an access token using the client credentials grant and caches it until shortly before it expires.

1. Validate the consent:

```
GET {isBaseUrl}/api/identity/consent-mgt/v2.0/consents/{consentId}/validate
Authorization: Bearer <access token>
Accept: application/json
```

Expected success response (`200 OK`) for a usable consent:

```json
{
  "state": "ACTIVE",
  "expiryTime": 1766383796000
}
```

Any state other than `ACTIVE` (`PENDING`, `REJECTED`, `REVOKED`, `EXPIRED`) is rejected.

2. Fetch the consent record (`GET .../consents/{consentId}`) and check the `purposes[].elements[]` list. Elements present in an active consent are the ones the user approved. The requested FHIR resource type (first path segment) must match an element `name` either exactly (e.g. `Patient`) or as a SMART scope for that resource (e.g. `patient/Patient.rs`).

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
| `consent_service_error` | WSO2 IS HTTP client failed to initialise, token request failed, network error, non-JSON response, or `state` field missing/wrong type |
| `consent_not_found` | The validate endpoint returned a non-200 status code (e.g. `404` for an unknown consent) |
| `consent_invalid` | The consent `state` is not `ACTIVE` |
| `consent_resource_not_found` | The consent is `ACTIVE` but the requested FHIR resource type is not covered by any element of any consent purpose. The `status` field carries the resource type name. |

---

## Logging

The policy emits structured logs at two levels:

| Level | Events |
|---|---|
| `INFO` | Missing auth header, JWT decode failure, missing `consent_id`, non-200 from WSO2 IS, consent not active, consent validated |
| `DEBUG` | consent_id extracted, HTTP client initialised, outgoing validate call, raw WSO2 IS validate response |
| `ERROR` | HTTP client init failure, token request failure, network error, JSON parse failure, `state` type error |

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
