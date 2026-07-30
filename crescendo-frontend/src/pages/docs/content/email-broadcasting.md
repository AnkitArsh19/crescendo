# Email delivery and broadcasts

Crescendo Email provides sender domains, API keys, templates, email logs, contacts, broadcasts, analytics, and suppressions from the **Email** workspace.

## Set up a sender domain

1. Open **Email → Domains** and add the domain or subdomain that will appear in your From address.
2. Copy the DNS records shown by Crescendo into your DNS provider exactly as displayed. The record names and values are generated per domain; do not copy generic SPF, DKIM, or DMARC values from another guide.
3. Wait for DNS to propagate and select **Verify**.
4. Send only after the domain status is verified.

If your DNS provider supports Domain Connect, use the connection flow provided in the domain screen. It can add the provider’s generated records for you, but you should still confirm the resulting records in your DNS provider.

## Create a reusable template

1. Open **Email → Templates** and create a draft.
2. Add a subject and message content in the visual editor or HTML view.
3. Use supported variables deliberately. The editor lists its reserved variables; use the exact spelling it provides.
4. Send a test to an address you control, then publish when the result is correct.

Publishing makes a template available for template-based sends. Editing a published template can require a new review or publishing step, so test after each material change.

## Send with the API

Create an API key in **Email → API Keys** with `email:send`. Keep the key on your server.

```javascript
const { crescendo } = require('@crescendo/email');

const client = crescendo({ apiKey: process.env.CRESCENDO_API_KEY });
await client.emails.send({
  from: 'updates@your-verified-domain.example',
  to: 'recipient@example.com',
  subject: 'Account update',
  htmlBody: '<p>Your account has been updated.</p>',
  textBody: 'Your account has been updated.',
  emailType: 'TRANSACTIONAL'
});
```

See the [Emails API reference](/docs/api/emails) for the complete live request schema and examples in every supported language.

## Contacts, broadcasts, and compliance

Use **Contacts** to maintain your audience and **Broadcasts** to prepare a campaign. Confirm that recipients have the appropriate consent before sending marketing content. Crescendo’s suppression list prevents future sends to addresses that have opted out or otherwise must not receive messages; do not work around it by sending from another workflow or provider.
