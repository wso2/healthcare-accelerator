# Consent App

A React-based OAuth/OIDC consent authorization webapp for WSO2 Healthcare Accelerator. It manages user consent for resource access during SMART on FHIR authorization flows.

## Overview

The app handles two workflows:
- **Practitioner flow**: Practitioner users first select a patient before approving consent scopes
- **Standard user flow**: Non-practitioner users go directly to the consent scope approval screen

## Getting Started

```bash
npm install
npm run dev
```

Build for production:
```bash
npm run build
```

## Required Query Parameters

The webapp requires the following query parameters to be present in the URL when launched:

| Parameter | Required | Description |
|-----------|----------|-------------|
| `sessionDataKeyConsent` | Yes | Session token issued by the authorization server for the consent flow |
| `spId` | Yes | Service Provider ID identifying the OAuth application requesting consent |

**Example URL:**
```
/consent-app/?sessionDataKeyConsent=<token>&spId=<service-provider-id>
```

If either parameter is missing, the app renders an error message and does not proceed.

## Expected Backend API

The webapp communicates with a backend service (BFF). In development, API calls to `/api/*` are proxied to `http://localhost:9091` via Vite config.

### `GET /api/consent-context`

Fetches the consent session details.

**Query Parameters:**
- `sessionDataKeyConsent` — session token
- `spId` — service provider ID

**Response:**
```json
{
  "sessionDataKeyConsent": "string",
  "spId": "string",
  "user": "string",
  "scopes": ["openid", "launch/patient", "patient/*.read"]
}
```

---

### `GET /api/me`

Fetches details about the logged-in user, used to determine whether to show the patient picker (practitioner flow).

**Query Parameters:**
- `userId` — user ID from the consent context

**Response (SCIM format):**
```json
{
  "id": "string",
  "userName": "string",
  "name": {
    "givenName": "string",
    "familyName": "string"
  },
  "emails": [
    { "type": "work", "value": "user@example.com" }
  ],
  "roles": [
    { "value": "role-name" }
  ],
  "urn:scim:schemas:extension:custom:User": {
    "fhirUser": "Practitioner/123"
  }
}
```

The `fhirUser` field determines the flow: if it starts with `Practitioner/`, the patient picker is shown.

**Error status codes:**
- `400` — userId is missing or invalid
- `502` — identity server unreachable

---

### `GET /api/patients`

Fetches the list of patients available for selection (practitioner flow only).

**Response (SCIM format):**
```json
{
  "Resources": [
    {
      "id": "string",
      "userName": "MRN-001",
      "name": {
        "givenName": "string",
        "familyName": "string"
      },
      "urn:scim:schemas:extension:custom:User": {
        "fhirUser": "Patient/456"
      }
    }
  ]
}
```

**Error status codes:**
- `502` — identity server unreachable
- Empty `Resources` array — displays "No patients found" message

---

## Scope Handling

- **`OH_launch/*` scopes**: Hidden from the user, always included in approval
- **SMART scopes** (`patient/`, `user/`, `system/`): Displayed for user selection, validated against pattern `{compartment}/{resourceType}.{permissions}`
- **Other scopes**: Displayed for user selection without additional validation

## Tech Stack

- React 18
- Vite
- Oxygen UI (WSO2 design system)
- Material UI
