# Audit Trail & Transactional Outbox

Tranche treats **audit logs** as the immutable system-of-record for *who did what*, and the **outbox** as the reliable hand-off to notifications. Both are written in the **same database transaction** as the business action that triggers them.

---

## Audit Module

### Purpose

Every meaningful domain action leaves an append-only row in `audit_logs`:

| Category | Actions |
|---|---|
| Opportunity lifecycle | `OPPORTUNITY_CREATED`, `OPPORTUNITY_UPDATED`, `OPPORTUNITY_SUBMITTED`, `OPPORTUNITY_APPROVED`, `OPPORTUNITY_REJECTED`, `OPPORTUNITY_PUBLISHED`, `OPPORTUNITY_FULLY_SUBSCRIBED`, `OPPORTUNITY_MATURED`, `OPPORTUNITY_SETTLED` |
| Money / allocation | `FUNDS_LOCKED`, `ALLOCATION_CREATED`, `COMMITMENT_REJECTED` |
| Portfolio | `PORTFOLIO_POSITION_MATURED`, `PORTFOLIO_POSITION_SETTLED` |

### Design rules

1. **Append-only** — no update or delete APIs; rows are never mutated after insert.
2. **Same transaction** — `AuditService.log()` is called from within `@Transactional` service methods. If the business transaction rolls back, the audit row rolls back with it.
3. **Structured state** — `before_state` and `after_state` are JSON snapshots (status transitions, amounts, etc.).
4. **Correlation** — `correlation_id` copied from `X-Correlation-Id` request header for end-to-end tracing.

### APIs (Admin)

```
GET /api/v1/audit?entityType=Opportunity&entityId=1&page=0&size=50
GET /api/v1/audit/{entityType}/{entityId}
```

The timeline endpoint returns all entries for an entity ordered by `created_at ASC`.

### Write path

```
Controller → Service (@Transactional)
                 ├── business mutation (DB)
                 ├── AuditService.log(...)
                 └── OutboxWriter.write(...)   [when applicable]
             commit
```

---

## Notification Outbox

### Problem solved

Dual-write anti-pattern: updating the database **and** calling an email/SMS API in one request risks inconsistent state (DB committed, notification failed, or vice versa).

### Pattern: transactional outbox

1. Business service inserts an `outbox_events` row with `status = PENDING` in the **same transaction** as the allocation / settlement.
2. A separate **dispatcher** reads pending rows and delivers notifications.
3. On success, the dispatcher marks the row `PUBLISHED`.

If the business transaction rolls back, the outbox row never exists.

### Event types

| Type | Trigger |
|---|---|
| `INVESTMENT_SUCCESSFUL` | Successful allocation (Part 7) |
| `MATURITY_DUE` | Admin transitions opportunity → `MATURED` |
| `SETTLEMENT_COMPLETE` | Admin transitions opportunity → `SETTLED` |

### Mock dispatcher

`OutboxPoller` runs on a schedule (`tranche.outbox.poll-interval-ms`, default 30s) and can be triggered manually:

```
POST /api/v1/admin/outbox/poll
GET  /api/v1/admin/outbox?status=PENDING
```

The poller **logs the payload** (mock transport) and sets `published_at`. In production this would publish to Kafka, SQS, or a notification microservice.

### Configuration

```yaml
tranche:
  outbox:
    polling-enabled: true
    poll-interval-ms: 30000
    batch-size: 50
```

Tests disable scheduled polling via `application-test.yml`.

---

## Portfolio Module

### Purpose

Materialized investor holdings created on each successful allocation. Read APIs power the investor dashboard.

### APIs (Investor)

```
GET /api/v1/portfolio
GET /api/v1/portfolio/positions/{positionId}
```

### N+1 prevention

Repository queries use `JOIN FETCH` on `opportunity` and `allocation` in a single round-trip:

```java
findAllByInvestorIdWithDetails(investorId)
findByIdAndInvestorIdWithDetails(positionId, investorId)
```

### Settlement flow

When admin settles an opportunity:

1. `OpportunityService` transitions status → `SETTLED`
2. `PortfolioService.settlePositionsForOpportunity()` sets each linked position to `SETTLED` and computes **annualized realized yield**
3. Audit + outbox written in the same transaction

---

## End-to-end traceability example

```
1. Investor commits        → ALLOCATION_CREATED audit + INVESTMENT_SUCCESSFUL outbox
2. Admin matures           → OPPORTUNITY_MATURED audit + MATURITY_DUE outbox
3. Admin settles           → OPPORTUNITY_SETTLED audit + SETTLEMENT_COMPLETE outbox
                           → PORTFOLIO_POSITION_SETTLED audit per position
4. Outbox poller           → logs mock delivery, marks PUBLISHED
5. Admin GET /audit/Opportunity/1 → full timeline
```

---

## Module layout

```
portfolio/
  controller/PortfolioController.java
  service/PortfolioService.java
  repository/PortfolioPositionRepository.java   ← JOIN FETCH queries

audit/
  controller/AuditController.java
  service/AuditService.java
  domain/AuditActions.java

notification/
  controller/OutboxAdminController.java
  service/OutboxWriter.java          ← transactional insert
  service/OutboxPoller.java          ← mock dispatcher
```

---

## Verification

- `OutboxPollerTest` — pending → published
- `RealizedYieldCalculatorTest` — yield math
- `IdempotencyIntegrationTest` — audit + outbox after allocation
- Manual: `POST /api/v1/admin/outbox/poll` after a commitment
