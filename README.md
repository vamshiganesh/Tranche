# Tranche

**An Invoice Discounting Order & Allocation System**

Tranche is a fintech-style Java backend where businesses publish invoice investment opportunities, investors place commitments, and the system allocates units fairly under concurrency — with full fund locking, lifecycle control, auditability, and notification events via an outbox.

> **Positioning:** I built an **allocation engine**, not a marketplace clone.

The hard problem is not listing invoices or storing orders. It is ensuring that when many investors commit to the same opportunity at the same time, the system never over-allocates, never double-reserves funds, and always leaves a complete audit trail.

---

## Business Problem

Invoice discounting connects businesses that need early liquidity with investors willing to buy discounted receivables. In a live system:

- Demand often exceeds available units on popular opportunities.
- Multiple investors may submit commitments within milliseconds of each other.
- Partial fills are common when an opportunity is nearly full.
- Every money-affecting action must be traceable for compliance and dispute resolution.

A naive CRUD backend will race, overbook, or lose consistency. Tranche treats **allocation correctness** as the central system concern.

---

## Core Capabilities

| Capability | Description |
|---|---|
| Opportunity publishing | Issuers create invoice opportunities with face value, discount rate, tenure, minimum lot, and risk grade |
| Admin review | Controlled lifecycle from draft through settlement |
| Investor commitment | Idempotent commitment API with partial-fill and over-subscription handling |
| Allocation engine | Atomic unit reservation under concurrent load |
| Fund locking | Investor funds reserved at allocation time |
| Portfolio | Invested amount, expected return, maturity date, realized yield |
| Audit trail | Every status transition and money-affecting action logged |
| Notification outbox | Async event emission for investment, maturity, and settlement |

---

## Modules

Tranche is a **modular monolith** — microservice-style boundaries without separate deployable services.

| Module | Responsibility |
|---|---|
| `auth` | JWT authentication, role-based access control, user identity |
| `issuer` | Business/issuer profiles and issuer-scoped actions |
| `investor` | Investor profiles, wallet balance, fund reservation |
| `opportunity` | Invoice opportunity CRUD, lifecycle state machine, listing |
| `allocation` | Commitment intake, idempotency, unit allocation, partial fills |
| `portfolio` | Position tracking, yield calculation, maturity views |
| `audit` | Immutable audit log for transitions and financial actions |
| `notification` | Transactional outbox for downstream notification delivery |

See [docs/architecture.md](docs/architecture.md) for layering, request flows, and technical design.

---

## Opportunity Lifecycle

Opportunities move through a strict state machine. Invalid transitions are rejected.

```
DRAFT → UNDER_REVIEW → APPROVED → LIVE → FULLY_SUBSCRIBED → MATURED → SETTLED
```

| State | Meaning |
|---|---|
| `DRAFT` | Issuer is editing; not visible to investors |
| `UNDER_REVIEW` | Submitted for admin review |
| `APPROVED` | Admin approved; ready to publish |
| `LIVE` | Open for investor commitments |
| `FULLY_SUBSCRIBED` | All units allocated; no further commitments |
| `MATURED` | Tenure elapsed; awaiting settlement |
| `SETTLED` | Funds distributed; terminal state |

Only `LIVE` opportunities accept commitments. Transitions are audited and enforced in the domain layer.

---

## Concurrency, Idempotency & Auditability

### Concurrency

The allocation path is the critical section. Tranche uses a combination of:

- **Pessimistic locking** on opportunity rows during allocation
- **Optimistic versioning** on entities subject to concurrent updates
- **Transactional boundaries** so allocation, fund lock, and audit write succeed or fail together

Under concurrent commitments, the system guarantees no overbooking and correct partial fills.

### Idempotency

Investor commitment requests carry an `Idempotency-Key` header. Retries with the same key return the original result without re-allocating or re-locking funds.

### Auditability

Every status transition and money-affecting action produces an immutable `audit_logs` record with actor, timestamp, entity reference, before/after state, and correlation ID. The audit trail is append-only.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.x |
| Web | Spring Web |
| Persistence | Spring Data JPA |
| Database | MariaDB |
| Cache | Redis |
| Migrations | Flyway |
| Security | Spring Security + JWT |
| Testing | JUnit 5, Testcontainers |
| Local infra | Docker Compose |

---

## Local Development

### Prerequisites

- Java 21
- Docker & Docker Compose
- Maven 3.9+

### Quick Start (once implemented)

```bash
# Start infrastructure
docker compose up -d

# Run migrations and start the app
./mvnw spring-boot:run

# Run tests (includes Testcontainers integration tests)
./mvnw test
```

### Docker Compose Services (planned)

| Service | Port | Purpose |
|---|---|---|
| MariaDB | 3306 | Primary datastore |
| Redis | 6379 | Opportunity listing cache |
| App | 8080 | Spring Boot API |

Environment variables will be documented in `.env.example` during Phase 1 scaffolding.

---

## Demo Flow

1. Admin logs in and reviews an invoice opportunity.
2. Admin approves and publishes it (`LIVE`).
3. Two investors submit commitments concurrently on the same opportunity.
4. System allocates units correctly — no overbooking, partial fills where needed.
5. Portfolio positions update for both investors.
6. Audit timeline shows every transition and allocation event.

---

## Documentation

| Document | Contents |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Module boundaries, layering, locking, outbox, caching, security |
| [docs/implementation-plan.md](docs/implementation-plan.md) | Phased build order, milestones, acceptance criteria, risks |
| [docs/api-contract.md](docs/api-contract.md) | REST endpoint sketch with request/response examples |

---

## Future Enhancements

- **Real payment gateway integration** — replace mock fund locking with actual payment rails
- **Outbox consumer** — dedicated worker to deliver notifications via email/SMS/push
- **Investor KYC workflow** — identity verification before first commitment
- **Secondary market** — transfer allocated positions between investors
- **Risk scoring service** — automated risk grade assignment
- **Rate limiting & API gateway** — production-grade traffic shaping
- **Observability** — distributed tracing, metrics dashboards, structured logging
- **Event sourcing for allocation** — full event replay for regulatory audit

---

## License

MIT 
