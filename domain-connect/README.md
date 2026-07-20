# Crescendo Domain Connect

This directory contains the Domain Connect configuration and templates for the Crescendo Email Service.

Domain Connect allows Crescendo users to automatically configure their DNS records (SPF, DKIM, DMARC, etc.) with supported providers (like GoDaddy, IONOS, etc.) without having to manually copy and paste TXT records.

## Files

- `crescendo.run.email.json`: The official Domain Connect template for Crescendo.
- `private_key.pem` (Ignored in Git): The private RSA key used to sign requests. **NEVER COMMIT THIS FILE.**
- `public_key.pem` (Ignored in Git): The public RSA key hosted on our server and DNS.

---

## 1. Hosting the Public Key

Because our template uses a sensitive variable (`%dkim_pub_key%`), Domain Connect requires us to digitally sign our requests to prevent phishing. Providers will verify our signature using our public key.

Our public key must be published in two places:

### A. HTTPS Endpoint
The raw text of `public_key.pem` must be served at:
`https://keys.crescendo.run/crescendo.run`

*(During local development, this is tested by running a static server on the `keys-server` directory and exposing it via a Cloudflare Tunnel).*

### B. DNS TXT Record
For older providers, the key must also be hosted as a DNS TXT record on Cloudflare.
- **Type**: `TXT`
- **Name**: `key1._domainconnect.keys`
- **Content**: `p=<base64_content_of_public_key>`

---

## 2. Template Submission Process

To get the template merged into the central Domain Connect repository:

1. **Host the logo**: Ensure the logo specified in `"logoUrl"` (e.g., `https://app.crescendo.run/logo.svg`) is live and returns a raw SVG/PNG (not an HTML page).
2. **Generate Test Markdown**:
   - Go to the [Domain Connect Online Editor](https://domainconnect.paulonet.eu/dc/free/templateedit).
   - Paste the `crescendo.run.email.json` file.
   - Click **Check template**.
   - Enter dummy variables for `token` and `dkim_pub_key`.
   - Click **Test apply template**.
   - Click **Copy Markdown** and save the link. *(Note: Generate two links: one with a blank Host, and one with Host set to `mail`).*
3. **Open GitHub PR**:
   - Fork `Domain-Connect/templates` on GitHub.
   - Add `crescendo.run.email.json` to the root directory.
   - Open a Pull Request and fill out their exact template, pasting your Markdown test links in the Test Results section.

---

## 3. Implementing the Protocol (Backend)

Once the template is merged, the Spring Boot backend must be updated to implement the **Synchronous Flow**.

When a user clicks "Connect my Domain" in the Crescendo dashboard:
1. The backend constructs the Domain Connect URL containing the user's variables (`v=domainconnect&token=...&dkim_pub_key=...`).
2. The backend uses `private_key.pem` to generate an RSA-SHA256 signature of the exact query string.
3. The signature and key ID are appended to the URL (`&key=key1&sig=<signature>`).
4. The user is redirected to this final, signed URL to complete the setup at their DNS provider.
