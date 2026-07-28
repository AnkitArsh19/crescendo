# API Governance & Error Handling

The Crescendo API enforces rigorous enterprise governance rules designed to preserve high infrastructure availability, protect backend integrations from duplicate execution errors, and deliver standardized diagnostic error schemas.

## Request Idempotency

Network disruptions happen. In distributed architectures, an external client server might transmit an HTTP `POST` request that triggers a workflow execution cleanly, yet encounter an unexpected network socket disconnection before receiving our `202 Accepted` confirmation response. To prevent accidentally duplicating workflow operations during subsequent client retry loops, Crescendo fully supports request idempotency verification.

Include a custom `Idempotency-Key` string within your HTTP headers on all state-mutating `POST`, `PUT`, or `PATCH` request invocations:

```bash language-bash
curl -X POST "https://api.crescendo.run/api/v1/public/workflows/wf_12345/execute" \
  -H "Authorization: Bearer re_1234567890abcdef" \
  -H "Idempotency-Key: unique_transaction_hash_887766" \
  -H "Content-Type: application/json" \
  -d '{"triggerData": {"order_id": "ord_54321"}}'
```

> [!IMPORTANT]
> **Strict Cryptographic Conflict Evaluation**
> We securely cache successful API response payloads mapped against your submitted `Idempotency-Key` for a duration of 24 hours. Furthermore, our engine computes a cryptographic SHA-256 hash across your transmitted JSON request body. If a subsequent request reuses a previously processed `Idempotency-Key` while submitting a **different request payload**, our server rejects the attempt and explicitly returns a `409 Conflict` status code. We never silently serve cached response payloads for mismatched data inputs.

## Standardized Error Architecture

Crescendo API endpoints maintain secure application boundaries and never leak internal JVM stack traces to external client callers. All client error responses (`4xx`) and internal server warnings (`5xx`) strictly comply with industry-standard RFC 7807 problem detail structured formatting:

```json language-json
{
  "type": "https://docs.crescendo.run/errors/idempotency-conflict",
  "title": "Idempotency Conflict Detected",
  "status": 409,
  "detail": "The submitted Idempotency-Key has already been processed with an incompatible request body payload.",
  "timestamp": "2026-07-28T10:05:00.000Z",
  "path": "/api/v1/public/workflows/wf_12345/execute"
}
```

## Opaque Cursor Pagination

When querying extensive data collections (such as historical contact audiences, verified domains, or execution run logs), endpoints wrap returned lists inside a clean paginated data envelope:

```json language-json
{
  "data": [
    { "id": "uuid-0001", "name": "mail.example.com", "status": "VERIFIED" },
    { "id": "uuid-0002", "name": "notify.demo.org", "status": "VERIFIED" }
  ],
  "has_more": true,
  "next_cursor": "T2Zmc2V0OjEwMA=="
}
```

To fetch the succeeding sequence page, submit the received `next_cursor` string back to the corresponding API endpoint using the `after` query parameter:

```bash language-bash
curl "https://api.crescendo.run/api/v1/public/domains?limit=100&after=T2Zmc2V0OjEwMA=="
```

> [!NOTE]
> Do not attempt to decode, modify, or engineer custom `next_cursor` values manually. Cursor strings are deliberately opaque to support high-performance database indexing and internal sharding optimizations.
