# IAM Service Extensions

A Ballerina service that extends the access token issuance flow of WSO2 Identity Server (IS) via a **Pre-Issue Access Token action**. This implements SMART on FHIR authorization semantics by intercepting token requests before they are issued and performing scope validation, expansion, and custom claim injection.

## Overview

WSO2 IS invokes this service at the `POST /pre-issue-access-token` endpoint before issuing an access token. The service processes the incoming event, applies the logic described below, and responds with a set of patch operations that IS applies to the token being issued.

---

## Core Functionalities

### 1. Consent-Based Scope Filtering

When a token request carries a `sessionDataKeyConsent` (present in the session data of the event), the service fetches the set of pre-approved scopes from a downstream consent service before any scope processing occurs.

- Only scopes present in the approved scope list are permitted into the final token.
- Scopes listed in `alwaysAllowedScopes` (default: `openid`) bypass the consent check entirely.
- If the consent service call fails, token issuance is denied with an `ERROR` response.
- If no approved scopes are found for the given consent key, token issuance is denied.

### 2. SMART Scope Validation and Expansion

SMART on FHIR scopes follow the format `<context>/<resource>.<operations>` (e.g. `patient/Observation.cruds`). The service validates and expands these scopes before they are written into the token.

**Validation rules:**
- Scopes must match the pattern `(patient|user|system)/<resource>.(cruds|subset)`.
- `system/*` scopes are only permitted for the `client_credentials` grant type.
- `patient/*` and `user/*` scopes are not permitted for the `client_credentials` grant type.
- Scopes present in the token request but absent from the original access token scope list are ignored.

**Expansion:**
Compound operation strings are split into individual single-character operation scopes:

```text
patient/Observation.cruds  →  patient/Observation.c
                               patient/Observation.r
                               patient/Observation.u
                               patient/Observation.d
                               patient/Observation.s
```

Single-operation scopes (e.g. `patient/Observation.r`) are passed through as-is.

Non-SMART scopes (those that do not match the `patient|user|system` prefix pattern) are passed through without modification.

### 3. EHR Launch Context Resolution

When the approved scope set contains an `OH_launch/<launchId>` scope, the service resolves the EHR launch context from the configured `ehrContextResolveUrl`.

- The resolved context may include a `patientId` and/or `encounterId`.
- If `redirect_uri` is present in the token request, it is validated against the `aud` field of the launch context. A mismatch causes token issuance to be denied, preventing launch contexts from being used by unintended applications.
- If `patientId` was already resolved from an `OH_patient/<id>` scope in the approved list, the launch context patient ID is not used (the explicit scope takes precedence).

### 4. Patient ID Resolution

The service attempts to resolve a patient ID through the following priority chain, stopping at the first successful resolution:

1. **`OH_patient/<id>` scope** — if the approved scope list contains this internal scope, the embedded patient ID is used directly.
2. **EHR launch context** — if launch context resolution (step 3 above) returns a `patientId`, it is used.
3. **SCIM user profile** — if the token request includes an authenticated user ID, the service fetches the user from the SCIM2 endpoint and:
   - Checks whether the user belongs to the configured patient group (`scimPatientGroupName`, default: `patient`).
   - Reads the `fhirUser` attribute from the SCIM custom schema extension (`urn:scim:schemas:extension:custom:User`).
   - Parses the patient ID from the `Patient/<id>` FHIR reference.

### 5. Custom Claim Injection

After resolving patient and encounter identifiers, the service injects them as custom claims into the access token:

| Claim | Source |
|---|---|
| `patient` | Resolved patient ID (from scope, launch context, or SCIM) |
| `encounter` | Resolved encounter ID from EHR launch context |

These claims are added via `add` patch operations on `/accessToken/claims`.

---

## Configuration

All runtime configuration is done via `Config.toml`.

```toml
# Listener
hostname = "localhost"
port = 9090

# Consent service — approved scopes lookup endpoint
approvedScopesApiBaseUrl = "http://localhost:9091/approved-scopes"

# EHR launch context resolve endpoint (leave empty to disable EHR launch support)
ehrContextResolveUrl = "http://localhost:9092/launch-context"

# SCIM2 user lookup (used to resolve patient ID from logged-in user's profile)
scimApiBaseUrl = "https://localhost:9443"
scimApiPath = "/scim2/Users"
scimApiUsername = "<username>"
scimApiPassword = "<password>"
# Required only when scimApiBaseUrl uses HTTPS with a custom certificate
scimApiTrustStorePath = "/path/to/truststore.p12"
scimApiTrustStorePassword = "truststorepassword"

# SCIM group name that identifies patient users
scimPatientGroupName = "patient"

# SCIM custom schema attribute that holds the FHIR user reference (e.g. "Patient/123")
fhirUserAttributeName = "fhirUser"

# Scopes that are always included in the token regardless of the approved scope list
alwaysAllowedScopes = ["openid", "profile"]
```

---

## Internal Scopes

The following internal scope conventions are used to carry context through the consent approval flow:

| Scope | Purpose |
|---|---|
| `OH_patient/<patientId>` | Carries the patient ID from consent approval into the token |
| `OH_launch/<launchId>` | Carries the EHR launch context ID from consent approval into the token |

These scopes are not written into the final access token. They are consumed by this service during processing and replaced with the resolved `patient` and `encounter` claims.

---

## Building and Running

```bash
bal build
bal run
```
