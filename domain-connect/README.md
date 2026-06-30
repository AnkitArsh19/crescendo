# Domain Connect Integration

This directory contains the necessary configuration for **Domain Connect** — an open-source standard that allows your platform to automatically configure DNS records for your users.

When a user adds their custom domain (e.g. `mail.theircompany.com`) to your platform, Domain Connect allows them to log into their DNS registrar (like GoDaddy, Cloudflare, IONOS) directly from your UI and automatically inject the required SPF, DKIM, and DMARC records with a single click.

## File Included

- `crescendo.run.email.json`: This is the standard JSON template containing all the required DNS rules for the Crescendo email service. 

## Next Steps: Getting Listed

To make the "Connect Automatically" button work across major DNS providers, this JSON template must be submitted to the official Domain Connect repository.

1. Review the `crescendo.run.email.json` file.
2. Ensure you have published your public key at the domain specified in `syncPubKeyDomain` (keys.crescendo.run).
3. Validate the template using the [Domain Connect Online Editor](https://templates.domainconnect.org/).
4. Fork the [Domain-Connect/templates repository](https://github.com/Domain-Connect/templates) on GitHub.
5. Add your JSON file to the root of your forked repository.
6. Open a Pull Request on the official repo using their PR template, pasting your test results from the Online Editor.

Once your Pull Request is merged, registrars worldwide will automatically sync your template and start natively supporting your application!
