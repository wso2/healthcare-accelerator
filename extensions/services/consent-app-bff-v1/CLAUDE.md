# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Service Overview

This is the **Consent App BFF (Backend-for-Frontend)** — a Ballerina service that acts as a proxy between the React `consent-app` frontend and the Asgardeo identity server. It runs on port **9095** and exposes a `/v1` HTTP service.

## Commands

```bash
# Run the service
bal run

# Run with explicit config file
BAL_CONFIG_FILES=Config.toml bal run
```

Run the tests with `bal test` from the service directory. Tests use a mocked `callAsgardeo` and an in-process mock OpenFGC HTTP service (ports 9096/9097 defined in `tests/mock_openfgc_service.bal`). A separate `tests/Config.toml` provides test-time configuration.

## Configuration

`Config.toml` is gitignored (contains secrets). Copy the example and fill in your values:

```bash
cp Config.toml.example Config.toml
```

Three required fields:

```toml
tenantDomain = "<asgardeo-tenant-slug>"   # e.g. "anjucha"
clientId     = "<oauth2-client-id>"
clientSecret = "<oauth2-client-secret>"
```

These map to `configurable` variables in `config.bal` and are consumed by `connections.bal`.

## Architecture

### File roles

| File | Purpose |
|------|---------|
| `config.bal` | All `configurable` variables: Asgardeo credentials, CORS origin, OpenFGC settings, and the `consentPurpose[]` array |
| `connections.bal` | Builds the shared `asgardeoClient` (plain HTTP) and `openfgcClient`. `getAsgardeoToken()` creates a fresh `ClientOAuth2Provider` on each call to avoid eager token fetch at module init. |
| `service.bal` | HTTP listener + `/v1` service: `GET /get-consent-data`, `POST /submit-consent`, `POST /bind-consent-to-token`. Contains `callAsgardeo()` with 401-retry logic. |
| `types.bal` | All record types: `ConsentPurposeConfig`, `Purpose`, `ConsentSubmission`, `ConsentedPurpose`, `ConsentPageData`, `ConsentSubmissionResponse`, OpenFGC payload types, Asgardeo action types |

### Token refresh strategy

`callAsgardeo()` manually sets the `Authorization` header (rather than using a built-in OAuth2 interceptor) so it can detect a `401` response, fetch a fresh token via `getAsgardeoToken()`, and retry once. `getAsgardeoToken()` creates a new `ClientOAuth2Provider` on each call (rather than reusing a module-level one) to avoid an eager token fetch during module init, which would fail in tests before the mock listener is up.
