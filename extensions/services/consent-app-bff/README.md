# consent-app-bff

Ballerina Backend-for-Frontend for the consent application. Runs on port **9092**, exposes a REST API, and works with both WSO2 IS and Asgardeo.

## Architecture

```
consent-app (UI)
      │
      ▼
consent-app-bff  :9092
      │
      ├── IDP (WSO2 IS / Asgardeo)  — OauthConsentKey + SCIM
      └── WSO2 IS consent management API (v2) — consent store
```

### Files

| File | Purpose |
|------|---------|
| `config.bal` | All configurables |
| `connections.bal` | IDP HTTP client (also used for the IS consent API), token management, purpose cache |
| `types.bal` | All record types |
| `service.bal` | HTTP listener, service endpoints |
| `tests/` | Mock WSO2 IS listener (token + consent API), IDP mocked at function level, service tests |

## API

### `GET /get-consent-data?sessionDataKeyConsent=&spId=`

Resolves the consent flow and returns all data the UI needs in one call.

**Responses:**

```json
// flow = "scope"
{
  "flow": "scope",
  "sessionDataKeyConsent": "...",
  "spId": "...",
  "user": { "id": "...", "displayName": "...", "email": "..." },
  "isPractitioner": false,
  "patients": [],
  "scopes": ["patient/Observation.read"],
  "hiddenScopes": ["OH_launch/abc"],
  "mandatoryClaims": "0_email",
  "previouslyApprovedScopes": [],
  "consentToken": "<jwt>"
}

// flow = "purpose"
{
  "flow": "purpose",
  "sessionDataKeyConsent": "...",
  "spId": "...",
  "appName": "MyApp",
  "user": { ... },
  "purposes": [{ "purposeName": "All Health Data Access", "mandatory": false, "elements": ["Patient", "Observation"] }],
  "scopes": ["openid", "fhirUser"],
  "mandatoryClaims": "",
  "consentToken": "<jwt>"
}

// flow = "redirect" (consentFlow = "auto", spId not in either list)
{
  "flow": "redirect",
  "redirectUrl": "https://<idp>/oauth2_consent.do?sessionDataKeyConsent=...&spId=..."
}
```

### `POST /submit-consent`

Validates the JWT consent token and stores the decision in WSO2 IS as a consent record (`POST /consents`). With `singleConsentPerUser`, previous active consents of the user are revoked after the new one is created. The UI form-POSTs directly to the IDP after this call succeeds.

```json
// scope flow
{
  "consentToken": "<jwt>",
  "sessionDataKeyConsent": "...",
  "spId": "...",
  "approved": true,
  "approvedScopes": ["patient/Observation.read"],
  "hiddenScopes": ["OH_launch/abc"]
}

// purpose flow
{
  "consentToken": "<jwt>",
  "sessionDataKeyConsent": "...",
  "spId": "...",
  "approved": true,
  "consentedPurposes": [{ "purposeName": "All Health Data Access", "consentedElements": ["Patient"] }],
  "existingConsentId": "<id-if-updating>"
}
```

## Consent flows

| `consentFlow` | Behaviour |
|---------------|-----------|
| `scope` | All apps use SMART scope selection |
| `purpose` | All apps use purpose + element selection |
| `auto` | Resolved by `spId`: `scopeConsentedApps` → scope, `purposeConsentedApps` → purpose, otherwise redirect to `defaultIdpConsentPage` |

When `consentFlow = "auto"`, flow is determined **before** any IDP call, so the IDP session is never corrupted by a server-side OauthConsentKey call for the redirect path.

When `scopes = []` (no visible scopes in the scope flow), the UI auto-approves silently without calling submit-consent.

## Setup

```bash
cp Config.toml.example Config.toml
# Edit Config.toml with your values
bal run
```

### Required config

| Key | Description |
|-----|-------------|
| `corsAllowedOrigin` | UI origin, e.g. `http://localhost:5175` |
| `idpBaseUrl` | WSO2 IS (`https://localhost:9443`) or Asgardeo (`https://api.asgardeo.io/t/<tenant>`) |
| `clientId` / `clientSecret` | OAuth2 client credentials; `clientSecret` is also the HS256 JWT signing secret |
| `serviceId` | Service ID recorded on consents (default `smart-on-fhir`) |
| `scopeConsent.purposeName` / `purposeConsent` | Consent purposes that must already exist in WSO2 IS 7.3.0+ |

The consent management API is called on `idpBaseUrl` (path `consentApiBasePath`, default `/api/identity/consent-mgt/v2.0`) with a token of the same management application (`clientId`/`clientSecret`). Authorize that application for the Consent Management API with `internal_consent_mgt_purpose_view`, `internal_consent_mgt_element_view`, `internal_consent_mgt_consent_create`, `internal_consent_mgt_consent_view` and `internal_consent_mgt_consent_update`.

### Consent record layout in WSO2 IS

| Field | Value |
|-------|-------|
| `subjectId` / `serviceId` | Logged-in username / `serviceId` config |
| `purposes[].elements` | Scope flow: the elements of `scopeConsent.purposeName`. Purpose flow: only the elements the user approved |
| `expiryTime` | Epoch milliseconds from the chosen validity (`never` → no expiry) |
| `properties` | `sessionDataKeyConsent`, `spId`, `application`, `clientId` (OAuth `client_id`, used by `iam-service-extensions` to find the consent in the token flow); scope flow also `approvedScopes` (space-separated) and `consentExpiryOption` |

See `Config.toml.example` for the full template including `consentFlow`, purpose definitions, and `auto` mode settings.

## Tests

```bash
bal test
```

Tests use in-process mock WSO2 IS listener (token endpoint and consent API) defined in `tests/`.
