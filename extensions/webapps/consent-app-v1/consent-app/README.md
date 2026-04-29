# consent-app

React + TypeScript UI for the external consent management flow. Presents consent purposes to the authenticated user and submits their approve/deny decision back to the Asgardeo identity server via the [consent-app-bff](../../demo-backends/consent-app-bff/).

## Prerequisites

- Node `^18.18.0 || >=20.0.0`

## Setup

```bash
cp .env.example .env   # fill in values (see Configuration below)
npm install
npm run dev
```

## Configuration

`.env` is gitignored. Copy `.env.example` and set:

| Variable | Required | Description |
|----------|----------|-------------|
| `VITE_CONSENT_BFF_URL` | yes | Base URL of the consent-app BFF (e.g. `http://localhost:9095`) |
| `VITE_ASGARDEO_AUTHORIZE_URL` | yes | Asgardeo `/oauth2/authorize` endpoint for your tenant (e.g. `https://api.asgardeo.io/t/<tenant>/oauth2/authorize`) |

## Commands

| Command | Description |
|---------|-------------|
| `npm run dev` | Start dev server |
| `npm run build` | Type-check and build for production |
| `npm run lint` | Run ESLint |
| `npm run preview` | Preview the production build locally |
