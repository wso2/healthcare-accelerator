# consent-app-v2 + consent-app-bff-v2 — Implementation Plan

## Decisions made

| Decision | Choice |
|----------|--------|
| Language (webapp) | TypeScript |
| IDP targeting | IDP-agnostic — OAuth2 client credentials + Bearer token (works for both WSO2 IS and Asgardeo) |
| Flow selection | Single server-wide config: `consentFlow = "scope" \| "purpose"` |
| Consent storage | OpenFGC only (no H2 DB) |
| Scope→OpenFGC mapping | Approved scopes in `authorizations[].resources.scopes`; fixed scope purpose wraps them |
| Token action | No bind-consent-to-token in BFF — iam-service-extensions handles all pre-issue token actions |
| Static file serving | BFF is pure REST API; UI deployed separately |
| JWT security | Yes — BFF issues HS256 consent token on get-consent-data, verified on submit-consent |
| Practitioner picker | Yes — in scope-based flow (like consent-app) |
| singleConsentPerUser | Yes — both flows |
| showConsentElements | Yes — purpose flow |
| Previously consented state | Yes — both flows |
| Tests | Yes — `bal test` with mocked IDP + mock OpenFGC |

---

## New directories

```
extensions/webapps/consent-app-v2/       ← TypeScript React webapp
extensions/services/consent-app-bff-v2/  ← Ballerina BFF
```

---

## consent-app-bff-v2

### Port: 9092 (default) | Service path: `/v2`

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v2/get-consent-data?sessionDataKeyConsent=&spId=` | **Aggregated**: BFF makes all upstream calls internally (IDP, SCIM user, SCIM patients, OpenFGC) and returns everything the UI needs in one response |
| POST | `/v2/submit-consent` | Validates JWT; stores consent in OpenFGC; POSTs to IDP authorize endpoint; returns redirect URL |

> **Architecture note:** The UI makes exactly one call on load (`GET /v2/get-consent-data`) and one on submit. There are no separate `/me`, `/patients`, or `/approved-scopes` endpoints. The BFF aggregates all upstream calls server-side in parallel before responding.

> **iam-service-extensions integration:** iam-service-extensions queries OpenFGC **directly** (no BFF hop). It looks up `GET {openfgcBaseUrl}/consents/attributes?key=sessionDataKeyConsent&value=<key>` → resolves consentId → `GET {openfgcBaseUrl}/consents/{consentId}` → extracts `authorizations[0].resources.scopes`. This requires adding OpenFGC config (`openfgcBaseUrl`, `orgId`, `tppClientId`) to iam-service-extensions and removing `approvedScopesApiBaseUrl`.

### IDP integration (IDP-agnostic)
- OAuth2 client credentials → Bearer token for all IDP calls (same for WSO2 IS and Asgardeo)
- 401-retry: on HTTP 401, fetch a fresh token and retry once
- Configurable `idpBaseUrl` (e.g. `https://api.asgardeo.io/t/mytenant` or `https://localhost:9443`)
- OauthConsentKey API: `GET {idpBaseUrl}/api/identity/auth/v1.1/data/OauthConsentKey/{sessionDataKeyConsent}`
- Authorize POST: `POST {idpAuthorizeUrl}` with `sessionDataKeyConsent`, `consent=approve|deny`, `user_claims_consent=true`, `consent_<id>=approved` per mandatory claim, `scope=<space-joined>`, cookies forwarded → returns redirect URL to UI
- Note: v2 BFF handles OpenFGC storage + IDP authorize POST in `submit-consent` — no separate `/store-scopes` endpoint needed

### OpenFGC data models

**Scope flow:**
```json
{
  "type": "<consentType>",
  "purposes": [{"name": "<scopeConsentPurposeName>", "elements": [{"name": "<scopeConsentElementName>", "isUserApproved": true}]}],
  "authorizations": [{"userId": "<user>", "type": "scope-authorization", "status": "APPROVED",
    "resources": {"spId": "...", "application": "...", "scopes": ["patient/Obs.read", "OH_launch/abc"]}}],
  "attributes": {"sessionDataKeyConsent": "<key>"}
}
```

**Purpose flow:**
```json
{
  "type": "<consentType>",
  "purposes": [{"name": "All Health Data Access", "elements": [{"name": "Patient", "isUserApproved": true}, {"name": "Observation", "isUserApproved": false}]}],
  "authorizations": [{"userId": "<user>", "type": "authorisation", "status": "APPROVED",
    "resources": {"spId": "...", "application": "..."}}],
  "attributes": {"sessionDataKeyConsent": "<key>"}
}
```

