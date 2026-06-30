# Interview Demo Script

Timed walkthrough for live demos (~18 minutes). Use the **React UI** as the primary path; keep [demo-flow.md](demo-flow.md) as a curl backup.

**Password for all seeded users:** `Password123!`

| Role | Email |
|------|-------|
| Admin | `admin@tranche.local` |
| Issuer | `issuer@tranche.local` |
| Investor 1 | `investor1@tranche.local` |
| Investor 2 | `investor2@tranche.local` |

---

## Before you start (5 min prior)

```bash
# Terminal 1 — API
cd Tranche
docker compose up -d
./mvnw spring-boot:run

# Terminal 2 — UI
cd frontend && npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Optional API explorer: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) (dev profile).

**Talking-point opener (30 sec):**

> Tranche is an invoice discounting platform where the hard problem is concurrent allocation — many investors committing at once without overbooking or double-reserving funds. I built a pessimistic-locking allocation engine with idempotent commits, partial fills, an append-only audit log, and a transactional outbox.

---

## Act 1 — Platform overview (2 min)

| Time | Action | Say |
|------|--------|-----|
| 0:00 | Landing page — scroll features + lifecycle + three workspaces | "Modular monolith: issuer, investor, and admin workspaces on one engine." |
| 1:30 | Click **Sign in** (don't log in yet) | "Full RBAC — each role sees only what they need." |

---

## Act 2 — Issuer publishes (3 min)

| Time | Action | Say |
|------|--------|-----|
| 2:00 | Sign in as **issuer@tranche.local** | |
| 2:30 | **My invoices** → open seeded **Acme Q1 Receivables** (or **New opportunity** if fresh DB) | "Issuer creates draft opportunities with face value, discount, tenure, risk grade." |
| 3:00 | Submit for review (detail page) | "Draft → under review. State machine rejects invalid transitions." |
| 4:30 | Sign out | |

---

## Act 3 — Admin publishes to marketplace (2 min)

| Time | Action | Say |
|------|--------|-----|
| 5:00 | Sign in as **admin@tranche.local** | |
| 5:30 | **Review queue** → approve → publish | "Admin gate before investors see it. Only LIVE opportunities accept commitments." |
| 6:30 | Optional: **Onboarding** tab if you registered a new user earlier | "KYC/KYB gates are real — commitments blocked until approval." |
| 7:00 | Sign out | |

---

## Act 4 — The race (4 min) ⭐ core story

Use **two browser windows** (or normal + incognito).

| Time | Action | Say |
|------|--------|-----|
| 7:00 | Window A: **investor1@tranche.local** → Marketplace → open LIVE opportunity | |
| 7:30 | Window B: **investor2@tranche.local** → same opportunity | |
| 8:00 | Both request units when few remain (e.g. 10 units each, ~15 left) | "Simulating concurrent demand on the last units." |
| 8:30 | Submit both commitments quickly | |
| 9:00 | Compare results — partial fill possible, never overbooked | "Pessimistic lock on opportunity row, atomic unit decrement, fund lock in same transaction." |
| 10:00 | Each investor → **Portfolio** | "Positions update with expected return and maturity." |
| 11:00 | If asked about idempotency | "Same Idempotency-Key returns original order — 201 then 200. See Swagger or demo-flow.md." |

---

## Act 5 — Audit & lifecycle close (3 min)

| Time | Action | Say |
|------|--------|-----|
| 11:00 | Admin → opportunity detail → **Audit timeline** | "Append-only log: actor, correlation ID, before/after state." |
| 12:00 | Transition **Matured** → **Settled** | "Controlled lifecycle through settlement." |
| 13:00 | Optional: Swagger → `POST .../commitments` | "OpenAPI documents the API; JWT via Authorize button." |
| 14:00 | Optional: mention `./mvnw test` + CI | "ConcurrentCommitmentIntegrationTest — 20-thread race at service layer." |

---

## Act 6 — Wrap (1 min)

| Time | Action | Say |
|------|--------|-----|
| 14:30 | Stop at landing or README on GitHub | |
| 15:00 | Close with honest tradeoffs | "Wallet and notifications are stubbed for demo; allocation correctness, audit, and gates are real." |

---

## Short path (10 min — time constrained)

1. Admin: publish seeded opportunity (2 min)
2. Two investors: concurrent commits (4 min)
3. Portfolio + audit timeline (2 min)
4. One sentence on tests + idempotency (2 min)

---

## If something breaks

| Problem | Fix |
|---------|-----|
| Port 8080 in use | `fuser -k 8080/tcp` then restart API |
| `INSUFFICIENT_FUNDS` | Investors need $3M wallet (V4 seed); restart app or use **Add demo funds** |
| Opportunity not DRAFT | Use `?status=LIVE` in API or pick from admin list |
| UI login fails | Check API health: `curl localhost:8080/actuator/health` |
| Deep dive backup | [demo-flow.md](demo-flow.md) curl commands |

---

## Questions they'll likely ask

1. **Lock ordering?** Opportunity `FOR UPDATE` first, then wallet — prevents deadlocks.
2. **Idempotency?** Header + `UNIQUE (idempotency_key, investor_id)` + replay returns same result.
3. **Partial fills?** `AllocationCalculator` caps units at `remaining_units`.
4. **Why monolith?** Clear module boundaries without microservice ops cost.
5. **Production next?** Real payments, outbox worker, observability — see [final-review-notes.md](final-review-notes.md).

---

## Related docs

| Doc | Use |
|-----|-----|
| [demo-flow.md](demo-flow.md) | curl race, idempotency, automated tests |
| [architecture.md](architecture.md) | Layering, locking, modules |
| [allocation-engine.md](allocation-engine.md) | Commitment path deep dive |
| [final-review-notes.md](final-review-notes.md) | Interview talking points |
