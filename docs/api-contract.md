# Tranche — API Contract

Draft REST API specification. No controller code yet — this is the contract to implement against.

**Base URL:** `http://localhost:8080/api/v1`

**Common Headers:**

| Header | Required | Description |
|---|---|---|
| `Authorization` | Protected routes | `Bearer <jwt>` |
| `Idempotency-Key` | Commitment endpoint | UUID v4, unique per investor |
| `X-Correlation-Id` | Optional | Request trace ID; generated if absent |
| `Content-Type` | POST/PUT | `application/json` |

**Common Error Envelope:**

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable description",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-06-29T10:00:00Z"
  }
}
```

---

## Auth

### Register

```
POST /auth/register
```

**Request:**

```json
{
  "email": "investor@example.com",
  "password": "SecurePass123!",
  "role": "INVESTOR",
  "fullName": "Jane Investor"
}
```

`role` values: `INVESTOR`, `ISSUER`. Admin accounts are seeded, not registerable.

**Response `201`:**

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "investor@example.com",
  "role": "INVESTOR",
  "fullName": "Jane Investor",
  "createdAt": "2026-06-29T10:00:00Z"
}
```

---

### Login

```
POST /auth/login
```

**Request:**

```json
{
  "email": "investor@example.com",
  "password": "SecurePass123!"
}
```

**Response `200`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "investor@example.com",
    "role": "INVESTOR",
    "fullName": "Jane Investor"
  }
}
```

---

### Get Current User

```
GET /auth/me
Authorization: Bearer <token>
```

**Response `200`:**

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "investor@example.com",
  "role": "INVESTOR",
  "fullName": "Jane Investor",
  "walletBalance": 500000.00
}
```

`walletBalance` present only for INVESTOR role.

---

## Issuer

### Create Issuer Profile

```
POST /issuers/profile
Authorization: Bearer <token>  (ISSUER role)
```

**Request:**

```json
{
  "companyName": "Acme Corp",
  "registrationNumber": "REG-12345"
}
```

**Response `201`:**

