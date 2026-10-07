# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this service does

Ballerina Backend-for-Frontend (BFF) for the consent application. Runs on port **9092**. Sits between the consent UI and two backends: the identity provider (WSO2 IS or Asgardeo) and the WSO2 IS consent management API (consent store).

```
consent-app (UI)
      │
      ▼
consent-app-bff  :9092
      ├── IDP (WSO2 IS / Asgardeo)  — OauthConsentKey API + SCIM
      └── WSO2 IS consent management API v2 — consent store (purposes, elements, consents)
```

## Commands

```bash
# Run (requires Config.toml)
cp Config.toml.example Config.toml  # then fill in values
bal run

# Test
bal test

# Build JAR
bal build
```

## Architecture

### Consent flows

The `consentFlow` configurable controls which UI is shown:

| Value | Behaviour |
|-------|-----------|
| `scope` | All apps use SMART scope selection |
| `purpose` | All apps use purpose + element selection |
| `auto` | Flow resolved per `spId` using `scopeConsentedApps` / `purposeConsentedApps`; falls back to redirect |

**Critical invariant**: when `consentFlow = "auto"`, the flow is resolved from config *before* any IDP call. This prevents consuming the IDP consent session for apps that should just redirect.

### Request lifecycle — `GET /get-consent-data`

1. Resolve effective flow from `spId` (return redirect immediately if needed)
2. Call IDP `OauthConsentKey` API to get `loggedInUser`, `application`, `scope`, `spQueryParams`
3. Fan out in parallel: SCIM user lookup + optional existing-consent lookup (if `singleConsentPerUser = true`)
4. For scope flow: parse/partition scopes into visible vs hidden; resolve EHR launch context if `launch` param present; fetch SCIM patients if practitioner and no patient from launch
5. Issue HS256 JWT consent token (signed with `clientSecret`, 10-minute expiry, binds `loggedInUser` + `sessionDataKeyConsent`)
6. Return `ScopeConsentData | PurposeConsentData | RedirectConsentData`

### Request lifecycle — `POST /submit-consent`

1. Validate HS256 JWT consent token (same secret, checks issuer/audience/expiry + `sdkc` claim must match `sessionDataKeyConsent`)
2. If denied: return success immediately (no consent API call)
3. Store decision in WSO2 IS: `POST /consents`. There is no update call, so with `singleConsentPerUser` the user's other ACTIVE consents for `serviceId` are revoked (`POST /consents/{id}/revoke`) after the new one is created (the client-supplied `existingConsentId` is not trusted). Scope flow keeps approved scopes in consent `properties.approvedScopes`; purpose flow binds only approved elements

### Scope partitioning rules (scope flow)

- `OH_*` prefixed, `launch*` prefixed, `fhirUser` → hidden (passed through but not shown to user)
- `system/*` → silently dropped
- In `alwaysAllowedScopes` (default `["openid"]`) → silently approved, not shown
- FHIR compound scopes like `patient/Observation.cru` → split into individual operation scopes

### Purpose cache

At startup, `fetchAndCachePurposes()` fetches all configured purpose names from WSO2 IS (`GET /purposes?filter=name eq <n>` then `GET /purposes/{id}`) incl. element ids and stores them in the module-level `purposeCache` (`isolated map<CachedPurpose>`). The service **fails to start** if any configured purpose is not found. The cache is read-only after init; lock blocks are used for concurrent access.

### EHR launch context

If `ehrContextResolveUrl` is configured and a `launch` query param is present in `spQueryParams`, the BFF calls `{ehrContextResolveUrl}/launch/{launchId}` to resolve patient/encounter. A resolved patient is added to `hiddenScopes` as `OH_patient/{patientId}` — skipping the patient picker in the UI.

### IDP token management

`getIdpToken()` creates a fresh `ClientOAuth2Provider` per call (intentionally — avoids eager token fetch at module init which would fail in tests). `callIdp()` retries once on 401 with a fresh token.

## Files

| File | Purpose |
|------|---------|
| `config.bal` | All `configurable` declarations |
| `connections.bal` | IDP HTTP client, `callConsentApi`, `getIdpToken`, `fetchAndCachePurposes`, `resolveLaunchContext` |
| `types.bal` | All record types (config, request/response, IS consent payloads, internal) |
| `service.bal` | HTTP listener, both resource functions, helper functions |
| `tests/service_test.bal` | Service tests with function-level mocks for IDP calls |
| `tests/mock_is_consent_service.bal` | In-process mock WSO2 IS listener on port 9196 (token endpoint + consent API) |
| `tests/Config.toml` | Test-specific configurables (IDP calls mocked at function level, not HTTP level) |

## Testing approach

- IDP functions (`callIdp`, `getScimUser`, `getScimPatients`) are mocked at the Ballerina function level using `@test:Mock`
- WSO2 IS (token + consent API) is mocked with a real in-process HTTP listener on port 9196 (`mock_is_consent_service.bal`); tests set `fetchPurposesOnStartup = false` because the mock starts after module init
- `mockHasExistingConsent` is a module-level boolean toggled in tests to simulate existing consent state
- The `clientSecret` in `tests/Config.toml` must be ≥ 32 characters (HS256 requirement)
- Tests call the actual BFF listener on port 9092 via an HTTP client

## Key config notes

- `clientSecret` doubles as the HS256 JWT signing secret — must be ≥ 32 characters
- `idpTokenEndpoint` defaults to `{idpBaseUrl}/oauth2/token` if left empty
- SSL to IDP: set `consentContextApiTrustStorePath` + `consentContextApiTrustStorePassword`
- `scopeConsent.purposeName` must exist in WSO2 IS even in purpose flow (it wraps the scope consent)
- `fhirUserAttributeName` defaults to `fhirUser`; maps to `urn:scim:schemas:extension:custom:User.{fhirUserAttributeName}` in SCIM
