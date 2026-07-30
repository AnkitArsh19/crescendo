# Email analytics

The Email analytics area summarizes delivery activity for the domains and messages in your workspace. Use it to spot delivery trends and investigate individual results through Email Logs.

## Read the numbers carefully

- **Sent / delivered / failed:** delivery state reported by Crescendo and the configured email provider.
- **Bounces and suppressions:** addresses that should not receive future messages unless the underlying issue is resolved and the address is legitimately removed from suppression.
- **Opens and clicks:** engagement signals, not proof that a person read a message. Mail privacy features and security scanners can affect them.

Compare trends over an appropriate time range rather than drawing conclusions from one message or a small send.

## Investigate delivery issues

1. Open **Email → Email Logs**.
2. Filter for the sending domain, status, or time range that matters.
3. Open the affected message and inspect its delivery state.
4. Check domain verification, sender address, recipient quality, and suppression state before trying another send.

For server-side reporting, the Metrics API is available to API keys with `metrics:read`. Use the [live Metrics reference](/docs/api/metrics) to see current filters and response fields.
