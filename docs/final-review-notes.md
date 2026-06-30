# Final Review Notes (Part 11)

Post-implementation review of the Tranche modular monolith. Business behavior is unchanged; this pass focused on consistency, deduplication, and interview readiness.

---

## Strengths of the Project

### Allocation correctness (the core story)
- **Pessimistic lock order**: opportunity `FOR UPDATE` first, then investor wallet — documented in `AllocationEngine` and enforced consistently to prevent deadlocks.
- **Atomic unit decrement** under the opportunity lock; `FULLY_SUBSCRIBED` transition when `remaining_units` hits zero.
- **Partial fills** via pure `AllocationCalculator` (unit-tested separately from concurrency).
- **Idempotency**: `Idempotency-Key` + `UNIQUE (idempotency_key, investor_id)` + replay handling for concurrent duplicates.
- **Fund locking** in the same transaction as allocation, audit, and outbox write.

### Architecture
- Clear **modular monolith** boundaries: auth, issuer, investor, opportunity, allocation, portfolio, audit, notification.
- **State machine** for opportunity lifecycle — invalid transitions fail fast with domain exceptions.
- **Transactional outbox** for notifications; poller with admin trigger.
- **Append-only audit** in the same TX as business actions.

### Production-minded polish (Parts 9–10)
- Unified error envelope, rate limiting on commitments, Redis caching with explicit eviction.
- Correlation IDs in logs and error responses.
- Testcontainers integration tests + HTTP-level auth and race scenarios.
- Demo seed data, `scripts/demo-env.sh`, and `docs/demo-flow.md`.

---

## Known Tradeoffs

| Area | Choice | Tradeoff |
|---|---|---|
| Caching | Redis cache-aside on listings/detail | Eventual consistency (30–60s TTL); evicted on writes |
| Outbox | Mock poller logs to stdout | No real email/SMS/push delivery |
| Payments | Wallet balance + lock columns | Not a real payment gateway |
| Yield | `expectedReturn` = pro-rata face value at allocation | Demo seed sets `unit_price = face/total_units` → zero realized yield unless unit price reflects discount |
| Monolith | Single deployable JAR | Simpler ops; horizontal scale requires sticky sessions or externalized rate-limit/cache |
| JWT | Stateless auth | No server-side revoke list; short TTL + refresh pattern needed in production |
| Tests | Testcontainers | Requires Docker; skipped in environments without it |
| `noRollbackFor` on allocation | Rejected orders persist | Correct for idempotency replay of rejections; TX commits even on business rejection |

---

## What Would Be Next in Production

See **Top 5 production next steps** in the README summary and below. Full backlog:

1. **OpenAPI / SpringDoc** — interactive API docs for consumers
2. **Real notification consumer** — replace mock outbox dispatch
3. **Payment rails** — PSP integration instead of wallet columns
4. **Observability** — Micrometer metrics, distributed tracing (OpenTelemetry), structured log shipping
5. **KYC / compliance** — investor onboarding gates before first commitment
6. **JWT hardening** — refresh tokens, key rotation, optional token denylist
7. **CI pipeline** — GitHub Actions with Docker for Testcontainers on every PR
8. **Secrets management** — Vault / cloud secret store instead of env vars
9. **Read replicas** — for portfolio/audit list queries at scale
10. **API gateway** — WAF, global rate limits, mTLS for B2B issuers

---

## What to Emphasize in Interviews

1. **You built an allocation engine**, not a CRUD app — lead with concurrency and correctness.
2. **Lock ordering** — explain why opportunity before wallet prevents deadlocks.
3. **Idempotency design** — header + DB uniqueness + replay semantics (201 vs 200).
4. **Partial fills** — real behavior demonstrated in the two-investor race demo.
5. **Audit + outbox in the same TX** — compliance and reliable downstream events.
6. **State machine** — controlled lifecycle; admin-only transitions for mature/settle.
7. **Defense in depth** — validation, RBAC, rate limits, safe errors (no stack traces).
8. **Test strategy** — unit calculator + integration races + HTTP auth tests.
9. **Modular monolith** — boundaries without microservice operational cost.
10. **Honest tradeoffs** — mock outbox, wallet not real money, cache TTL staleness.

---

## Refactors Applied (Part 11)

| Change | Purpose |
|---|---|
| `@EvictOpportunityCaches` / `@EvictOpportunityCachesOnCommit` | Deduplicate cache eviction annotations |
| `AuditEntityTypes` | Replace magic audit entity strings |
| `JsonMapConverter` | Shared JSON for audit + outbox |
| `CommitmentMapper`, `IssuerMapper` | Consistent DTO mapping pattern |
| `AllocationEngine.auditRejectedCommitment` | Deduplicate rejection audit blocks |
| `PortfolioService` active count | Derive from loaded list (no extra COUNT query) |
| `OutboxPoller.scheduledPoll` `@Transactional` | Fix scheduled self-invocation TX boundary |
| Controllers → `SecurityUtils.requireCurrentUser()` | Remove duplicated `currentUser()` |
| `V5__outbox_created_at_index.sql` | Support admin outbox list ordering |
| `AbstractIntegrationTest.prepareLiveOpportunity()` | Shared test fixture |

---

## Production Hardening TODOs (codebase)

- [x] SpringDoc OpenAPI (`/swagger-ui.html`, dev profile)
- [x] GitHub Actions CI with Testcontainers (`.github/workflows/ci.yml`)
- [x] Maven wrapper (`./mvnw`) for reproducible builds
- [ ] Dedicated outbox worker service + retry/DLQ policy
- [ ] Apply `discount_rate` when computing `unit_price` on publish (or validate issuer input)
- [ ] Investor profile read API (wallet exposed only via `/auth/me` today)
- [ ] Flyway repeatable migration for reference data in staging
- [ ] Health indicators for Redis + custom allocation metrics (commits/sec, reject rate)
- [ ] Integration test for rate-limit 429 at HTTP layer

---

## Key Documentation Index

| Doc | Topic |
|---|---|
| [architecture.md](architecture.md) | Layering, modules, locking |
| [allocation-engine.md](allocation-engine.md) | Commitment path deep dive |
| [audit-and-outbox.md](audit-and-outbox.md) | Audit queries, poller |
| [non-functional-design.md](non-functional-design.md) | Errors, cache, rate limits |
| [demo-flow.md](demo-flow.md) | Live demo curl walkthrough |
| [demo-script.md](demo-script.md) | Timed interview UI walkthrough |
| [schema-notes.md](schema-notes.md) | Tables, indexes, migrations |
| [api-contract.md](api-contract.md) | REST sketch |