### iam-service-extensions integration
- iam-service-extensions queries OpenFGC **directly** — no BFF hop
- Add to iam-service-extensions config: `openfgcBaseUrl`, `orgId`, `tppClientId`
- Remove from iam-service-extensions config: `approvedScopesApiBaseUrl`
- Lookup sequence: `GET {openfgcBaseUrl}/consents/attributes?key=sessionDataKeyConsent&value=<key>` → `consentIds[0]` → `GET {openfgcBaseUrl}/consents/{consentId}` → `authorizations[0].resources.scopes`

### Files

| File | Purpose |
|------|---------|
| `Ballerina.toml` | Package: org=ballerinax, name=consent_app_bff_v2, dist=2201.12.10 |
| `config.bal` | All `configurable` variables |
| `connections.bal` | IDP OAuth2 client, `getIdpToken()`, OpenFGC HTTP client |
| `service.bal` | HTTP listener + `/v2` service, `callIdp()` with 401-retry |
| `types.bal` | All record types (request/response, OpenFGC, IDP, consent types) |
| `Config.toml.example` | Template for deployment |
| `tests/service_test.bal` | Tests for both flow types |
| `tests/mock_idp_service.bal` | In-process mock IDP listener |
| `tests/mock_openfgc_service.bal` | In-process mock OpenFGC listener |
| `tests/Config.toml` | Test-time config |

### Key configurables (Config.toml)

```toml
# Server
hostname = "localhost"
port = 9092
corsAllowedOrigin = "http://localhost:5176"

# IDP (works for both WSO2 IS and Asgardeo)
idpBaseUrl = "https://api.asgardeo.io/t/mytenant"
idpTokenEndpoint = ""          # defaults to {idpBaseUrl}/oauth2/token
idpAuthorizeUrl = ""           # defaults to {idpBaseUrl}/oauth2/authorize
clientId = "..."
clientSecret = "..."           # also used as JWT signing secret

# Consent flow: "scope" or "purpose"
consentFlow = "scope"

# OpenFGC
openfgcBaseUrl = "http://localhost:8080"
orgId = "..."
tppClientId = "..."
consentType = "smart-consent"

# Scope flow specific
scopeConsentPurposeName = "SMART Scope Authorization"
scopeConsentElementName = "scope-access"
alwaysAllowedScopes = ["openid"]

# Both flows
singleConsentPerUser = false

# Purpose flow specific
showConsentElements = true
[[consentPurpose]]
name = "All Health Data Access"
description = "Access your health data related to USCDI."
mandatory = false
elements = ["Patient", "Observation", "Condition", ...]
```

---

## consent-app-v2

### Tech stack
- React 18, TypeScript, Vite, `@oxygen-ui/react` v2
- React Router v6
- `StrictMode` omitted (IDP OauthConsentKey API invalidates session on double-invoke)

### get-consent-data response shapes

**Scope flow:**
```json
{
  "flow": "scope",
  "sessionDataKeyConsent": "...",
  "spId": "...",
  "user": { "id": "...", "displayName": "John Smith", "email": "john@example.com" },
  "isPractitioner": true,
  "patients": [{ "id": "...", "name": "Jane Doe", "mrn": "MRN-001", "fhirUser": "Patient/123" }],
  "scopes": ["patient/Observation.read", "patient/Patient.read"],
  "hiddenScopes": ["OH_launch/abc"],
  "mandatoryClaims": "0_Telephone,2_Email",
  "previouslyApprovedScopes": ["patient/Observation.read"],
  "consentToken": "<jwt>"
}
```
- `patients` only present when `isPractitioner: true`
- `previouslyApprovedScopes` only when `singleConsentPerUser=true` and prior consent found
- BFF makes these upstream calls in parallel: OauthConsentKey → SCIM user → (if practitioner) SCIM patient search + (if singleConsentPerUser) OpenFGC lookup

**Purpose flow:**
```json
{
  "flow": "purpose",
  "sessionDataKeyConsent": "...",
  "spId": "...",
  "appName": "MyHealthApp",
  "user": { "id": "...", "displayName": "John Smith", "email": "john@example.com" },
  "purposes": [{ "purposeName": "All Health Data Access", "mandatory": false, "purposeDescription": "...", "elements": ["Patient", "Observation"] }],
  "existingConsentId": "...",
  "previouslyConsentedPurposeNames": ["All Health Data Access"],
  "previouslyConsentedElements": { "All Health Data Access": ["Patient"] },
  "consentToken": "<jwt>"
}
```
- `existingConsentId`, `previouslyConsented*` only when `singleConsentPerUser=true` and prior consent found
- BFF makes these upstream calls in parallel: OauthConsentKey → SCIM user + (if singleConsentPerUser) OpenFGC lookup

### Files

