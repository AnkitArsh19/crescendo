# Security & Connection Setup: BYOK vs. OAuth 2.0

To orchestrate automated workflows across third-party applications, Crescendo provides a secure connection architecture supporting both **Bring Your Own Key (BYOK)** and **OAuth 2.0 Protocol Authorization**.

This guide explains how to acquire required API credentials from major provider developer portals, how credentials are encrypted and stored within Crescendo, and how to configure custom OAuth applications.

---

## Navigation & Cross-References
* [113+ Backend Apps Catalog Index](/docs/apps-catalog-deepdive)
* [Workflow Studio & Canvas Manual](/docs/workflow-canvas)
* [Developer REST API Reference](/docs/public-api)
* [Settings & Workspace Administration](/docs/settings-security)

---

## 1. Bring Your Own Key (BYOK) Setup

BYOK enables organizations to connect services using directly generated secret keys, personal access tokens (PATs), bearer tokens, or API credentials.

### Where to Find API Keys in Popular Developer Dashboards

#### A. OpenAI (ChatGPT / DALL-E)
1. Log in to the **OpenAI Platform Dashboard** (`platform.openai.com`).
2. Navigate to **API Keys** in the left sidebar menu.
3. Click **Create new secret key**.
4. Assign a name (e.g., `Crescendo Production Key`) and select project permissions.
5. Copy the generated secret key (starts with `sk-proj-...`).
6. In Crescendo, open **Connections > Add Connection > OpenAI**, select **API Key**, and paste your secret key.

#### B. Stripe Payments
1. Open the **Stripe Dashboard** (`dashboard.stripe.com`).
2. Toggle between **Test Mode** or **Live Mode** depending on your target environment.
3. Navigate to **Developers > API Keys**.
4. Under **Secret key**, click **Reveal secret key** (starts with `rk_live_...` or `sk_live_...`).
5. Copy the secret key into the Crescendo Stripe connection configuration dialog.

#### C. SendGrid Email
1. Open the **SendGrid Console** (`app.sendgrid.com`).
2. Go to **Settings > API Keys**.
3. Click **Create API Key**, grant **Full Access** or restricted **Mail Send** permissions, and copy the key (starts with `SG....`).

#### D. Personal Access Tokens (GitHub & GitLab)
1. On GitHub, navigate to **Settings > Developer Settings > Personal Access Tokens > Fine-grained tokens**.
2. Generate a new token with repository `contents:read` and `issues:write` permissions.
3. Paste the generated token (`github_pat_...`) into your Crescendo GitHub connection setup.

---

## 2. OAuth 2.0 Authorization Mechanics

OAuth 2.0 is an industry-standard delegation protocol that allows Crescendo to perform actions on your behalf without storing your primary account password.

### Managed OAuth vs. Custom App OAuth

Crescendo supports two OAuth deployment models:

```
+-----------------------------------------------------------------------+
|                       OAuth 2.0 Deployment Models                     |
+-----------------------------------+-----------------------------------+
|     Crescendo Managed OAuth       |       Custom App OAuth (BYO)      |
|  (Instant 1-Click Authorization)  |   (Enterprise White-Labeled App)  |
+-----------------------------------+-----------------------------------+
| - Pre-configured verified app     | - You supply Client ID & Secret   |
| - Zero provider developer setup   | - Custom consent screen logo      |
| - Fast onboarding for team users  | - Custom corporate scope control  |
+-----------------------------------+-----------------------------------+
```

#### Option A: Crescendo Managed OAuth (Recommended for Speed)
1. Navigate to **Connections** in Crescendo and click **Add Connection**.
2. Choose your target service (e.g., *Slack*, *Google Drive*, or *Salesforce*).
3. Click **Connect Account**.
4. Your browser redirects to the provider consent window. Click **Allow** or **Authorize**.
5. You are redirected back to Crescendo with an active, authenticated connection link.

#### Option B: Custom App OAuth (Recommended for Enterprise White-Labeling)
If your organization requires your corporate brand logo on authorization screens, or enforces strict internal security policies prohibiting third-party OAuth client IDs:

1. Register a developer application within the target service (e.g., Google Cloud Console or Slack App Directory).
2. Configure the authorized OAuth Redirect URI to:
   ```http
   https://api.crescendo.run/api/v1/connections/oauth/callback
   ```
3. Copy your generated **Client ID** and **Client Secret**.
4. In Crescendo, open the target app connection dialog, select **Use Custom OAuth App**, input your Client ID and Client Secret, and click **Initiate Authorization**.

---

## 3. Provider OAuth Scopes Reference

When setting up custom OAuth applications, ensure your developer app requests the required scope permissions:

| Provider | Required OAuth Scopes | Capability Granted |
| :--- | :--- | :--- |
| **Google (Gmail / Drive)** | `https://mail.google.com/`, `https://www.googleapis.com/auth/drive` | Read inbox, send HTML emails, manage Drive files |
| **Slack** | `chat:write`, `channels:read`, `reactions:read`, `users:read` | Post channel messages, list channels, monitor emoji reactions |
| **GitHub** | `repo`, `workflow`, `admin:repo_hook` | Create issues, merge PRs, dispatch workflow webhooks |
| **Salesforce** | `api`, `refresh_token`, `offline_access` | Query CRM objects, update leads, execute SOQL |
| **Microsoft 365** | `Mail.Send`, `Calendars.ReadWrite`, `User.Read` | Send emails via Outlook, update Teams calendars |

---

## 4. Cryptographic Storage & AES-256 Encryption

Security of customer credentials is a core architectural requirement:

### Encryption at Rest
* Every API key, secret token, and OAuth refresh token is encrypted using **AES-256-GCM** authenticated encryption prior to database persistence.
* Random 96-bit Initialization Vectors (IV) and authentication tags are generated per secret, preventing rainbow table attacks.

### Runtime Secret Injection
* Decryption keys are loaded into memory exclusively from environment variables (`CRESCENDO_ENCRYPTION_KEY`) during backend server boot.
* Credentials are never returned in frontend API responses or written to execution log outputs. When an integration step runs, worker threads decrypt the key in-memory, execute the HTTP network request, and instantly clear plaintext secrets from memory stack frames.

### Automated Refresh Token Rotation
OAuth access tokens expire periodically (typically every 3600 seconds). Crescendo features a background token renewal engine:
1. Worker threads evaluate token expiration timestamps prior to step execution.
2. If a token is within 5 minutes of expiration, an out-of-band refresh request is dispatched using the encrypted refresh token.
3. The renewed access token is encrypted back into storage, maintaining continuous automated workflow operation without manual user re-authentication.
