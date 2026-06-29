# Tranche — Architecture

This document describes the technical architecture of Tranche: a modular monolith for invoice discounting with an allocation engine at its core.

---

## Architectural Style

**Modular monolith** with microservice-style module boundaries inside a single deployable unit.

| Principle | Decision |
|---|---|
| Deployment | Single JAR, single process |
| Module coupling | Modules communicate via service interfaces, not direct repository access across boundaries |
| Data store | Shared MariaDB schema with table-level ownership per module |
| Extraction path | Any module can become a microservice later by replacing in-process calls with messaging |

---

## Package Structure

```
com.tranche
├── TrancheApplication.java
├── common
│   ├── config          # Shared Spring configuration
│   ├── exception       # Global exception handling, error codes
│   ├── security        # JWT filter, SecurityConfig
│   └── util            # Idempotency helpers, correlation IDs
├── auth
│   ├── controller
│   ├── service
│   ├── domain
│   └── repository
├── issuer
│   ├── controller
│   ├── service
│   ├── domain
│   └── repository
├── investor
│   ├── controller
│   ├── service
│   ├── domain
│   └── repository
├── opportunity
│   ├── controller
│   ├── service
│   ├── domain          # State machine, Opportunity entity
│   └── repository
├── allocation
│   ├── controller
│   ├── service         # AllocationEngine, CommitmentService
│   ├── domain          # InvestmentOrder, Allocation entities
│   └── repository
├── portfolio
│   ├── controller
│   ├── service
│   ├── domain
│   └── repository
├── audit
│   ├── service         # AuditService (called by other modules)
│   ├── domain
│   └── repository
└── notification
    ├── service         # OutboxWriter, OutboxPoller (mock)
    ├── domain
    └── repository
```

Each module follows **clean layering**:

```
Controller  →  HTTP boundary, request validation, DTO mapping
Service     →  Use-case orchestration, transaction boundaries
Domain      →  Entities, value objects, state machine, business rules
Repository  →  JPA interfaces, query methods
Infra       →  Redis adapters, outbox dispatch (where not in service)
```

**Cross-module rule:** A module's `repository` package is private to that module. Other modules call its `service` layer only.

---

## Layer Responsibilities

### Controller

- Accept and validate HTTP requests (`@Valid`, custom validators)
- Map DTOs ↔ domain objects
- Enforce authorization annotations (`@PreAuthorize`)
- Return standardized API responses and error envelopes
- Extract `Idempotency-Key` header and correlation ID

### Service

- Own transaction boundaries (`@Transactional`)
- Orchestrate domain logic and cross-module calls
- Invoke `AuditService` and `OutboxWriter` within the same transaction
- Never expose JPA entities to controllers

### Domain

- Encapsulate business invariants (e.g., "only LIVE opportunities accept commitments")
- State pattern for opportunity lifecycle transitions
- Strategy pattern for yield calculation
- Factory for outbox event creation
- No Spring annotations in pure domain objects where possible

### Repository

- Spring Data JPA interfaces
- Custom `@Query` for allocation-critical reads (pessimistic lock queries)
- No business logic

---

## Request Flow: Investor Commitment API

The commitment path is the most critical flow in the system.

```
Client
  │
  ▼
┌─────────────────────────────────────────────────────────┐
│  AllocationController                                   │
│  POST /api/v1/opportunities/{id}/commitments            │
│  Headers: Authorization, Idempotency-Key                │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  IdempotencyInterceptor / CommitmentService             │
│  1. Check idempotency key → return cached if exists     │
└────────────────────┬────────────────────────────────────┘
                     │ (new request)
                     ▼
┌─────────────────────────────────────────────────────────┐
│  @Transactional                                         │
│  CommitmentService.allocate()                           │
│                                                         │
│  2. Load opportunity WITH PESSIMISTIC WRITE LOCK        │
│  3. Validate state == LIVE                              │
│  4. Compute available units                           │
│  5. Determine fill: full | partial | rejected           │
│  6. Create InvestmentOrder + Allocation records         │
│  7. Lock investor funds (InvestorService)                 │
│  8. Decrement opportunity remaining units               │
│  9. Transition to FULLY_SUBSCRIBED if depleted          │
│  10. Write audit log entries                            │
│  11. Write outbox event (INVESTMENT_SUCCESSFUL)         │
│  12. Store idempotency record + response                │
│  13. Invalidate Redis cache for opportunity listing     │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
              Response DTO
         (status, allocated units, amount)
```

