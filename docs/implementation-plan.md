# Tranche — Implementation Plan

Phased build order for the Tranche backend. Each phase produces a demoable increment with clear acceptance criteria.

---

## Overview

```
Phase 0: Project scaffolding & infrastructure
Phase 1: Auth & user management
Phase 2: Opportunity lifecycle
Phase 3: Allocation engine (core)
Phase 4: Portfolio & yield
Phase 5: Audit & notification outbox
Phase 6: Caching, security hardening & polish
```

Estimated total: 6 phases, each building on the previous.

---

## Phase 0: Project Scaffolding & Infrastructure

### Deliverables

- Maven project with Java 21, Spring Boot 3.x
- Docker Compose: MariaDB, Redis
- Flyway migration setup with baseline schema
- Package structure per architecture doc
- Global exception handler, error envelope, correlation ID filter
- Testcontainers base configuration
- `.env.example`, `application.yml` profiles (`local`, `test`)

### Acceptance Criteria

- [ ] `./mvnw spring-boot:run` starts without errors against Docker Compose
- [ ] Flyway runs migrations on startup
- [ ] Health endpoint `GET /actuator/health` returns UP
- [ ] Testcontainers smoke test connects to MariaDB

### Test Strategy

- Single context-load test
- Flyway migration validation test

---

## Phase 1: Auth & User Management

### Deliverables

- `users` table with role enum (ADMIN, ISSUER, INVESTOR)
- `investor_profiles` table (wallet balance, default mock balance for demo)
- `issuers` table (company name, linked to user)
- Registration and login endpoints
- JWT generation and validation
- Spring Security filter chain with role-based access
- Seed data: 1 admin, 1 issuer, 2 investors

### Acceptance Criteria

- [ ] Admin, issuer, and investor can register and login
- [ ] JWT returned on login; required on protected endpoints
- [ ] Role-based access enforced (investor cannot access admin endpoints)
- [ ] Invalid credentials return 401
- [ ] Investor profile created with default wallet balance on registration

### Test Strategy

- Unit tests for JWT service (token generation, validation, expiry)
- Integration tests for register/login flow
- Security tests: unauthorized access returns 403

---

## Phase 2: Opportunity Lifecycle

### Deliverables

- `opportunities` table with all fields (face value, discount rate, tenure, min lot, risk grade, status, remaining units, version)
- State machine in domain layer (State pattern)
- Issuer: create, update (DRAFT only), submit for review
- Admin: approve, reject, publish (→ LIVE)
- Opportunity listing endpoint (paginated, filterable by status/risk grade)
- Opportunity detail endpoint
- Audit log entries on every status transition
- Admin transition endpoint for MATURITY and SETTLEMENT (manual trigger for demo)

### Acceptance Criteria

- [ ] Issuer creates opportunity in DRAFT state
- [ ] Invalid transitions rejected (e.g., DRAFT → LIVE) with clear error
- [ ] Admin can move DRAFT → UNDER_REVIEW → APPROVED → LIVE
- [ ] Only LIVE opportunities appear in investor listing
- [ ] Each transition creates an audit log entry
- [ ] Pagination works on listing endpoint

### Test Strategy

- Unit tests for state machine (all valid/invalid transitions)
- Integration tests for full lifecycle flow
- Repository tests for listing queries with filters

---

## Phase 3: Allocation Engine (Core)

This is the centerpiece phase. Do not rush it.

### Deliverables

- `investment_orders` table with idempotency key
- `allocations` table
- `POST /api/v1/opportunities/{id}/commitments` endpoint
- Idempotency key handling (store + replay)
- Allocation engine service:
  - Pessimistic lock on opportunity
  - Available unit calculation
  - Full fill, partial fill, and rejection logic
  - Fund locking on investor wallet
  - Remaining unit decrement
  - Auto-transition to FULLY_SUBSCRIBED when units exhausted
- Audit entries for allocation and fund lock
- Outbox event on successful allocation

### Acceptance Criteria

- [ ] Investor can commit to a LIVE opportunity
- [ ] Full fill when requested units ≤ available
- [ ] Partial fill when requested units > available (allocates remaining)
- [ ] Rejection when opportunity is not LIVE or zero units remain
- [ ] Concurrent commitments from 2+ investors: no overbooking (verified by test)
- [ ] Idempotency: retry with same key returns original response, no double allocation
- [ ] Investor wallet balance decremented by allocated amount
- [ ] Opportunity transitions to FULLY_SUBSCRIBED when all units allocated
- [ ] Audit log and outbox event written in same transaction

### Test Strategy

- **Unit tests:** Allocation logic (full fill, partial fill, rejection, zero units)
- **Integration tests:** Single commitment happy path
- **Concurrency tests:** 2+ threads committing simultaneously; assert total allocated ≤ total units
- **Idempotency tests:** Duplicate key returns same response, single allocation in DB
- **Transaction tests:** Rollback on fund lock failure leaves no orphan records

### Key Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Race condition overbooking | Pessimistic write lock on opportunity row |
| Partial fill edge case (1 unit left, 2 investors) | Lock serializes; second gets partial or rejection |
| Idempotency key collision across investors | Unique constraint scoped to `(key, investor_id)` |
| Transaction timeout under load | Keep allocation logic lean; no external calls inside transaction |
| Deadlock between opportunity and investor locks | Always lock opportunity first, then investor profile |

---

## Phase 4: Portfolio & Yield

### Deliverables

