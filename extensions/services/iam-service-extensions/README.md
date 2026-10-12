# iam-service-extensions

Ballerina pre-issue-access-token action service for the consent system. Runs on port **9093**. Intercepts token issuance, validates requested scopes against the user's consent record in the WSO2 IS consent management service (IS 7.3.0+), and injects patient/encounter/consent_id claims.

## Architecture

```
IDP (WSO2 IS / Asgardeo)
      │ POST /pre-issue-access-token
      ▼
iam-service-extensions  :9093
      │
      ├── IS consent management API — consent record lookup (by sessionDataKeyConsent)
      └── SCIM     — patient ID resolution fallback (optional)
      └── EHR      — launch context resolution (optional)
```

### Files

| File | Purpose |
|------|---------|
| `configurables.bal` | All configurables |
| `types.bal` | Request/response record types |
| `client.bal` | IS consent API + SCIM + EHR HTTP clients, token caching |
| `utils.bal` | Scope helpers, patient ID extraction, URI encoding |
| `service.bal` | POST `/pre-issue-access-token` handler |
| `tests/` | Mock IS consent API + SCIM, service tests |

## Processing sequence

1. Extract `sessionDataKeyConsent` from `event.session` (not sent by every IS version)
2. Look up the ACTIVE consent record in WSO2 IS
   - With a session key: `GET {isBaseUrl}/api/identity/consent-mgt/v2.0/consents?filter=properties.sessionDataKeyConsent eq <key>&state=ACTIVE`
   - Without one (user-bound grants such as `authorization_code` and `refresh_token`): `GET .../consents?userId=<event.user.id>&relation=SUBJECT&serviceId=<consentServiceId>&state=ACTIVE&filter=properties.clientId eq <event.request.clientId>`. `consent-app-bff` stores the OAuth `client_id` as the consent property `clientId`; with `singleConsentPerUser` there is one active consent per user
   - No consent found → SUCCESS with no operations (pass-through)
3. Fetch the consent (`GET /consents/{id}`) → extract the approved scopes from the `approvedScopes` consent property (space separated, written by `consent-app-bff`); consented purpose element names are also treated as approved scopes. Non-`ACTIVE` consents yield no scopes
4. Extract internal `OH_patient/<id>` and `OH_launch/<id>` scopes (consumed, not forwarded)
5. Validate each requested token scope:
   - Must be in approved scopes or `alwaysAllowedScopes`
   - Must match SMART regex: `^(patient|user|system)/(\*|[A-Za-z]*)\.(cruds|c?r?u?d?s?)$`
   - `system/*` blocked for non-`client_credentials` grants; `patient/`+`user/` blocked for `client_credentials`
   - Multi-char operations expanded (`cruds` → `c`, `r`, `u`, `d`, `s`)
6. Build patch ops: remove existing scopes, add validated scopes
7. Resolve patient ID (priority): `OH_patient/` scope → EHR context (`OH_launch/`) → SCIM `fhirUser` attribute
8. Inject claims: `consent_id`, `patient`, `encounter`

## Setup

```bash
cp Config.toml.example Config.toml
# Edit Config.toml with your values
bal run
```

### Required config

| Key | Description |
|-----|-------------|
| `isBaseUrl` | WSO2 IS (7.3.0+) base URL, used for consent lookup, SCIM and token introspect |
| `scimClientId` / `scimClientSecret` | Credentials of the management application, used to call the consent management API and SCIM |

The management application must be authorized for the Consent Management API with the `internal_consent_mgt_consent_view` scope (and `internal_user_mgt_view` for SCIM). The service requests a single token with both scopes. No separate consent store needs to be deployed.

### Optional config

| Key | Default | Description |
|-----|---------|-------------|
| `ehrContextResolveUrl` | `""` | EHR launch context endpoint; skipped when blank |
| `consentApiPath` | `"/api/identity/consent-mgt/v2.0"` | Consent management API path on IS; use `/t/<tenant-domain>/api/identity/consent-mgt/v2.0` for tenants |
| `scimTokenEndpoint` | `""` | Management app token endpoint. Defaults to `{isBaseUrl}/oauth2/token` |
| `scimPatientGroupName` | `"patient"` | Group name used to identify patient users |
| `fhirUserAttributeName` | `"fhirUser"` | SCIM custom attribute holding the FHIR user reference |
| `patientAttributeName` | `"patient"` | SCIM custom attribute holding the patient resource reference |
| `keystorePath` | `""` | Path to the keystore file for the HTTPS listener; HTTP used when blank |
| `keystorePassword` | `""` | Password to open the keystore |
| `consentServiceId` | `smart-on-fhir` | Must match `serviceId` of `consent-app-bff`; used to find the user's consent when IS sends no `sessionDataKeyConsent` |
| `alwaysAllowedScopes` | `["openid"]` | Scopes that bypass consent checks |

### Example Config.toml

```toml
hostname = "localhost"
port = 9093

ehrContextResolveUrl = "https://ehr.example.com/launch-context"

isBaseUrl = "https://localhost:9443"
# Management application (authorized for Consent Management API + SCIM)
scimClientId = "<client-id>"
scimClientSecret = "<client-secret>"

# Optional: HTTPS listener
keystorePath = "/path/to/keystore.p12"
keystorePassword = "<keystore-password>"

alwaysAllowedScopes = ["openid", "fhirUser"]
```

## Tests

```bash
bal test
```

Tests use in-process mock IS consent API and SCIM listeners defined in `tests/`.
