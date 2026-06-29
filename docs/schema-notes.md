# Tranche — Schema Notes

Design rationale for the initial Flyway schema (`V1__init_schema.sql`). This document explains **why** the tables are shaped this way, with emphasis on allocation correctness, auditability, and interview clarity.

---

## Design Principles

1. **Surrogate BIGINT primary keys** for efficient joins and JPA mapping; **`public_id` (UUID)** on `users` for stable external API identifiers (JWT `sub`, API responses).
2. **`DECIMAL(19,4)`** for all monetary fields — avoids floating-point drift in fund locking and yield calculations.
3. **`TIMESTAMP(6)`** microsecond precision on transactional tables — supports ordering concurrent commitments and audit timelines.
4. **`version` columns** on hot mutable rows (`users`, `investor_profiles`, `opportunities`, `investment_orders`, `portfolio_positions`) for optimistic locking outside the allocation critical path.
5. **CHECK constraints** at the database layer as a safety net; business rules are still enforced in the domain layer.
6. **Append-only `audit_logs`** — no `updated_at`; immutability is a compliance requirement.
7. **InnoDB + foreign keys** — referential integrity and row-level locking for `SELECT ... FOR UPDATE`.

---

## Entity Relationships

```
users (1) ── (0..1) investor_profiles
users (1) ── (0..1) issuers

issuers (1) ── (N) opportunities

opportunities (1) ── (N) investment_orders
investment_orders (1) ── (0..1) allocations   [1:1 when confirmed]

allocations (1) ── (1) portfolio_positions

audit_logs ── references any entity (polymorphic via entity_type + entity_id)
outbox_events ── references any aggregate (polymorphic via aggregate_type + aggregate_id)
```

---

## Table-by-Table Notes

### `users`

| Decision | Rationale |
|---|---|
| `public_id` UUID separate from `id` | Internal joins use compact BIGINT; APIs and JWTs expose non-sequential UUIDs |
| `role` ENUM | Only three roles exist; ENUM is self-documenting and index-friendly |
| `version` | Supports optimistic updates to user metadata without blocking allocation |

### `investor_profiles`

| Decision | Rationale |
|---|---|
| Separate from `users` | Wallet state is investor-specific; keeps `users` thin for auth |
| `wallet_balance` + `locked_balance` | Available funds = `wallet_balance - locked_balance` (or model as move between columns on lock). Split makes locked funds explicit for audit and reconciliation |
| `version` | **Pessimistic lock** is primary during allocation; `version` catches concurrent profile edits |
| `CHECK (wallet_balance >= 0)` | DB-level guard against negative balances if application logic fails |

### `issuers`

| Decision | Rationale |
|---|---|
| 1:1 with `users` | Issuer is a role specialization; opportunities belong to issuer entity, not raw user |

### `opportunities`

| Decision | Rationale |
|---|---|
| `remaining_units` denormalized | Allocation engine decrements under row lock without aggregating from `allocations` — O(1) availability check |
| `total_units` + `remaining_units` | `CHECK (remaining_units <= total_units AND remaining_units >= 0)` prevents over-allocation at DB layer |
| `version` | Optimistic lock for admin/issuer metadata edits; allocation path uses pessimistic `FOR UPDATE` |
| `maturity_date` nullable until LIVE | Computed when published: `published_at + tenure_days` |
| Lifecycle `status` ENUM | Seven states map directly to the domain state machine |
| `risk_grade` ENUM | Constrained to A–D for listing filters and index selectivity |

### `investment_orders`

| Decision | Rationale |
|---|---|
| `UNIQUE (idempotency_key, investor_id)` | **Core idempotency guarantee** — network retries cannot create duplicate orders for the same investor |
| `fill_status` vs `status` | `fill_status` (FULL/PARTIAL/REJECTED) describes allocation outcome; `status` (PENDING/CONFIRMED/REJECTED) describes order lifecycle |
| `correlation_id` | Links order to request trace for audit and debugging |
| `units_allocated` / `amount_allocated` defaults 0 | Rejected orders retain requested amounts with zero allocation |

### `allocations`

| Decision | Rationale |
|---|---|
| `UNIQUE (investment_order_id)` | Enforces 1:1 order-to-allocation; an order cannot be double-allocated |
| Denormalized `opportunity_id`, `investor_id` | Avoids join through `investment_orders` for portfolio and listing queries |
| `discount_amount`, `expected_return` stored | Snapshot at allocation time — yield terms are immutable even if opportunity metadata changes later |
| No `version` | Allocations are immutable once created |

### `portfolio_positions`

| Decision | Rationale |
|---|---|
| `UNIQUE (allocation_id)` | One position per allocation; idempotent portfolio updates |
| `status` ENUM (ACTIVE/MATURED/SETTLED) | Position lifecycle independent of opportunity lifecycle timing |
| `realized_yield` nullable | Populated only after settlement |
| `version` | Supports concurrent settlement updates |

### `audit_logs`

