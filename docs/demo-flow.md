# Demo Flow

End-to-end walkthrough for interviews and local demos. All seeded users share password **`Password123!`**.

| Role | Email |
|---|---|
| Admin | `admin@tranche.local` |
| Issuer | `issuer@tranche.local` |
| Investor 1 | `investor1@tranche.local` |
| Investor 2 | `investor2@tranche.local` |

Base URL: `http://localhost:8080`

---

## 1. Start infrastructure and app

```bash
cd Tranche
docker compose up -d          # MariaDB + Redis
./mvnw spring-boot:run        # API on :8080
```

Optional full stack (app in Docker):

```bash
docker compose --profile full up -d --build
```

---

## 2. Login helpers

Save tokens for each role:

```bash
export ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@tranche.local","password":"Password123!"}' \
  | jq -r .accessToken)

export ISSUER_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"issuer@tranche.local","password":"Password123!"}' \
  | jq -r .accessToken)

export INV1_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"investor1@tranche.local","password":"Password123!"}' \
  | jq -r .accessToken)

export INV2_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"investor2@tranche.local","password":"Password123!"}' \
  | jq -r .accessToken)
```

---

## 3. Find the seeded demo opportunity

Flyway seeds **Acme Q1 Receivables — Invoice #INV-2026-0142** in `DRAFT` status.

```bash
curl -s "http://localhost:8080/api/v1/opportunities?status=DRAFT" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

export OPP_ID=$(curl -s "http://localhost:8080/api/v1/opportunities?status=DRAFT" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq '.content[] | select(.title | contains("INV-2026-0142")) | .id')
```

---

## 4. Issuer submits for review

```bash
curl -s -X POST "http://localhost:8080/api/v1/opportunities/$OPP_ID/submit" \
  -H "Authorization: Bearer $ISSUER_TOKEN" | jq
```

---

## 5. Admin approves and publishes

```bash
curl -s -X POST "http://localhost:8080/api/v1/opportunities/$OPP_ID/review" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"action":"APPROVE","comment":"Credit policy satisfied"}' | jq

curl -s -X POST "http://localhost:8080/api/v1/opportunities/$OPP_ID/publish" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

Opportunity is now **LIVE** — 100 units at $25,000 per unit.

---

## 6. Two investors racing for units

Scenario: only **15 units** remain (after optional pre-commitments) or run the race on a fresh LIVE opportunity with limited units.

### Option A — race on a nearly-full opportunity

Pre-fill 85 units as investor 1, then race for the last 15:

```bash
curl -s -X POST "http://localhost:8080/api/v1/opportunities/$OPP_ID/commitments" \
  -H "Authorization: Bearer $INV1_TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H 'Content-Type: application/json' \
  -d '{"unitsRequested":85,"amount":2125000.0000}' | jq
```

### Option B — concurrent race (run in two terminals)

Both investors request **10 units** at the same time when **15 remain**:

**Terminal 1 (investor 1):**

```bash
curl -s -X POST "http://localhost:8080/api/v1/opportunities/$OPP_ID/commitments" \
  -H "Authorization: Bearer $INV1_TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H 'Content-Type: application/json' \
  -d '{"unitsRequested":10,"amount":250000.0000}' | jq
```

**Terminal 2 (investor 2)** — run immediately:

```bash
curl -s -X POST "http://localhost:8080/api/v1/opportunities/$OPP_ID/commitments" \
  -H "Authorization: Bearer $INV2_TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H 'Content-Type: application/json' \
  -d '{"unitsRequested":10,"amount":250000.0000}' | jq
```

**Expected outcome:**
- Combined allocation ≤ 15 units (never overbooks).
- One investor may receive a **partial fill** (`fillStatus: PARTIAL`) if demand exceeds supply.
- `remainingUnits` on the opportunity matches `100 - sum(allocations)`.

Verify:

```bash
curl -s "http://localhost:8080/api/v1/opportunities/$OPP_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.remainingUnits, .status'
```

---

## 7. Idempotency replay

Retry the same commitment with the **same** `Idempotency-Key`:

```bash
export IDEM_KEY=$(uuidgen)

curl -s -w "\nHTTP %{http_code}\n" -X POST "http://localhost:8080/api/v1/opportunities/$OPP_ID/commitments" \
  -H "Authorization: Bearer $INV1_TOKEN" \
  -H "Idempotency-Key: $IDEM_KEY" \
  -H 'Content-Type: application/json' \
  -d '{"unitsRequested":5,"amount":125000.0000}'

curl -s -w "\nHTTP %{http_code}\n" -X POST "http://localhost:8080/api/v1/opportunities/$OPP_ID/commitments" \
  -H "Authorization: Bearer $INV1_TOKEN" \
  -H "Idempotency-Key: $IDEM_KEY" \
  -H 'Content-Type: application/json' \
  -d '{"unitsRequested":5,"amount":125000.0000}'
```

First call → **201 Created**. Replay → **200 OK** with the same `orderId`.

---

## 8. Portfolio views

```bash
curl -s http://localhost:8080/api/v1/portfolio \
  -H "Authorization: Bearer $INV1_TOKEN" | jq

curl -s http://localhost:8080/api/v1/portfolio \
  -H "Authorization: Bearer $INV2_TOKEN" | jq
```

---

## 9. Admin maturity and settlement

```bash
curl -s -X POST "http://localhost:8080/api/v1/opportunities/$OPP_ID/transition" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"targetStatus":"MATURED"}' | jq

curl -s -X POST "http://localhost:8080/api/v1/opportunities/$OPP_ID/transition" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"targetStatus":"SETTLED"}' | jq
```

---

## 10. Audit timeline

```bash
curl -s "http://localhost:8080/api/v1/audit/Opportunity/$OPP_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

---

## 11. Outbox (admin)

```bash
curl -s "http://localhost:8080/api/v1/admin/outbox" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

curl -s -X POST "http://localhost:8080/api/v1/admin/outbox/poll" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

---

## Run automated tests

Tests use **Testcontainers** (MariaDB + Redis). Docker must be available:

```bash
./mvnw test
```

Key integration suites:

| Test class | What it proves |
|---|---|
| `ConcurrentCommitmentIntegrationTest` | 20-thread service-layer race, no over-allocation |
| `InvestorRaceHttpIntegrationTest` | HTTP race between two investors |
| `PartialFillIntegrationTest` | Partial fill when demand > supply |
| `IdempotencyIntegrationTest` / `CommitmentHttpIntegrationTest` | Idempotent commitment semantics |
| `OpportunityLifecycleIntegrationTest` | DRAFT → SETTLED with invalid transition guard |
| `AuthRoleIntegrationTest` | Role-based API boundaries |
| `FlywaySeedDataIntegrationTest` | Seeded users and demo opportunity |

---

## Interview talking points

1. **Allocation correctness** — pessimistic opportunity lock, atomic `remaining_units`, partial fills.
2. **Idempotency** — `Idempotency-Key` + unique constraint prevents double spend on retries.
3. **Auditability** — every transition and allocation logged with correlation ID.
4. **Outbox** — investment/maturity events emitted transactionally, polled async.
5. **Defense in depth** — validation, rate limits, RBAC, safe error envelope (see `docs/non-functional-design.md`).
