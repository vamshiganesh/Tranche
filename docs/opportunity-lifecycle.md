# Opportunity Lifecycle

This document defines the invoice opportunity state machine implemented in `OpportunityStateMachine` and enforced by `OpportunityService` on every status change.

---

## States

| Status | Meaning | Accepts commitments? |
|---|---|---|
| `DRAFT` | Issuer is editing | No |
| `UNDER_REVIEW` | Awaiting admin review | No |
| `APPROVED` | Admin approved, not yet published | No |
| `LIVE` | Open for investor commitments | Yes (allocation module) |
| `FULLY_SUBSCRIBED` | All units allocated | No |
| `MATURED` | Tenure elapsed / manually matured | No |
| `SETTLED` | Terminal — funds distributed | No |


---

## Allowed Transitions

```
DRAFT ──────────────► UNDER_REVIEW
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
          APPROVED ◄──reject──      (back to DRAFT)
              │
              ▼ publish
            LIVE ──────────► FULLY_SUBSCRIBED (allocation engine, Part 7)
              │                      │
              │ manual/admin         │
              └──────────┬───────────┘
                         ▼
                      MATURED
                         │
                         ▼ settle
                      SETTLED (terminal)
```

### Transition table

| From | To | Trigger | Actor |
|---|---|---|---|
| `DRAFT` | `UNDER_REVIEW` | Submit for review | Issuer (owner) |
| `UNDER_REVIEW` | `APPROVED` | Review approve | Admin |
| `UNDER_REVIEW` | `DRAFT` | Review reject | Admin |
| `APPROVED` | `LIVE` | Publish | Admin |
| `LIVE` | `FULLY_SUBSCRIBED` | All units allocated | Allocation engine (Part 7) |
| `LIVE` | `MATURED` | Manual maturity (demo) | Admin |
| `FULLY_SUBSCRIBED` | `MATURED` | Manual maturity | Admin |
| `MATURED` | `SETTLED` | Settlement complete | Admin |

Any transition **not** listed above is rejected with `INVALID_STATE_TRANSITION` (HTTP 409).

---

## Enforcement

1. **Single authority** — `OpportunityStateMachine.assertTransition(from, to)` is called before every status change. Controllers never set status directly.

2. **Explicit rejection** — Invalid transitions throw `InvalidStateTransitionException` with a clear message: `Cannot transition opportunity from X to Y`.

3. **Edit guard** — Updates to opportunity fields are only allowed in `DRAFT`. Other states return `OPPORTUNITY_NOT_EDITABLE` (HTTP 400).

4. **Ownership** — Issuer actions (submit, update) require the authenticated issuer to own the opportunity. Admins bypass ownership for review/publish/transition.

5. **Side effects on transition**:
   - `APPROVED` / `DRAFT` via review → sets `reviewedAt`, `reviewComment`
   - `LIVE` via publish → sets `publishedAt`, computes `maturityDate = publishedAt + tenureDays`
   - `MATURED` → sets `maturedAt`
   - `SETTLED` → sets `settledAt`

---

## API mapping

| Endpoint | Transition |
|---|---|
| `POST /opportunities/{id}/submit` | `DRAFT → UNDER_REVIEW` |
| `POST /opportunities/{id}/review` (APPROVE) | `UNDER_REVIEW → APPROVED` |
| `POST /opportunities/{id}/review` (REJECT) | `UNDER_REVIEW → DRAFT` |
| `POST /opportunities/{id}/publish` | `APPROVED → LIVE` |
| `POST /opportunities/{id}/transition` (MATURED) | `LIVE/FULLY_SUBSCRIBED → MATURED` |
| `POST /opportunities/{id}/transition` (SETTLED) | `MATURED → SETTLED` |

---

## Business validation (create / update)

Independent of lifecycle, these rules are enforced in `OpportunityService`:

- `minimumLot >= unitPrice` (at least one unit)
- `minimumLot <= totalUnits × unitPrice`
- `faceValue >= totalUnits × unitPrice`
- On create: `remainingUnits = totalUnits`
- On update (DRAFT only): changing `totalUnits` resets `remainingUnits`

---

## Cache invalidation

Redis caches are evicted on any write that affects listing or detail views:

| Cache | Key pattern | TTL | Invalidated when |
|---|---|---|---|
| `opportunity-live-listings` | `riskGrade:page:size:sort` | 60s | Create, update, any status change |
| `opportunity-detail` | `{id}` | 30s | Update, any status change |

Only `status=LIVE` list queries are cached (hot investor browse path).

---

## Future: allocation integration (Part 7)

The allocation engine will call:

```
LIVE → FULLY_SUBSCRIBED  (when remainingUnits reaches 0)
```

This transition is already defined in the state machine but not yet invoked from application code.
