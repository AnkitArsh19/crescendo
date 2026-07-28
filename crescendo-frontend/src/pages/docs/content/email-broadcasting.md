# Email Marketing & Broadcasting Manual

Crescendo provides a high-performance email delivery engine designed for both automated transactional notifications and high-volume marketing campaigns. This guide explains how to configure custom sender domains, design responsive email templates, and manage broadcast dispatch schedules.

---

## Navigation & Cross-References
* [Audience & Contact Management](/docs/audiences-contacts)
* [Analytics & Insights Dashboards](/docs/analytics-insights)
* [Node.js / TypeScript SDK Reference](/docs/sdk-node)
* [Python SDK Reference](/docs/sdk-python)

---

## 1. Sender Domains & DNS Authentication

To send emails on behalf of your domain and ensure inbox deliverability, you must configure a verified sender domain. Authenticating custom domain records prevents inbox providers (such as Gmail, Yahoo, and Outlook) from flagging your correspondence as spam.

### Step-by-Step Domain Registration
1. Open **Domains** from the dashboard sidebar.
2. Click **Add Domain** and enter your sending hostname (e.g., `mail.yourcompany.com`).
3. Upon creation, Crescendo generates three distinct DNS validation records: SPF, DKIM, and DMARC.

### Required DNS Verification Records

Add the following DNS records inside your domain registrar console (e.g., Cloudflare, GoDaddy, Namecheap, AWS Route53):

| Record Type | Host / Name | Value / Target | Purpose |
| :--- | :--- | :--- | :--- |
| **TXT (SPF)** | `@` or `mail` | `v=spf1 include:mail.crescendo.run ~all` | Authorizes Crescendo mail servers to dispatch email for your domain |
| **TXT (DKIM)** | `crescendo._domainkey` | `k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQ...` | Attaches a cryptographic signature verifying message integrity |
| **TXT (DMARC)** | `_dmarc` | `v=DMARC1; p=none; rua=mailto:dmarc-reports@yourcompany.com` | Instructs inbox providers on how to handle failed authentication mail |

Once DNS propagation completes (typically 5 to 15 minutes), click **Verify Domain**. The domain status will change to **Verified**, enabling outgoing email delivery.

---

## 2. Designing Email Templates

The Template Builder allows you to create responsive HTML layouts for newsletters, marketing announcements, and transactional receipts.

### Creating a Template
1. Navigate to **Templates** and click **Create Template**.
2. Input an internal Template Name, Email Subject Line, and Preheader Preview Text.
3. Use the visual WYSIWYG editor or paste custom HTML code directly.
4. Preview layouts across desktop and mobile viewports before saving.

### Dynamic Variable Placeholders
Templates support dynamic data binding using double curly brace syntax:
* `{{contact.first_name}}` - Replaced by recipient first name.
* `{{contact.email}}` - Recipient email address.
* `{{company.name}}` - Your organization name.
* `{{unsubscribe_url}}` - Mandatory unsubscribe link required for commercial compliance.

---

## 3. Campaigns & Broadcast Dispatching

A campaign dispatches a template message to selected contact audiences.

### Initiating a Campaign
1. Open **Campaigns** and click **New Campaign**.
2. Select your verified Sender Domain and Template.
3. Select your target **Audience Segment**.
4. Send a test dispatch to an internal email address to verify formatting.
5. Choose **Immediate Dispatch** or set a future UTC **Scheduled Dispatch** timestamp.
