# Reliability, idempotency, and errors

## Idempotency for side effects

Use an `Idempotency-Key` on public operations that create or trigger something, especially when your HTTP client retries after a timeout. Crescendo currently enforces duplicate detection for workflow triggers, template creation, and outbound webhook creation.

```bash
curl --request POST 'https://api.crescendo.run/api/v1/workflows/<workflowId>/trigger' \
  --header "Authorization: Bearer $CRESCENDO_API_KEY" \
  --header 'Content-Type: application/json' \
  --header 'Idempotency-Key: payment-<paymentId>-succeeded' \
  --data '{"paymentId":"<paymentId>","event":"payment.succeeded"}'
```

Use one stable, unique key per logical operation. Do not reuse a key for unrelated requests. A repeated key for the same workflow trigger is rejected with `409 Conflict` for 24 hours; create a new key after the retention period or for a genuinely new event.

## Retry policy

- Retry only transient failures such as connection errors, `429`, and selected `5xx` responses.
- Use exponential backoff with jitter. Do not retry validation (`400`), authentication (`401`), or scope (`403`) errors until the request or credentials are corrected.
- Preserve the same idempotency key during a retry of the same logical operation.
- Respect each API key’s configured requests-per-minute limit.

## Read error responses

The status code is the reliable programmatic signal. The response body contains a human-readable explanation where available.

| Status | Meaning | Next action |
| --- | --- | --- |
| `400` | Invalid path, query, header, or request body | Correct the request against the live schema. |
| `401` | Missing, invalid, expired, or revoked credentials | Check the server-side key and its deployment. |
| `403` | The key lacks the required scope or cannot access that resource | Add the minimal needed scope or use the correct account. |
| `404` | The resource is not present for this account | Verify the identifier and ownership. |
| `409` | Duplicate idempotency key or a conflicting state | Reuse only for an actual retry; otherwise resolve the conflict. |
| `429` | Rate limit reached | Back off and retry later. |
| `5xx` | Unexpected service failure | Retry cautiously with the same idempotency key for supported operations. |

## Pagination and filtering

List endpoints define their own query parameters in the OpenAPI reference. Do not assume that every endpoint supports the same cursor, page-size, or filter names. Use the response and the endpoint-specific schema to determine how to request the next page.
