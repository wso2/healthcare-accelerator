# consent-app-bff vs consent-app-bff-v1 Comparison (+ iam-service-extensions)

## Overview

| Aspect | `consent-app-bff` | `consent-app-bff-v1` |
|--------|------------------|---------------------|
| Port | 9091 | 9095 |
| Service path | `/` (root) | `/v1` |
| Identity server | WSO2 IS (on-prem) | Asgardeo (cloud SaaS) |
| Auth to IS | Basic auth (username/password) | OAuth2 client credentials → Bearer token |
| Consent storage | H2 embedded database | OpenFGC consent service |
| Consent model | SMART scopes (flat list) | Purposes + elements (hierarchical) |
| Token security | None (no JWT) | HS256 JWT consent token (signed with clientSecret) |
| CORS | Not configured | Configured (`corsAllowedOrigin`) |
| Static file serving | Yes (serves Vite UI assets) | No (UI runs separately) |
| Token injection hook | Via `iam-service-extensions` (external service) | Built-in (`POST /v1/bind-consent-to-token`) |
| Tests | No | Yes (`bal test` with mocked Asgardeo + mock OpenFGC) |
| Package org | `ballerinax` | `wso2` |
| Ballerina distribution | 2201.12.10 | 2201.12.7 |

---

## Endpoints