```json
{
  "id": 1,
  "companyName": "Acme Corp",
  "registrationNumber": "REG-12345",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

## Opportunities

### Create Opportunity

```
POST /opportunities
Authorization: Bearer <token>  (ISSUER or ADMIN)
```

**Request:**

```json
{
  "title": "Invoice #INV-2026-001",
  "faceValue": 1000000.00,
  "discountRate": 8.5,
  "tenureDays": 90,
  "minimumLot": 10000.00,
  "riskGrade": "A",
  "totalUnits": 100,
  "unitPrice": 10000.00,
  "description": "Payment receivable from verified buyer"
}
```

**Response `201`:**

```json
{
  "id": 42,
  "title": "Invoice #INV-2026-001",
  "faceValue": 1000000.00,
  "discountRate": 8.5,
  "tenureDays": 90,
  "minimumLot": 10000.00,
  "riskGrade": "A",
  "totalUnits": 100,
  "remainingUnits": 100,
  "unitPrice": 10000.00,
  "status": "DRAFT",
  "issuerId": 1,
  "createdAt": "2026-06-29T10:00:00Z"
}
```

---

### Update Opportunity (DRAFT only)

```
PUT /opportunities/{id}
Authorization: Bearer <token>  (ISSUER owner or ADMIN)
```

**Request:** Same fields as create (partial update supported).

**Response `200`:** Updated opportunity object.

---

### Submit for Review

```
POST /opportunities/{id}/submit
Authorization: Bearer <token>  (ISSUER owner)
```

Transitions: `DRAFT → UNDER_REVIEW`

**Response `200`:**

```json
{
  "id": 42,
  "status": "UNDER_REVIEW",
  "updatedAt": "2026-06-29T10:05:00Z"
}
```

---

### Admin: Review Opportunity

```
POST /opportunities/{id}/review
Authorization: Bearer <token>  (ADMIN)
```

**Request:**

```json
{
  "action": "APPROVE",
  "comment": "Documentation verified"
}
```

`action` values: `APPROVE` (→ APPROVED), `REJECT` (→ DRAFT)

**Response `200`:**

```json
{
  "id": 42,
  "status": "APPROVED",
  "reviewedAt": "2026-06-29T10:10:00Z",
  "reviewComment": "Documentation verified"
}
```

---

### Admin: Publish Opportunity

```
POST /opportunities/{id}/publish
Authorization: Bearer <token>  (ADMIN)
```

Transitions: `APPROVED → LIVE`

**Response `200`:**

```json
{
  "id": 42,
  "status": "LIVE",
  "publishedAt": "2026-06-29T10:15:00Z"
}
```

---

### List Opportunities

```
GET /opportunities?status=LIVE&riskGrade=A&page=0&size=20&sort=maturityDate,asc
Authorization: Bearer <token>
```

**Response `200`:**

```json
{
  "content": [
    {
      "id": 42,
      "title": "Invoice #INV-2026-001",
      "faceValue": 1000000.00,
      "discountRate": 8.5,
      "tenureDays": 90,
      "riskGrade": "A",
      "remainingUnits": 100,
      "unitPrice": 10000.00,
      "status": "LIVE",
      "maturityDate": "2026-09-27T00:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### Get Opportunity Detail

```
GET /opportunities/{id}
Authorization: Bearer <token>
```

**Response `200`:**

```json
{
  "id": 42,
  "title": "Invoice #INV-2026-001",
  "faceValue": 1000000.00,
  "discountRate": 8.5,
  "tenureDays": 90,
  "minimumLot": 10000.00,
  "riskGrade": "A",
  "totalUnits": 100,
  "remainingUnits": 60,
  "unitPrice": 10000.00,
  "status": "LIVE",
  "description": "Payment receivable from verified buyer",
  "issuerId": 1,
  "issuerName": "Acme Corp",
  "maturityDate": "2026-09-27T00:00:00Z",
  "createdAt": "2026-06-29T10:00:00Z",
  "publishedAt": "2026-06-29T10:15:00Z"
}
```

---

### Admin: Transition to Matured / Settled

```
POST /opportunities/{id}/transition
Authorization: Bearer <token>  (ADMIN)
```

**Request:**

```json
{
  "targetStatus": "MATURED"
}
```

Allowed admin transitions: `LIVE → MATURED` (manual demo trigger), `FULLY_SUBSCRIBED → MATURED`, `MATURED → SETTLED`

**Response `200`:**

```json
{
  "id": 42,
  "status": "MATURED",
  "updatedAt": "2026-09-27T00:00:00Z"
}
```

---

## Allocation / Commitments

### Place Commitment

```
POST /opportunities/{id}/commitments
Authorization: Bearer <token>  (INVESTOR)
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

**Request:**

```json
{
  "unitsRequested": 10,
  "amount": 100000.00
}
```

`amount` must equal `unitsRequested × unitPrice`. Server validates against opportunity's `unitPrice` and `minimumLot`.

**Response `201` — Full Fill:**

```json
{
  "orderId": 101,
  "opportunityId": 42,
  "unitsRequested": 10,
  "unitsAllocated": 10,
  "amountRequested": 100000.00,
  "amountAllocated": 100000.00,
  "fillStatus": "FULL",
  "status": "CONFIRMED",
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2026-06-29T11:00:00Z"
}
```

**Response `201` — Partial Fill:**

```json
{
  "orderId": 102,
  "opportunityId": 42,
  "unitsRequested": 10,
  "unitsAllocated": 4,
  "amountRequested": 100000.00,
  "amountAllocated": 40000.00,
  "fillStatus": "PARTIAL",
  "status": "CONFIRMED",
  "idempotencyKey": "660e8400-e29b-41d4-a716-446655440001",
  "createdAt": "2026-06-29T11:00:01Z"
}
```

**Response `200` — Idempotent Replay:**

Same body as original response. HTTP 200 instead of 201.

**Response `422` — Rejected:**

```json
{
  "error": {
    "code": "INSUFFICIENT_UNITS",
    "message": "No units available for allocation",
    "correlationId": "abc-123",
    "timestamp": "2026-06-29T11:00:02Z"
  }
}
```

Other rejection codes: `OPPORTUNITY_NOT_LIVE`, `INSUFFICIENT_FUNDS`, `BELOW_MINIMUM_LOT`

---

### Get Commitment / Order Detail

```
GET /commitments/{orderId}
Authorization: Bearer <token>  (INVESTOR owner or ADMIN)
```

**Response `200`:**

```json
{
  "orderId": 101,
  "opportunityId": 42,
  "opportunityTitle": "Invoice #INV-2026-001",
  "unitsRequested": 10,
  "unitsAllocated": 10,
  "amountAllocated": 100000.00,
  "fillStatus": "FULL",
  "status": "CONFIRMED",
  "allocation": {
    "allocationId": 201,
    "units": 10,
    "amount": 100000.00,
    "discountAmount": 8500.00,
    "expectedReturn": 108500.00,
    "allocatedAt": "2026-06-29T11:00:00Z"
  },
  "createdAt": "2026-06-29T11:00:00Z"
}
```

---

### List Investor Orders

```
GET /commitments?page=0&size=20
Authorization: Bearer <token>  (INVESTOR)
```

**Response `200`:**

```json
{
  "content": [
    {
      "orderId": 101,
      "opportunityId": 42,
      "opportunityTitle": "Invoice #INV-2026-001",
      "unitsAllocated": 10,
      "amountAllocated": 100000.00,
      "fillStatus": "FULL",
      "status": "CONFIRMED",
      "createdAt": "2026-06-29T11:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

## Portfolio

### Get Portfolio

```
GET /portfolio
Authorization: Bearer <token>  (INVESTOR)
```

**Response `200`:**

```json
{
  "summary": {
    "totalInvested": 250000.00,
    "totalExpectedReturn": 271250.00,
    "activePositions": 2,
    "realizedYield": null
  },
  "positions": [
    {
      "positionId": 301,
      "opportunityId": 42,
      "opportunityTitle": "Invoice #INV-2026-001",
      "investedAmount": 100000.00,
      "expectedReturn": 108500.00,
      "discountRate": 8.5,
      "maturityDate": "2026-09-27T00:00:00Z",
      "status": "ACTIVE",
      "realizedYield": null
    },
    {
      "positionId": 302,
      "opportunityId": 43,
      "opportunityTitle": "Invoice #INV-2026-002",
      "investedAmount": 150000.00,
      "expectedReturn": 162750.00,
      "discountRate": 8.5,
      "maturityDate": "2026-10-15T00:00:00Z",
      "status": "ACTIVE",
      "realizedYield": null
    }
  ]
}
```

---

### Get Position Detail

```
GET /portfolio/positions/{positionId}
Authorization: Bearer <token>  (INVESTOR owner)
```

**Response `200`:**

```json
{
  "positionId": 301,
  "opportunityId": 42,
  "opportunityTitle": "Invoice #INV-2026-001",
  "investedAmount": 100000.00,
  "expectedReturn": 108500.00,
  "discountRate": 8.5,
  "tenureDays": 90,
  "maturityDate": "2026-09-27T00:00:00Z",
  "status": "ACTIVE",
  "realizedYield": null,
  "allocationId": 201,
  "allocatedAt": "2026-06-29T11:00:00Z"
}
```

---

## Audit

### List Audit Logs (Admin)

```
GET /audit?entityType=Opportunity&entityId=42&page=0&size=50
Authorization: Bearer <token>  (ADMIN)
```

**Response `200`:**

```json
{
  "content": [
    {
      "id": 1001,
      "actorId": "admin-uuid",
      "actorRole": "ADMIN",
      "action": "OPPORTUNITY_PUBLISHED",
      "entityType": "Opportunity",
      "entityId": 42,
      "beforeState": { "status": "APPROVED" },
      "afterState": { "status": "LIVE" },
      "correlationId": "abc-123",
      "createdAt": "2026-06-29T10:15:00Z"
    },
    {
      "id": 1002,
      "actorId": "investor-uuid",
      "actorRole": "INVESTOR",
      "action": "ALLOCATION_CREATED",
      "entityType": "InvestmentOrder",
      "entityId": 101,
      "beforeState": null,
      "afterState": {
        "unitsAllocated": 10,
        "amountAllocated": 100000.00,
        "fillStatus": "FULL"
      },
      "correlationId": "def-456",
      "createdAt": "2026-06-29T11:00:00Z"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 2,
  "totalPages": 1
}
```

---

### Entity Audit Timeline

```
GET /audit/{entityType}/{entityId}
Authorization: Bearer <token>  (ADMIN)
```

**Response `200`:**

```json
{
  "entityType": "Opportunity",
  "entityId": 42,
  "timeline": [
    {
      "id": 1000,
      "action": "OPPORTUNITY_CREATED",
      "actorRole": "ISSUER",
      "afterState": { "status": "DRAFT" },
      "createdAt": "2026-06-29T10:00:00Z"
    },
    {
      "id": 1001,
      "action": "OPPORTUNITY_PUBLISHED",
      "actorRole": "ADMIN",
      "afterState": { "status": "LIVE" },
      "createdAt": "2026-06-29T10:15:00Z"
    },
    {
      "id": 1002,
      "action": "ALLOCATION_CREATED",
      "actorRole": "INVESTOR",
      "afterState": { "unitsAllocated": 10, "amountAllocated": 100000.00 },
      "createdAt": "2026-06-29T11:00:00Z"
    }
  ]
}
```

---

## Notification / Admin Utilities

### List Outbox Events (Admin)

```
GET /admin/outbox?status=PENDING&page=0&size=20
Authorization: Bearer <token>  (ADMIN)
```

**Response `200`:**

```json
{
  "content": [
    {
      "id": 501,
      "eventType": "INVESTMENT_SUCCESSFUL",
      "aggregateType": "Allocation",
      "aggregateId": 201,
      "payload": {
        "investorId": "investor-uuid",
        "opportunityId": 42,
        "amountAllocated": 100000.00,
        "unitsAllocated": 10
      },
      "status": "PENDING",
      "createdAt": "2026-06-29T11:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### Trigger Outbox Poll (Admin / Dev)

```
POST /admin/outbox/poll
Authorization: Bearer <token>  (ADMIN)
```

Manually triggers the outbox poller for demo purposes.

**Response `200`:**

```json
{
  "processed": 3,
  "published": 3,
  "failed": 0
}
```

---

## Health & Meta

### Health Check

```
GET /actuator/health
```

**Response `200`:**

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

---

## HTTP Status Code Summary

| Code | Usage |
|---|---|
| `200` | Successful read or idempotent replay |
| `201` | Resource created (commitment, opportunity, registration) |
| `400` | Validation error |
| `401` | Missing or invalid JWT |
| `403` | Insufficient role permissions |
| `404` | Resource not found |
| `409` | Conflict (e.g., invalid state transition) |
| `422` | Business rule violation (insufficient units/funds) |
| `429` | Rate limit exceeded |

---

## Fill Status Values

| Value | Meaning |
|---|---|
| `FULL` | All requested units allocated |
| `PARTIAL` | Fewer units allocated than requested |
| `REJECTED` | Zero units allocated |

## Opportunity Status Values

`DRAFT` | `UNDER_REVIEW` | `APPROVED` | `LIVE` | `FULLY_SUBSCRIBED` | `MATURED` | `SETTLED`

## Outbox Event Types

`INVESTMENT_SUCCESSFUL` | `MATURITY_DUE` | `SETTLEMENT_COMPLETE`