### Sequence: Two Concurrent Investors

```
Investor A ──commit──►  ┐
                         ├──► DB (serializes via row lock on opportunity)
Investor B ──commit──►  ┘

Timeline:
  T1: A acquires pessimistic lock on opportunity row
  T2: B blocks waiting for lock
  T3: A allocates 60 of 100 units → commits → releases lock
  T4: B acquires lock, sees 40 remaining units
  T5: B requests 50 → partial fill of 40 → commits
  T6: Opportunity transitions to FULLY_SUBSCRIBED
```

---

## Locking Strategy

### Primary: Pessimistic Write Lock on Opportunity

```sql
SELECT * FROM opportunities WHERE id = ? FOR UPDATE
```

Used during every allocation. Serializes concurrent commitments on the same opportunity. Trade-off: throughput per opportunity is limited, but correctness is guaranteed.

### Secondary: Optimistic Locking (Version Column)

`@Version` on `Opportunity`, `InvestmentOrder`, and `InvestorProfile` (wallet balance). Catches concurrent updates outside the allocation path (e.g., admin editing metadata). Allocation path relies on pessimistic lock, not optimistic retry.

### Idempotency Lock

Unique constraint on `(idempotency_key, investor_id)`. Prevents duplicate order creation on retries. Checked before entering the allocation transaction.

### Fund Reservation

Investor wallet balance updated within the same `@Transactional` boundary as allocation. Pessimistic lock on `investor_profiles` row when deducting available balance.

### Strategy Summary

| Scenario | Strategy |
|---|---|
| Concurrent commitments on same opportunity | Pessimistic write lock on opportunity |
| Admin updates opportunity metadata | Optimistic version check |
| Investor wallet deduction | Pessimistic lock on investor profile |
| Retry with same idempotency key | Unique constraint lookup, no lock needed |
| Read-heavy opportunity listing | No lock; Redis cache with TTL |

---

## Outbox Design

Notifications are emitted via the **transactional outbox pattern** to avoid dual-write problems.

### Table: `outbox_events`

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | Auto-increment |
| event_type | VARCHAR | `INVESTMENT_SUCCESSFUL`, `MATURITY_DUE`, `SETTLEMENT_COMPLETE` |
| aggregate_type | VARCHAR | e.g., `Opportunity`, `Allocation` |
| aggregate_id | BIGINT | Entity ID |
| payload | JSON | Event-specific data |
| status | ENUM | `PENDING`, `PUBLISHED`, `FAILED` |
| created_at | TIMESTAMP | |
| published_at | TIMESTAMP | Nullable |

### Write Path

Outbox events are inserted in the **same database transaction** as the business operation (allocation, settlement). If the transaction rolls back, the event is never written.

### Dispatch Path (Mock)

A scheduled poller (`@Scheduled`) reads `PENDING` events, logs them (mock delivery), and marks them `PUBLISHED`. In production, this would push to a message broker or notification service.

### Event Factory

`OutboxEventFactory` creates typed event payloads per domain action, keeping event structure consistent.

---

## Caching Strategy

### What Is Cached

| Key Pattern | Data | TTL |
|---|---|---|
| `opportunities:live:page:{page}` | Paginated LIVE opportunity listings | 60s |
| `opportunity:{id}` | Single opportunity detail | 30s |
| `opportunities:live:count` | Total live opportunity count | 60s |

### What Is NOT Cached

- Allocation/computation results
- Portfolio positions (read from DB with indexes)
- Audit logs
- Investor wallet balances

### Invalidation

Cache entries are evicted on:

- Opportunity state transition (especially → LIVE or → FULLY_SUBSCRIBED)
- Opportunity update by admin/issuer
- New allocation on an opportunity (remaining units changed)

Use Redis `DEL` on pattern keys or explicit key deletion from the service layer post-commit.

### Cache-Aside Pattern