| Decision | Rationale |
|---|---|
| No `updated_at` | Append-only by design |
| `actor_id` nullable | SYSTEM actions have no user actor |
| `before_state` / `after_state` JSON | Flexible snapshots without schema churn per action type |
| Polymorphic `entity_type` + `entity_id` | Single table for all audited entities |

### `outbox_events`

| Decision | Rationale |
|---|---|
| Inserted in same TX as business action | Transactional outbox — no lost events on rollback |
| `idx_outbox_events_poller (status, created_at)` | Poller query: `WHERE status = 'PENDING' ORDER BY created_at` |
| `retry_count`, `last_error` | Supports future retry/backoff without schema change |
| `payload` JSON | Event-specific data without per-event-type tables |

---

## Indexing Strategy

### Required indexes (from architecture)

| Index | Table | Columns | Query Pattern |
|---|---|---|---|
| Listing filter | `opportunities` | `(status, risk_grade, maturity_date)` | `WHERE status = 'LIVE' AND risk_grade = ? ORDER BY maturity_date` |
| Order history | `investment_orders` | `(opportunity_id, investor_id, created_at)` | Investor's orders on an opportunity, time-ordered |
| Idempotency | `investment_orders` | `UNIQUE (idempotency_key, investor_id)` | `SELECT` on retry before allocation |
| Portfolio | `allocations` | `(investor_id, created_at)` | Investor allocation history |
| Audit timeline | `audit_logs` | `(entity_type, entity_id, created_at)` | Entity audit trail |
| Outbox poller | `outbox_events` | `(status, created_at)` | Pending event dispatch |

### Additional indexes

| Index | Purpose |
|---|---|
| `opportunities (issuer_id, status)` | Issuer dashboard |
| `investment_orders (opportunity_id, created_at)` | All orders on an opportunity |
| `investment_orders (investor_id, created_at)` | Investor order history across opportunities |
| `portfolio_positions (investor_id, status, maturity_date)` | Portfolio view with maturity sorting |
| `audit_logs (actor_id, created_at)` | Admin investigation by actor |
| `outbox_events (aggregate_type, aggregate_id)` | Lookup events for a specific aggregate |

### What we deliberately did NOT index yet

- Full-text on `opportunities.description` — not in MVP query patterns
- Covering indexes — add after `EXPLAIN` on real query load
- Partial indexes (`WHERE status = 'LIVE'`) — MariaDB support varies; composite index on `status` leading column is sufficient at MVP scale

---

## Concurrency & Integrity

### Allocation critical path (schema support)

```
BEGIN;
  SELECT * FROM opportunities WHERE id = ? FOR UPDATE;   -- row lock
  SELECT * FROM investor_profiles WHERE user_id = ? FOR UPDATE;
  -- validate remaining_units, wallet balance
  INSERT investment_orders ...;
  INSERT allocations ...;
  UPDATE opportunities SET remaining_units = remaining_units - ?;
  UPDATE investor_profiles SET locked_balance = locked_balance + ?;
  INSERT audit_logs ...;
  INSERT outbox_events ...;
  INSERT portfolio_positions ...;
COMMIT;
```

Schema elements that make this safe:

1. **`remaining_units` CHECK** — cannot go negative even if application has a bug
2. **`UNIQUE (idempotency_key, investor_id)`** — duplicate retries fail fast or return existing row
3. **`UNIQUE (investment_order_id)` on allocations** — no double allocation per order
4. **Foreign keys** — orphan allocations/orders cannot exist
5. **InnoDB row locks** — `FOR UPDATE` on `opportunities` serializes concurrent commits

### Idempotency scope

The unique key is **per investor**, not global. Two different investors may coincidentally use the same UUID (extremely unlikely) without conflict. Same investor + same key = same result — correct semantics.

---

## Seed Data (`V2__seed_reference_data.sql`)

| User | Email | Role | Notes |
|---|---|---|---|
| Platform Admin | `admin@tranche.local` | ADMIN | Not registerable via API |
| Acme Issuer | `issuer@tranche.local` | ISSUER | Linked to Acme Corp issuer profile |
| Jane Investor | `investor1@tranche.local` | INVESTOR | $500,000 wallet |
| John Investor | `investor2@tranche.local` | INVESTOR | $500,000 wallet |

Password for all: `Password123!`

Fixed `public_id` UUIDs (prefix `a0000000-...`) for reproducible demos and documentation examples.

---

## Migration Order

| Version | File | Purpose |
|---|---|---|
| V1 | `V1__init_schema.sql` | Full schema, indexes, constraints |
| V2 | `V2__seed_reference_data.sql` | Demo users, issuer, investor profiles |

No `opportunity_tranches` table in V1 — single-tranche opportunities are sufficient for MVP. A future `V3` can add tranches if invoice splitting is required.

---

## JPA Mapping Notes (for next phase)

- `@Version` maps to `version` columns on mutable entities
- `public_id` on `User` should use `@Column(columnDefinition = "CHAR(36)")` or UUID type
- JSON columns map to `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6)
- ENUMs map to Java enums with `@Enumerated(EnumType.STRING)`
- Do not use `ddl-auto: update` — Flyway owns the schema; Hibernate `validate` only