### consent-app-bff

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/consent-context` | Fetches consent session data from WSO2 IS OauthConsentKey API; returns `sessionDataKeyConsent`, `spId`, `user`, `scopes[]` |
| `GET` | `/api/me` | Proxies SCIM2 user lookup to IS (`/scim2/Users/{userId}`) |
| `GET` | `/api/patients` | Searches SCIM2 users filtered by `fhirUser co Patient` |
| `POST` | `/consent` | Receives form submission; stores scopes in H2 DB; redirects to IS authorize endpoint |
| `GET` | `/approved-scopes` | Returns persisted approved scopes by consent key |
| `GET` | `/assets/*` | Serves Vite-built UI static assets (JS, CSS) |

### consent-app-bff-v1

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/v1/get-consent-data` | Calls Asgardeo OauthConsentKey API; optionally fetches existing consent from OpenFGC; returns `appName`, `purposes[]`, signed `consentToken`, previously consented data |
| `POST` | `/v1/submit-consent` | Validates consent token JWT; creates or updates consent record in OpenFGC |
| `POST` | `/v1/bind-consent-to-token` | Asgardeo pre-issue-access-token action hook; injects `consent_id` claim into the access token |

---

## Consent Flow Comparison

### consent-app-bff (WSO2 IS + H2 + SMART scopes)

1. UI calls `GET /api/consent-context` → BFF calls WSO2 IS `OauthConsentKey` API with Basic auth
2. BFF extracts scopes from IS response (recursive JSON search for `scope`/`scopes`/`requestedScopes` keys)
3. BFF also extracts `OH_launch/<launchId>` scope from `spQueryParams` if a `launch` param is present
4. UI shows scope checkboxes; user approves/denies
5. UI POSTs form to `POST /consent`
6. BFF stores approved scopes in H2 DB (`CONSENT_APPROVED_SCOPES` table, keyed by `sessionDataKeyConsent`)
7. BFF forwards consent decision to IS authorize endpoint (form POST with session cookies forwarded)
8. BFF responds with `302` redirect to the Location returned by IS

### consent-app-bff-v1 (Asgardeo + OpenFGC + Purposes)

1. UI calls `GET /v1/get-consent-data` → BFF fetches Bearer token via client credentials; calls Asgardeo `OauthConsentKey` API
2. If `singleConsentPerUser=true`, BFF looks up existing active consent in OpenFGC for this user+app
3. BFF issues a short-lived HS256 JWT (`consentToken`) embedding `loggedInUser`, `application`, `sessionDataKeyConsent`
4. BFF returns purposes list (from `Config.toml`) + previously consented state + `consentToken`
5. UI shows purpose checkboxes; user approves/denies
6. UI POSTs JSON to `POST /v1/submit-consent` including `consentToken`
7. BFF validates `consentToken` (issuer, audience, signature, session key match)
8. BFF creates or updates OpenFGC consent record with element-level approvals
9. UI then redirects to Asgardeo authorize URL directly (no BFF redirect)
10. On token issuance, Asgardeo calls `POST /v1/bind-consent-to-token` → BFF looks up `consent_id` in OpenFGC and injects it as a claim

---

## Identity Server Integration

| Aspect | `consent-app-bff` | `consent-app-bff-v1` |
|--------|------------------|---------------------|
| Identity server | WSO2 IS (on-prem, `localhost:9443`) | Asgardeo (cloud, `api.asgardeo.io`) |
| Auth mechanism | HTTP Basic (admin username/password) | OAuth2 client credentials (Bearer token) |
| TLS | Optional — configurable truststore (`.p12`) | Handled by Asgardeo cloud (no custom truststore) |
| Token retry | N/A | 401-retry: fetches fresh token and retries once |
| Consent key API | `GET {IS}/api/identity/auth/v1.1/data/OauthConsentKey/{key}` | `GET {Asgardeo}/api/identity/auth/v1.1/data/OauthConsentKey/{key}` |
| Authorize redirect | BFF POSTs form to IS and follows the redirect | UI directly POSTs form to Asgardeo authorize URL |
| Cookie forwarding | Yes — forwards `JSESSIONID`, `opbs`, `commonAuthId` | No |

---

## Consent Storage

| Aspect | `consent-app-bff` | `consent-app-bff-v1` |
|--------|------------------|---------------------|
| Storage backend | H2 embedded database (JDBC) | OpenFGC consent service (REST API) |
| What is stored | `sessionDataKeyConsent` → `approvedScopes` (space-separated) | Full consent record with purposes, elements, authorizations, and attributes |
| Update strategy | `MERGE` (upsert by consent key) | Create new consent OR update existing (controlled by `singleConsentPerUser`) |
| Retrieval | `GET /approved-scopes` | Via `POST /v1/bind-consent-to-token` (looks up by `sessionDataKeyConsent` attribute) |
| Scope of record | Scopes only | Purposes, elements, user, app, authorization status, attributes |

---

## Security

| Aspect | `consent-app-bff` | `consent-app-bff-v1` |
|--------|------------------|---------------------|
| Consent submission auth | None (cookie forwarding is the implicit trust) | HS256 JWT consent token validated on every submission |
| CORS | Not configured | Explicit `corsAllowedOrigin` allowlist |
| Session binding | IS session cookies forwarded to IS | JWT `sdkc` claim must match submitted `sessionDataKeyConsent` |
| Token injection | Via `iam-service-extensions`: SMART scope rewriting + `patient`/`encounter` claims | `consent_id` injected as access token claim via Asgardeo pre-issue action |

---

## Configuration

### consent-app-bff (`Config.toml`)

| Key | Description |
|-----|-------------|
| `hostname` / `port` | Bind address (default `localhost:9091`) |
| `consentContextApiBaseUrl` | WSO2 IS base URL |
| `consentContextApiPath` | OauthConsentKey API path |
| `consentContextApiUsername` / `Password` | IS admin credentials |
| `consentContextApiTrustStorePath` / `Password` | Client truststore for HTTPS to IS |
| `consentAuthorizeRedirectUrl` | IS OAuth2 authorize endpoint |
| `consentStoreDbUrl` / `User` / `Password` | H2 JDBC connection |
| `uiDistPath` | Path to Vite build output (served as static files) |

### consent-app-bff-v1 (`Config.toml`)

| Key | Description |
|-----|-------------|
| `tenantDomain` | Asgardeo tenant slug |
| `asgardeoTokenEndpoint` | OAuth2 token URL (defaults to standard Asgardeo URL) |
| `corsAllowedOrigin` | Exact UI origin for CORS |
| `clientId` / `clientSecret` | OAuth2 client credentials + JWT signing secret |
| `orgId` / `tppClientId` / `consentType` | OpenFGC identifiers |
| `openfgcBaseUrl` | OpenFGC consent service base URL |
| `singleConsentPerUser` | Reuse existing consent for same user+app (default `false`) |
| `showConsentElements` | Show individual elements in UI vs purpose-only (default `true`) |
| `[[consentPurpose]]` | Array of purpose configs (name, description, mandatory, elements) |

---

## Key Capabilities Unique to Each

| Capability | `consent-app-bff` | `consent-app-bff-v1` |
|-----------|:-----------------:|:--------------------:|
| Serves UI static assets | Yes | No |
| SCIM patient/user lookup | Yes | No |
| Practitioner patient picker support | Yes | No |
| SMART scope extraction from IS response | Yes | No |
| `OH_launch/` scope injection from `spQueryParams` | Yes | No |
| H2 scope persistence | Yes | No |
| Purpose + element consent model | No | Yes |
| OpenFGC consent storage | No | Yes |
| JWT consent token (CSRF protection) | No | Yes |
| Previously consented state restoration | No | Yes |
| `singleConsentPerUser` mode | No | Yes |
| `showConsentElements` toggle | No | Yes |
| Asgardeo pre-issue-access-token hook | No (uses iam-service-extensions for WSO2 IS) | Yes (built-in) |
| `consent_id` claim injection into access token | No | Yes |
| SMART scope rewriting + `patient`/`encounter` claims | Via iam-service-extensions | No |
| CORS configuration | No | Yes |
| Automated tests | No | Yes |

---

## Token Action: Pre-Issue Access Token

Each BFF stack has a corresponding pre-issue-access-token action that intercepts the token issuance flow to inject custom claims or rewrite scopes.

### Architecture comparison

```
WSO2 IS flow (consent-app-bff stack):
  ┌─────────────────────┐       ┌──────────────────────────┐
  │   consent-app-bff   │       │   iam-service-extensions  │
  │     port 9091       │◄──────│       port 9090           │
  │                     │       │                           │
  │ GET /approved-scopes│       │ POST /pre-issue-access-   │
  │  (H2 DB lookup)     │       │       token               │
  └─────────────────────┘       │                           │
                                │ Registered as WSO2 IS     │
                                │ pre-issue-access-token    │
                                │ action                    │
                                └──────────────────────────┘

Asgardeo flow (consent-app-bff-v1 stack):
  ┌──────────────────────────────────────────────────────────┐
  │                   consent-app-bff-v1                     │
  │                      port 9095                           │
  │                                                          │
  │  POST /v1/submit-consent  (stores in OpenFGC)            │
  │  POST /v1/bind-consent-to-token  ◄── Asgardeo action     │
  │         (injects consent_id claim)                       │
  └──────────────────────────────────────────────────────────┘
```

### iam-service-extensions vs consent-app-bff-v1's bind-consent-to-token

| Aspect | `iam-service-extensions` | `consent-app-bff-v1` bind-consent-to-token |
|--------|--------------------------|---------------------------------------------|
| Deployment | Separate Ballerina service (port 9090) | Built into consent-app-bff-v1 (port 9095) |
| Identity server | WSO2 IS (on-prem) | Asgardeo (cloud) |
| Registered as | WSO2 IS pre-issue-access-token action | Asgardeo pre-issue-access-token action |
| Scope source | Calls `consent-app-bff` `GET /approved-scopes` | Calls OpenFGC `GET /consents/attributes` |
| Scope rewriting | Yes — validates, filters, and rewrites SMART scopes | No scope rewriting |
| Claims injected | `patient`, `encounter` (from EHR context or SCIM) | `consent_id` (OpenFGC consent record ID) |
| SMART scope enforcement | Full: validates regex, grant-type restrictions, cruds expansion | None |
| EHR launch context resolution | Yes (`ehrContextResolveUrl`) | No |
| Patient ID resolution | Multi-step: `OH_patient/` scope → EHR context → SCIM user profile | No |
| Internal scopes consumed | `OH_launch/<id>`, `OH_patient/<id>` (stripped from final token) | None |

### iam-service-extensions — What it does

Single endpoint: `POST /pre-issue-access-token` (registered as a WSO2 IS action)

Processing sequence per token request:

1. **Fetch approved scopes** — Extracts `sessionDataKeyConsent` from the event session data; calls `consent-app-bff` `GET /approved-scopes` to get the user-approved scope list
2. **Validate SMART scopes** — Filters requested scopes against approved list and validates against regex `^(patient|user|system)/(\*|[A-Za-z]*)\.(cruds|c?r?u?d?s?)$`
3. **Grant-type enforcement** — `system/*` scopes only allowed with `client_credentials`; `patient`/`user` scopes blocked for `client_credentials`
4. **Process internal scopes** — Extracts `OH_patient/<id>` and `OH_launch/<id>` from approved scopes (these are consumed, not written to token)
5. **EHR launch context** — If `OH_launch/<id>` present, calls `ehrContextResolveUrl` to resolve patientId, encounterId, and validates redirect_uri against launch aud
6. **Patient ID resolution** (priority order): `OH_patient/` scope → EHR launch context `patientId` → SCIM user profile (checks patient group membership + `fhirUser` attribute)
7. **Encounter ID resolution** — From EHR launch context if available
8. **Inject claims** — Adds `patient` and `encounter` as access token claims via patch `add` operations
9. **Rewrite scopes** — Generates `remove` operations for old scopes and `add` operations for validated/expanded scopes

### iam-service-extensions configuration

| Key | Default | Description |
|-----|---------|-------------|
| `hostname` / `port` | `localhost` / `9090` | Listener bind address |
| `approvedScopesApiBaseUrl` | `http://localhost:9091/approved-scopes` | consent-app-bff endpoint for scope lookup (integration link) |
| `ehrContextResolveUrl` | `""` (disabled) | EHR launch context resolution endpoint |
| `scimApiBaseUrl` | `""` | SCIM2 API base URL for patient ID resolution |
| `scimApiPath` | `/scim2/Users` | SCIM2 users path |
| `scimApiUsername` / `scimApiPassword` | `""` | SCIM2 basic auth credentials |
| `scimApiTrustStorePath` / `scimApiTrustStorePassword` | `""` | Truststore for HTTPS SCIM connections |
| `scimPatientGroupName` | `"patient"` | SCIM group name identifying patient users |
| `fhirUserAttributeName` | `"fhirUser"` | SCIM custom schema attribute holding FHIR user reference |
| `alwaysAllowedScopes` | `["openid"]` | Scopes that bypass consent checks |
| `consentContextApiBaseUrl` / `Organization` / `Token` | `""` | Configured but unused in current implementation |
| `consentAuthorizeRedirectUrl` | `""` | Configured but unused in current implementation |
