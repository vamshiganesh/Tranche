# Tranche Frontend

React + Vite + TypeScript web client for the Tranche invoice discounting platform.

## Stack

- React 19, React Router 7
- Vite dev server with proxy to the Spring Boot API (`/api` → `localhost:8080`)
- Archivo + IBM Plex Mono typography, warm institutional design system

## Run locally

Start the backend first (from `Tranche/`):

```bash
docker compose up -d
mvn spring-boot:run
```

Then start the frontend:

```bash
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

## Demo accounts

Password for all seeded users: `Password123!`

| Role | Email |
|------|-------|
| Admin | `admin@tranche.local` |
| Issuer | `issuer@tranche.local` |
| Investor | `investor1@tranche.local`, `investor2@tranche.local` |

## Role workflows

- **Investor:** browse live marketplace, place commitments with idempotency keys, view portfolio
- **Issuer:** create draft opportunities, submit for review, track status
- **Admin:** review queue, approve/reject, publish, mature, settle, audit timeline

## Build

```bash
npm run build
npm run preview
```

Production build outputs to `dist/`. Configure your reverse proxy to forward `/api` to the backend.