```
read:
  1. Check Redis
  2. On miss → query DB → populate Redis → return

write:
  1. Update DB (transactional)
  2. On commit → evict relevant cache keys
```

---

## Audit Strategy

### Table: `audit_logs`

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| actor_id | BIGINT | User who performed the action |
| actor_role | VARCHAR | ADMIN, INVESTOR, ISSUER, SYSTEM |
| action | VARCHAR | e.g., `OPPORTUNITY_APPROVED`, `FUNDS_LOCKED`, `ALLOCATION_CREATED` |
| entity_type | VARCHAR | e.g., `Opportunity`, `InvestmentOrder` |
| entity_id | BIGINT | |
| before_state | JSON | Nullable snapshot |
| after_state | JSON | Snapshot after action |
| correlation_id | VARCHAR | Request trace ID |
| created_at | TIMESTAMP | Immutable |

### Rules

- **Append-only** — no updates or deletes on audit records
- **Written in same transaction** as the action it records
- **Called via `AuditService.log()`** from other modules' service layer
- Every opportunity status transition produces an audit entry
- Every fund lock/release produces an audit entry

---

## Security: Roles & Permissions

### Roles

| Role | Description |
|---|---|
| `ADMIN` | Platform administrator — review, approve, publish, settle |
| `ISSUER` | Business user — create and manage own opportunities |
| `INVESTOR` | Investor — browse, commit, view portfolio |

### Permission Matrix

| Action | ADMIN | ISSUER | INVESTOR |
|---|---|---|---|
| Login / register | ✓ | ✓ | ✓ |
| Create opportunity | ✓ | ✓ (own) | ✗ |
| Submit for review | ✓ | ✓ (own) | ✗ |
| Approve / reject opportunity | ✓ | ✗ | ✗ |
| Publish opportunity (→ LIVE) | ✓ | ✗ | ✗ |
| Browse live opportunities | ✓ | ✓ | ✓ |
| Place commitment | ✗ | ✗ | ✓ |
| View own portfolio | ✗ | ✗ | ✓ |
| View audit log | ✓ | ✗ | ✗ |
| Settle opportunity | ✓ | ✗ | ✗ |
| View outbox events (debug) | ✓ | ✗ | ✗ |

### JWT Structure

```json
{
  "sub": "user-uuid",
  "role": "INVESTOR",
  "iat": 1700000000,
  "exp": 1700003600
}
```

### Additional Security Measures

- BCrypt password hashing
- Request validation on all input DTOs
- Rate limiting on commitment endpoint (e.g., 10 req/min per investor)
- CORS configuration for known origins
- Correlation ID in `X-Correlation-Id` header for request tracing

---

## Data Model Overview

```
users
  └── investor_profiles (1:1, wallet balance)
  └── issuers (1:1)

issuers
  └── opportunities (1:N)
        └── investment_orders (1:N)
              └── allocations (1:1)
        └── portfolio_positions (1:N)

audit_logs (references any entity)
outbox_events (references any aggregate)
```

### Key Indexes

| Table | Index | Purpose |
|---|---|---|
| `opportunities` | `(status, risk_grade, maturity_date)` | Filtered listing queries |
| `opportunities` | `(issuer_id, status)` | Issuer dashboard |
| `investment_orders` | `(opportunity_id, investor_id, created_at)` | Order history |
| `investment_orders` | `(idempotency_key, investor_id)` UNIQUE | Idempotency enforcement |
| `allocations` | `(investor_id, created_at)` | Portfolio queries |
| `audit_logs` | `(entity_type, entity_id, created_at)` | Entity timeline |
| `outbox_events` | `(status, created_at)` | Poller query |

---

## Error Handling

Standardized error envelope:

```json
{
  "error": {
    "code": "OPPORTUNITY_NOT_LIVE",
    "message": "Opportunity is not accepting commitments",
    "correlationId": "abc-123",
    "timestamp": "2026-06-29T10:00:00Z"
  }
}
```

Domain-specific error codes: `INSUFFICIENT_UNITS`, `INSUFFICIENT_FUNDS`, `DUPLICATE_IDEMPOTENCY_KEY`, `INVALID_STATE_TRANSITION`, `OPPORTUNITY_NOT_LIVE`.
