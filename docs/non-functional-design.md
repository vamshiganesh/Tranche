# Non-Functional Design

Production-readiness controls for the Tranche API: consistent errors, validation, rate limiting, caching, observability, and authorization boundaries.

---

## API Error Envelope

All errors return the same JSON shape:

```json
{
  "error": {
    "code": "INSUFFICIENT_FUNDS",
    "message": "Insufficient wallet balance",
    "correlationId": "abc-123",
    "timestamp": "2026-06-29T10:00:00Z"
  }
}
```

| HTTP | Typical `code` values |
|---|---|
| 400 | `VALIDATION_ERROR`, `MISSING_IDEMPOTENCY_KEY`, `OPPORTUNITY_NOT_EDITABLE` |
| 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS` |
| 403 | `FORBIDDEN`, `INVALID_ROLE` |
| 404 | `NOT_FOUND` |
| 409 | `CONFLICT`, `INVALID_STATE_TRANSITION` |
| 422 | `INSUFFICIENT_UNITS`, `INSUFFICIENT_FUNDS`, `OPPORTUNITY_NOT_LIVE` |
| 429 | `RATE_LIMIT_EXCEEDED` |
| 500 | `INTERNAL_ERROR` |

**Safety rules:**
- `GlobalExceptionHandler` never exposes stack traces or internal exception messages to clients.
- Unexpected errors are logged server-side with `correlationId`; clients receive a generic message.
- `BusinessException` messages are curated domain text (safe to return).

Success responses return domain DTOs directly (no wrapper) — consistent per resource module.

---

## Validation

Request DTOs use Jakarta Bean Validation (`@Valid` on controller parameters):

| DTO | Key constraints |
|---|---|
| `RegisterRequest` | email format, password 8–100 chars |
| `LoginRequest` | email, password |
| `CreateOpportunityRequest` | positive amounts, discount 0–100%, title length |
| `CommitmentRequest` | units 1–100,000; amount > 0 |
| Path variables | `@Min(1)` on opportunity IDs (`@Validated` controllers) |

Malformed JSON → `400 VALIDATION_ERROR`. Missing `Idempotency-Key` → `400 MISSING_IDEMPOTENCY_KEY`.

---

## Rate Limiting

**Endpoint:** `POST /api/v1/opportunities/{id}/commitments`

| Setting | Default |
|---|---|
| Window | 1 minute |
| Max requests | 10 per investor per window |
| Backend | Redis `INCR` + TTL (fixed window) |
| Key | `rate:commitment:{investorUserId}` |

`CommitmentRateLimitFilter` runs **after** JWT authentication so limits are per authenticated investor, not per IP.

Exceeded limit → `429` with `RATE_LIMIT_EXCEEDED`.

Configuration:

```yaml
tranche:
  rate-limit:
    commitment:
      enabled: true
      requests-per-window: 10
      window: 1m
```

Tests use a higher limit (`100`) to avoid interfering with integration tests.

---

## Caching (Redis)

| Cache | Key | TTL | Content |
|---|---|---|---|
| `opportunity-live-listings` | status + risk + page + sort | 60s | `PageResponse` of LIVE listings |
| `opportunity-detail` | opportunity id | 30s | `OpportunityResponse` |

**Cache-aside** in `OpportunityService` (`@Cacheable`).

**Eviction (`@CacheEvict`)** on:
- Opportunity create / update / lifecycle transition
- Successful allocation (`AllocationEngine`) — listing + detail for that opportunity

**Not cached:** commitments, portfolio, audit, wallet balances, admin outbox.

Serializer uses a dedicated Redis `ObjectMapper` with Java time support and default typing for records (see `RedisSerializationConfig`).

---

## Pagination

| Setting | Value |
|---|---|
| Default page size | 20 |
| Maximum page size | 100 (enforced by `PageableHandlerMethodArgumentResolverCustomizer`) |

Applies to opportunity list, audit list, and outbox admin list. Requests with `size > 100` are capped silently to 100.

---

## Observability

### Correlation ID

- Client may send `X-Correlation-Id`.
- If absent, server generates a UUID.
- Echoed on response header and included in every error payload.
- Stored in MDC (`correlationId`) for log lines.
- Propagated to `audit_logs` and `investment_orders`.

### Request logging

`RequestLoggingFilter` logs at INFO:

```
GET /api/v1/opportunities 200 45ms
```

Health checks (`/actuator/health`) are excluded. Log pattern includes `[%X{correlationId}]`.

---

## Authorization Boundaries

| Path pattern | Role |
|---|---|
| `/api/v1/auth/register`, `/login` | Public |
| `/actuator/health`, `/info` | Public |
| `/api/v1/opportunities/{id}/commitments` | `INVESTOR` |
| `/api/v1/portfolio/**` | `INVESTOR` |
| `/api/v1/opportunities` POST/PUT/submit | `ISSUER` or `ADMIN` |
| `/api/v1/opportunities/*/review`, `/publish`, `/transition` | `ADMIN` |
| `/api/v1/audit/**` | `ADMIN` |
| `/api/v1/admin/outbox/**` | `ADMIN` |

Enforced via `@PreAuthorize` on controllers + Spring Security JWT filter. Service layer performs additional ownership checks (e.g. issuer owns opportunity).

---

## N+1 Query Prevention

| Read path | Strategy |
|---|---|
| Portfolio list/detail | `JOIN FETCH` opportunity + allocation |
| Opportunity detail | `@EntityGraph(issuer)` on `findWithIssuerById` |
| Audit list | `@EntityGraph(actor)` on filtered query |
| Audit timeline | `LEFT JOIN FETCH actor` |
| Opportunity list (summary) | No issuer join needed — summary DTO omits issuer |

---

## Module Map

```
common/
  exception/GlobalExceptionHandler.java   ← unified errors
  ratelimit/CommitmentRateLimitFilter.java
  ratelimit/RateLimitService.java
  util/CorrelationIdFilter.java
  util/RequestLoggingFilter.java
  config/WebConfig.java                   ← pagination caps
  config/CacheConfig.java
  config/RateLimitProperties.java
```

---

## Verification

- `GlobalExceptionHandlerTest` — safe error mapping, correlation ID propagation
- `RateLimitServiceTest` — Redis counter logic
- Manual: exceed 10 commitments/min → `429 RATE_LIMIT_EXCEEDED`
