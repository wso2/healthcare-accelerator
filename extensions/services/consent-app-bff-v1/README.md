# consent-app-bff

Ballerina BFF (Backend-for-Frontend) that proxies between the [consent-app](../../apps/consent-app/) UI and the Asgardeo identity server. Runs on port **9095**.

## Prerequisites

- Ballerina Swan Lake `2201.12.x`

## Setup

```bash
cp Config.toml.example Config.toml   # fill in values (see Configuration below)
bal run
```

## Configuration

`Config.toml` is gitignored. Copy `Config.toml.example` and set:

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `tenantDomain` | yes | — | Asgardeo tenant slug (e.g. `anjucha`) |
| `asgardeoTokenEndpoint` | no | `https://api.asgardeo.io/t/<tenantDomain>/oauth2/token` | Asgardeo OAuth2 token endpoint |
| `corsAllowedOrigin` | yes | — | Exact origin of the consent-app UI (e.g. `http://localhost:5175`) |
| `clientId` | yes | — | OAuth2 client ID for the client credentials grant |
| `clientSecret` | yes | — | OAuth2 client secret (also used to sign/verify the consent token JWT) |
| `orgId` | yes | — | OpenFGC organisation ID |
| `tppClientId` | yes | — | TPP client ID sent to OpenFGC when creating a consent |
| `consentType` | yes | — | Consent type string used in OpenFGC (e.g. `patient-access`) |
| `openfgcBaseUrl` | yes | — | Base URL of the OpenFGC consent service |
| `singleConsentPerUser` | no | `false` | When `true`, reuses and updates an existing active consent instead of creating a new one |
| `showConsentElements` | no | `true` | When `false`, hides individual elements from the UI — user approves each purpose as a whole |
| `[[consentPurpose]]` | no | single "All Health Data Access" purpose | Array of purpose objects (see below) |

Each `[[consentPurpose]]` block supports:

| Field | Required | Description |
|-------|----------|-------------|
| `name` | yes | Unique purpose name; used as the identifier in OpenFGC |
| `description` | no | Human-readable description shown in the consent UI |
| `mandatory` | no (default `false`) | Whether the user must approve this purpose to proceed |
| `elements` | yes | List of OpenFGC consent element names belonging to this purpose |

Example with two purposes:

```toml
[[consentPurpose]]
name = "All Health Data Access"
description = "Access your health data related to USCDI."
mandatory = false
elements = ["Patient", "Observation", "Condition"]

[[consentPurpose]]
name = "Appointment Scheduling"
description = "Allow scheduling appointments on your behalf."
mandatory = false
elements = ["Appointment", "Schedule", "Slot"]
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/v1/get-consent-data` | Returns app name, purposes, and a signed consent token for the given `sessionDataKeyConsent` |
| `POST` | `/v1/submit-consent` | Records the user's approve/deny decision in OpenFGC |
| `POST` | `/v1/bind-consent-to-token` | Asgardeo pre-issue-access-token action — injects `consent_id` claim into the access token |

## Architecture

### Consent flow

1. **`GET /v1/get-consent-data`** — the consent-app loads the page with a `sessionDataKeyConsent` query param. The BFF calls Asgardeo to resolve the application name and logged-in user, optionally looks up an existing consent in OpenFGC (`singleConsentPerUser`), and returns the purpose list plus a short-lived **HS256 consent token** (signed with `clientSecret`, valid 10 min) that binds the user identity and session key.
2. **`POST /v1/submit-consent`** — the UI sends back the user's decision along with the consent token. The BFF validates the token (issuer, audience, signature, session-key match), then creates or updates a consent record in OpenFGC with the appropriate element approvals.
3. **`POST /v1/bind-consent-to-token`** — registered as an Asgardeo **pre-issue-access-token** action. After the user approves consent, in the token flow, before issuing the token, Asgardeo calls this endpoint (should be configured) with the session data. The BFF looks up the matching consent ID in OpenFGC by the stored `sessionDataKeyConsent` attribute and injects it as a `consent_id` claim into the access token.

### Consent token

`get-consent-data` issues a JWT signed with `clientSecret` (HS256) embedding `loggedInUser`, `application`, and `sessionDataKeyConsent`. `submit-consent` verifies this token so it can trust the user identity without re-calling Asgardeo.

### Asgardeo client

`callAsgardeo()` manually sets the `Authorization` header and handles a `401` retry by fetching a fresh token. This avoids relying on Ballerina's built-in OAuth2 interceptor, which can serve a cached (expired) token.

## Testing

```bash
bal test
```

Tests mock `callAsgardeo()` and run an in-process mock OpenFGC HTTP service (ports 9096/9097). Test-time configuration is in `tests/Config.toml`.
