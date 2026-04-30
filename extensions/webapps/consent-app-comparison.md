# consent-app vs consent-app-v1 Comparison

## Overview

| Aspect | `consent-app` | `consent-app-v1` |
|--------|--------------|-----------------|
| Language | JavaScript (JSX) | TypeScript (TSX) |
| Router | None (React state) | React Router v6 |
| Consent model | SMART scopes (flat list) | Purposes + elements (hierarchical) |
| Practitioner flow | Yes (`PatientPickerPage`) | No |
| Oxygen UI import | `@oxygen-ui/react` | `@wso2/oxygen-ui` |
| Theme setup | `extendTheme` | `OxygenUIThemeProvider + AcrylicOrangeTheme` |
| BFF protocol | Vite proxy `/api/*` to `localhost:9091` | Direct `VITE_CONSENT_BFF_URL` env var |
| Auth redirect | POST form to `consentAuthorizeRedirectUrl` prop (configurable, default `https://localhost:9443/oauth2/authorize`) | POST form to `VITE_ASGARDEO_AUTHORIZE_URL` |
| Logo | WSO2 logo (CDN URL) | Asgardeo logo (local SVG asset) |
| StrictMode | Yes | Intentionally omitted (Asgardeo API invalidates `sessionDataKeyConsent` on double-invoke) |
| Config | No env vars needed | `.env` with 2 required vars |

---

## Consent Model

### consent-app — SMART Scopes (flat)
- Scopes arrive as a flat string array from `/api/consent-context`
- `OH_launch/*` scopes are hidden from the user but always included on approval
- SMART scopes (`patient/`, `user/`, `system/`) validated against regex: `(patient|user|system)/{ResourceType}.{cruds}`
- User can toggle individual scopes via checkboxes; bulk select/clear all available
- **`mandatoryClaims`** prop (new): parsed from `"0_Telephone,2_Email"` format; shown as non-toggleable "Required User Attributes" section above scopes; submitted as `consent_<id>: "approved"` fields
- **Before approve**: calls `POST /store-scopes` (fire-and-forget JSON) with `{sessionDataKeyConsent, scopes}`
- **On approve/deny**: form POSTs directly to `consentAuthorizeRedirectUrl` (configurable, defaults to `https://localhost:9443/oauth2/authorize`)
- Scopes submitted as single space-joined `scope` field (not multiple `scope` fields)
- Form field names (lowercase): `sessionDataKeyConsent`, `consent`, `user_claims_consent`

### consent-app-v1 — Purposes + Elements (hierarchical)
- Purposes fetched from `{BFF_URL}/v1/get-consent-data`
- Each purpose has: `purposeName`, `mandatory`, `purposeDescription`, `elements[]`
- Mandatory purposes: always checked, user cannot uncheck
- Optional purposes: user toggles at purpose level or individual element level
- Indeterminate checkbox state when some elements are checked
- Supports previously consented state (`initialConsentedPurposeNames`, `initialConsentedElements`)
- Approved purposes POSTed as JSON to `{BFF_URL}/v1/submit-consent`, then redirects to Asgardeo

---

## User Flows

### consent-app
1. URL: `/consent-app/?sessionDataKeyConsent=<token>&spId=<id>`
2. Fetches consent context from `GET /api/consent-context`
3. Fetches user info from `GET /api/me?userId=`
4. **If user is a Practitioner** (`fhirUser` contains `"Practitioner"`):
   - Shows `PatientPickerPage` → user selects a patient
5. **Otherwise**: Goes directly to `ConsentPage`
6. If approving: `POST /store-scopes` (fire-and-forget) → form POST to `consentAuthorizeRedirectUrl`
7. If denying: form POST directly to `consentAuthorizeRedirectUrl`

### consent-app-v1
1. URL: `/consent-page?sessionDataKeyConsent=<token>&spId=<id>`
2. Fetches consent data from `GET {BFF_URL}/v1/get-consent-data`
3. Shows `ConsentPage` with purposes
4. Allow → POST to `{BFF_URL}/v1/submit-consent` → form POST to Asgardeo authorize URL
5. Deny → form POST directly to Asgardeo authorize URL

---

## Backend API Differences

### consent-app (`/api/*` proxied to localhost:9091)

| Endpoint | Purpose |
|----------|---------|
| `GET /api/consent-context` | Returns `sessionDataKeyConsent`, `spId`, `user`, `scopes[]` |
| `GET /api/me?userId=` | Returns SCIM user; `fhirUser` field determines practitioner |
| `GET /api/patients` | Returns SCIM `Resources[]` of patients (practitioner flow only) |
| `POST /store-scopes` | Fire-and-forget; persists `{sessionDataKeyConsent, scopes}` before authorize redirect |

### consent-app-v1 (`VITE_CONSENT_BFF_URL`)

| Endpoint | Purpose |
|----------|---------|
| `GET /v1/get-consent-data` | Returns `appName`, `consentToken`, `purposes[]`, `existingConsentId`, previously consented data |
| `POST /v1/submit-consent` | Accepts consent payload; triggers Asgardeo redirect on success |

---

## UI / Component Differences

| Area | `consent-app` | `consent-app-v1` |
|------|--------------|-----------------|
| Consent card styling | Custom CSS (`ConsentPage.css`) | Oxygen UI `Paper`, `Box`, `Typography` |
| Patient picker | Full page with Oxygen UI (`AppBar`, `Card`, `Select`, `Chip`, `Avatar`) | Not present |
| Error handling | Inline error div / `ErrorBanner` component | `Snackbar` + `Alert` for submit errors |
| Loading state | Returns `null` while loading | `CircularProgress` shown inline |
| Submit feedback | No spinner on buttons | `CircularProgress` inside Allow/Deny buttons while submitting |
| App name display | Not shown | Shown prominently: `"{appName} wants to access your account"` |

---

## Key Missing Features (consent-app vs consent-app-v1)

| Feature | consent-app | consent-app-v1 |
|---------|------------|----------------|
| TypeScript | No | Yes |
| Purpose-level consent (not just scopes) | No | Yes |
| Mandatory purposes | No | Yes |
| Per-element consent | No | Yes |
| Previously consented state restoration | No | Yes |
| Submit loading state on buttons | No | Yes |
| App name display | No | Yes |
| Practitioner patient picker | **Yes** | No |
| SMART scope validation | **Yes** | No |
| Mandatory claims display | **Yes** (`mandatoryClaims` prop) | No |
| Pre-authorize scope persistence (`POST /store-scopes`) | **Yes** | No |
| Configurable authorize redirect URL | **Yes** | No (env var only) |