- `portfolio_positions` table
- Portfolio created/updated on successful allocation
- `GET /api/v1/portfolio` endpoint for authenticated investor
- Yield calculation (Strategy pattern): expected return based on discount rate and tenure
- Realized yield field (populated on settlement)
- Maturity date computed from opportunity tenure

### Acceptance Criteria

- [ ] After allocation, portfolio position exists for investor
- [ ] Portfolio shows: invested amount, expected return, maturity date
- [ ] Multiple allocations aggregate correctly in portfolio view
- [ ] Portfolio only visible to owning investor
- [ ] After settlement, realized yield is populated

### Test Strategy

- Unit tests for yield calculation strategy
- Integration tests: commit → check portfolio
- Test multiple allocations on different opportunities

---

## Phase 5: Audit & Notification Outbox

### Deliverables

- `audit_logs` table (if not already created in Phase 2)
- `outbox_events` table
- `GET /api/v1/audit` endpoint (admin, paginated, filterable by entity)
- `GET /api/v1/audit/{entityType}/{entityId}` — entity timeline
- Outbox poller (`@Scheduled`) — reads PENDING, logs payload (mock), marks PUBLISHED
- Outbox events for: INVESTMENT_SUCCESSFUL, MATURITY_DUE, SETTLEMENT_COMPLETE
- `GET /api/v1/admin/outbox` — admin debug view of outbox events

### Acceptance Criteria

- [ ] Full demo flow produces complete audit timeline
- [ ] Audit log is append-only (no update/delete endpoints)
- [ ] Outbox events created in same transaction as business action
- [ ] Poller processes PENDING events and marks PUBLISHED
- [ ] Admin can query audit by entity type and ID

### Test Strategy

- Integration test: allocation → verify audit entry + outbox event exist
- Test outbox poller picks up and marks events
- Test audit timeline ordering by created_at

---

## Phase 6: Caching, Security Hardening & Polish

### Deliverables

- Redis cache for live opportunity listings (cache-aside)
- Cache invalidation on state change and allocation
- Rate limiting on commitment endpoint
- Request validation on all DTOs
- API documentation (SpringDoc / OpenAPI)
- README updated with actual run instructions
- Docker Compose full stack (app + MariaDB + Redis)
- N+1 query review and fix (JOIN FETCH where needed)

### Acceptance Criteria

- [ ] Opportunity listing served from cache on second request
- [ ] Cache invalidated after allocation or state change
- [ ] Rate limit returns 429 on excessive commitment requests
- [ ] OpenAPI spec available at `/swagger-ui.html`
- [ ] Full demo flow works end-to-end via Docker Compose
- [ ] No N+1 queries on listing and portfolio endpoints

### Test Strategy

- Cache integration test: verify hit/miss/invalidation
- Rate limit test: exceed threshold, verify 429
- Performance smoke test: listing endpoint < 200ms with cache

---

## Milestone Summary

| Milestone | Phase | Demoable Outcome |
|---|---|---|
| M0: Boot | Phase 0 | App starts, DB connected |
| M1: Identity | Phase 1 | Login as admin/issuer/investor |
| M2: Lifecycle | Phase 2 | Create and publish opportunity |
| M3: Allocation | Phase 3 | Two investors commit concurrently, no overbooking |
| M4: Portfolio | Phase 4 | Investor sees positions and expected return |
| M5: Observability | Phase 5 | Full audit timeline + outbox events |
| M6: Production-ready | Phase 6 | Cached, rate-limited, documented |

---

## Test Strategy (Overall)

### Test Pyramid

```
        ┌───────────┐
        │  E2E (few) │  Full demo flow via MockMvc / TestRestTemplate
        ├───────────┤
        │ Integration│  Testcontainers (MariaDB, Redis), Spring context
        ├───────────┤
        │   Unit     │  State machine, allocation logic, yield calc, JWT
        └───────────┘
```

### Critical Test Scenarios

1. **Concurrent allocation** — 5 threads, 100 units, random demand → total allocated = 100
2. **Idempotency replay** — same key twice → one allocation, same response
3. **Partial fill** — 10 units left, request 15 → allocated 10, status PARTIALLY_FILLED
4. **State machine** — all invalid transitions rejected
5. **Transaction rollback** — fund lock failure → no allocation, no audit, no outbox
6. **Full lifecycle** — DRAFT to SETTLED with audit at every step

### CI Pipeline (Future)

```yaml
# Planned: GitHub Actions
- Build + unit tests
- Integration tests with Testcontainers
- Concurrency stress test (allocation)
- Flyway migration check
```

---

## Key Risks & Mitigations

| # | Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|---|
| R1 | Overbooking under concurrency | Critical | Medium | Pessimistic row lock; concurrency integration tests |
| R2 | Idempotency failure on retry | High | Medium | Unique DB constraint; store response body |
| R3 | Lost notification events | Medium | Low | Transactional outbox; event written in same TX |
| R4 | N+1 queries on listing | Medium | High | JOIN FETCH; DTO projections; review in Phase 6 |
| R5 | Stale cache after allocation | Medium | Medium | Explicit cache eviction post-commit |
| R6 | Scope creep into real payments | Low | High | Mock wallet balance; document as future enhancement |
| R7 | State machine bugs | High | Low | Exhaustive unit tests for all transitions |
| R8 | Deadlock on nested locks | Medium | Low | Consistent lock ordering: opportunity → investor |

---

## Definition of Done (Project)

- [ ] All 6 phases complete with acceptance criteria met
- [ ] Concurrent allocation test passes with 0 overbooking
- [ ] Full demo flow executable via Docker Compose
- [ ] API documented via OpenAPI
- [ ] Architecture, implementation plan, and API contract docs accurate and up to date
