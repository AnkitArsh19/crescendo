# Audience & Contact Management Manual

Crescendo features a built-in CRM database layer designed to store subscriber profiles, categorize audiences into manageable segments, and enforce automated email hygiene rules.

---

## Navigation & Cross-References
* [Email Marketing & Broadcasting](/docs/email-broadcasting)
* [Analytics & Insights Dashboards](/docs/analytics-insights)
* [Workflow Studio & Canvas](/docs/workflow-canvas)
* [BYOK & OAuth Security](/docs/byok-vs-oauth)

---

## 1. Contact Profiles & Metadata

A Contact represents an individual subscriber profile containing identity data and custom metadata attributes.

### Creating Contacts Manually
1. Open **Audiences** or **Contacts** from the left navigation menu.
2. Click **Add Contact**.
3. Supply the email address, first name, last name, and phone number.
4. Input custom JSON metadata attributes (e.g., `{"plan": "enterprise", "industry": "fintech"}`). Custom properties can be referenced inside email templates or workflow routing logic.

---

## 2. Batch CSV Import Wizard

For subscriber migrations, Crescendo provides an asynchronous CSV import pipeline:

### CSV File Formatting Requirements
Ensure your CSV spreadsheet uses UTF-8 encoding and includes standard header columns:
* Required Column: `email`
* Optional Columns: `first_name`, `last_name`, `phone`, `tags`

### Import Execution Steps
1. Go to **Audiences** and click **Import CSV**.
2. Select your `.csv` file.
3. Map CSV headers to corresponding Crescendo database fields.
4. Select a duplicate resolution policy (*Update Existing*, *Skip Duplicates*, or *Overwrite*).
5. Click **Start Import**. Progress displays in real time as records are processed.

---

## 3. Dynamic Segmentation & Tags

### Contact Tags
Tags provide fast categorization. Multiple tags can be assigned to a contact (e.g., `vip`, `trial_user`, `q3_lead`). Workflow action steps can apply or remove tags automatically upon task completion.

### Dynamic Audience Segments
Unlike static subscriber lists, Dynamic Segments automatically evaluate contact attributes against rules:
* Example Segment Criteria: `plan equals 'enterprise'` AND `tags contains 'active'`.
* Whenever a workflow or API call updates a contact profile matching these rules, the contact is dynamically added to the segment.

---

## 4. Suppressions & Bounce Hygiene

To protect sender domain reputation, Crescendo automatically manages undeliverable email addresses:

### Suppression Triggers
* **Hard Bounces:** Inbox server permanently rejects delivery.
* **Unsubscriptions:** Recipient clicks the mandatory `{{unsubscribe_url}}` footer link.
* **Spam Complaints:** Recipient flags an email as spam in Gmail, Outlook, or Yahoo Mail.

> [!NOTE]
> Suppressed addresses are automatically blocked from future marketing dispatches across all campaigns.