| File | Purpose |
|------|---------|
| `src/main.tsx` | Entry — OxygenUI theme + BrowserRouter, no StrictMode |
| `src/App.tsx` | Fetches `/v2/get-consent-data`, routes to correct flow/page |
| `src/ScopeConsentPage.tsx` | SMART scope checkboxes, OH_launch hidden, select-all/clear |
| `src/PurposeConsentPage.tsx` | Purpose+element hierarchy, mandatory, indeterminate state, previously consented |
| `src/PatientPickerPage.tsx` | Practitioner patient picker (receives `patients[]` from App as props — no additional API calls) |
| `src/types.ts` | Shared TypeScript types for API responses and component props |
| `src/api.ts` | Typed fetch helpers (`getConsentData`, `submitConsent`) — only 2 functions needed |
| `src/index.css` | Global styles |

### App.tsx routing logic

```
URL: /consent?sessionDataKeyConsent=&spId=
  ↓ fetch GET /v2/get-consent-data  ← single API call, all data arrives here
  ↓ if flow === "scope":
      if isPractitioner && !selectedPatient
        → PatientPickerPage(patients=data.patients, user=data.user)
      else
        → ScopeConsentPage(scopes, hiddenScopes, user, selectedPatient)
  ↓ if flow === "purpose":
      → PurposeConsentPage(purposes, user, appName, previouslyConsented*)
```

### ScopeConsentPage
- SMART scope regex validation: `^(patient|user|system)/(\*|[A-Za-z]+)\.(cruds|c?r?u?d?s?)$`
- `OH_launch/*` hidden, always included on approve
- **`mandatoryClaims`**: displayed as non-toggleable "Required User Attributes" section; submitted as `consent_<id>: "approved"` fields in the authorize form
- Scope checkboxes + select-all/clear-all + count badge
- On Approve/Deny: POST `/v2/submit-consent` (stores in OpenFGC) → `window.location.href = response.redirectUrl`
- Error banner if submit fails

### PurposeConsentPage
- Purposes with mandatory flag (disabled, always checked)
- Element-level checkboxes with parent indeterminate state
- Pre-populates from `previouslyConsentedPurposeNames` + `previouslyConsentedElements`
- On Allow/Deny: POST `/v2/submit-consent` → redirect
- Snackbar for submit errors

### PatientPickerPage
- **No API calls** — receives `patients[]` and `user` as props from App (data already in get-consent-data response)
- Oxygen UI: AppBar, Card, Select dropdown, Avatar, Chip
- Error banner if patients list is empty
- Proceed (disabled until patient selected) + Cancel

### Vite proxy (dev only)
```ts
proxy: { '/v2': 'http://localhost:9092' }
```

---

## Implementation order

1. `consent-app-bff-v2`: Ballerina.toml → types.bal → config.bal → connections.bal → service.bal → Config.toml.example
2. `consent-app-bff-v2` tests: mock services + test cases
3. `consent-app-v2`: package.json + tsconfig + vite.config.ts → types.ts → api.ts → main.tsx → App.tsx → ScopeConsentPage → PurposeConsentPage → PatientPickerPage

## Key reference files

| Reference | Used for |
|-----------|----------|
| `extensions/services/consent-app-bff-v1/service.bal` | `callIdp()` with 401-retry pattern |
| `extensions/services/consent-app-bff-v1/config.bal` | Configurable structure, purpose config |
| `extensions/services/consent-app-bff-v1/types.bal` | OpenFGC type definitions to reuse |
| `extensions/services/consent-app-bff-v1/connections.bal` | OAuth2 client setup |
| `extensions/services/consent-app-bff/consent_service.bal` | SCIM patient search, scope extraction, cookie forwarding, authorize redirect |
| `extensions/webapps/consent-app/src/ConsentPage.jsx` | Scope UI logic |
| `extensions/webapps/consent-app/src/PatientPickerPage.jsx` | Patient picker UI |
| `extensions/webapps/consent-app-v1/src/ConsentPage.tsx` | Purpose UI logic + indeterminate state |

## Verification steps

1. `bal build` in consent-app-bff-v2/ → clean compile
2. `bal test` in consent-app-bff-v2/ → all tests pass
3. `npm run build` in consent-app-v2/ → clean TypeScript compile
4. Manual: BFF with `consentFlow=scope` → `GET /v2/get-consent-data` → verify response includes `flow`, `scopes`, `user`, `isPractitioner`, `patients` (if practitioner), `consentToken`
5. Manual: BFF with `consentFlow=purpose` → `GET /v2/get-consent-data` → verify response includes `flow`, `appName`, `purposes`, `user`, `consentToken`
6. Manual: `POST /v2/submit-consent` → verify OpenFGC consent record created with correct shape, and response contains `redirectUrl`
