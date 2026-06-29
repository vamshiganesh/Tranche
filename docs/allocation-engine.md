# Allocation Engine

The allocation engine is the correctness-critical path of Tranche. It accepts investor commitments against **LIVE** opportunities, reserves units and wallet funds atomically, and emits audit and outbox records in the **same database transaction**.

---

## Endpoints

```
POST /api/v1/opportunities/{id}/commitments
Authorization: Bearer <INVESTOR token>
Idempotency-Key: <UUID>
```

Request body:

```json
{
  "unitsRequested": 10,
  "amount": 100000.00
}
```

`amount` must equal `unitsRequested × unitPrice`. The requested amount must meet `minimumLot`.

---

## Transaction Boundaries

All commitment processing runs inside a single `@Transactional` method on `AllocationEngine.allocate()`:

| Step | In transaction? |
|---|---|
| Idempotency lookup | Yes |
| `SELECT ... FOR UPDATE` on opportunity | Yes |
| Fill computation | Yes |
| `SELECT ... FOR UPDATE` on investor profile | Yes |
| Wallet lock (available → locked) | Yes |
| Decrement `remaining_units` | Yes |
| Insert `investment_orders` | Yes |
| Insert `allocations` | Yes |
| Insert `portfolio_positions` | Yes |
| Opportunity status → `FULLY_SUBSCRIBED` | Yes |
| Insert `audit_logs` | Yes |
| Insert `outbox_events` | Yes |
| Redis cache eviction | After commit (Spring `@CacheEvict`) |

If any step fails, the entire transaction rolls back — no partial allocation, no orphan audit row, no outbox event.

Rejected commitments that must be idempotent use `@Transactional(noRollbackFor = BusinessException.class)` so the persisted `REJECTED` order commits before the 422 response is returned.

---

## Idempotency Approach

1. **Client requirement:** `Idempotency-Key` header (UUID) on every commitment request.
2. **Database enforcement:** `UNIQUE (idempotency_key, investor_id)` on `investment_orders`.
3. **Application flow:**
   - Before processing, look up existing order by `(key, investor)`.
   - If found → return stored response (`200 OK` replay).
   - If not found → process allocation and insert order.
   - On concurrent duplicate insert → catch `DataIntegrityViolationException`, reload winner row, return replay.

Rejected orders are also stored with the idempotency key so retries receive the same `422` outcome.

---

## Locking Strategy

### Why pessimistic locking?

Concurrent investors committing to the **same opportunity** must not read stale `remaining_units`. Optimistic retry loops add latency and complexity under high contention; invoice allocation prioritises **correctness over throughput**.

### Lock order (deadlock prevention)

All threads acquire locks in this fixed order:

1. **Opportunity row** — `PESSIMISTIC_WRITE` via `findByIdForUpdate()`
2. **Investor profile row** — `PESSIMISTIC_WRITE` via `findByUserIdForUpdate()`

Different investors on the same opportunity serialize on step 1. The same investor on different opportunities may block on their wallet at step 2 — never in reverse order.

### What each lock protects

| Lock target | Prevents |
|---|---|
| Opportunity `FOR UPDATE` | Two threads allocating more than `remaining_units` |
| Investor profile `FOR UPDATE` | Double-spend of wallet balance |
| `UNIQUE (idempotency_key, investor_id)` | Double-processing the same HTTP retry |
| `CHECK (remaining_units >= 0)` | Over-allocation even if application logic regresses |
| `UNIQUE (investment_order_id)` on allocations | One allocation per order |

### Optimistic `version` columns

`@Version` on `Opportunity` and `InvestorProfile` remains for non-allocation updates (admin edits). The allocation path does **not** rely on optimistic retry.

---

## Partial Fill Logic

After the opportunity row is locked, `AllocationCalculator.computeFill()` runs:

```
unitsToAllocate = min(unitsRequested, remainingUnits)
amountAllocated = unitsToAllocate × unitPrice
```

| Condition | Result |
|---|---|
| `remainingUnits == 0` | `REJECTED` / `INSUFFICIENT_UNITS` |
| `amountAllocated < minimumLot` | `REJECTED` / `BELOW_MINIMUM_LOT` |
| `unitsToAllocate < unitsRequested` | `PARTIAL` — allocate available units |
| `unitsToAllocate == unitsRequested` | `FULL` |

Partial fills still require sufficient wallet balance for the **allocated** amount (not the full request).

When `remaining_units` reaches zero after decrement, `OpportunityStateMachine` transitions `LIVE → FULLY_SUBSCRIBED`.

---

## Race Conditions Prevented

| Race | Mitigation |
|---|---|
| Two investors over-subscribe 100 units | Opportunity `FOR UPDATE` serializes; second thread sees updated `remaining_units` |
| Same investor double-clicks submit | Idempotency unique constraint + pre-check |
| Retry after network timeout | Idempotency replay returns original order |
| Wallet debited twice | Investor profile `FOR UPDATE` + single debit per confirmed order |
| Audit/outbox missing after allocation | Same transaction as business writes |
| Stale listing shows old unit count | Cache evicted on successful allocation |

---

## Fund Locking

Funds move from `wallet_balance` to `locked_balance` (not an external payment gateway in this phase):

```
wallet_balance  -= amountAllocated
locked_balance  += amountAllocated
```

The check `wallet_balance >= amountAllocated` runs while the investor profile row is locked.

---

## Module Layout

```
allocation/
  controller/AllocationController.java
  service/AllocationEngine.java      ← transaction owner
  service/CommitmentService.java     ← idempotency header validation
  domain/AllocationCalculator.java   ← pure fill/yield math
  domain/InvestmentOrder.java
  domain/Allocation.java
audit/service/AuditService.java
notification/service/OutboxWriter.java
portfolio/domain/PortfolioPosition.java   ← created on successful allocation
```

---

## Verification

- **Unit:** `AllocationCalculatorTest` — fill decisions and amount validation
- **Integration:** `ConcurrentCommitmentIntegrationTest` — parallel threads, sum(allocations) ≤ total units
- **Integration:** `IdempotencyIntegrationTest` — duplicate key → single order row
