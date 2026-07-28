# Security Policy & Vulnerability Reporting

The Crescendo engineering team considers platform security, token vault encryption, and safe application execution to be top organizational priorities. This policy outlines our security commitments and how to report potential vulnerabilities responsibly.

## Supported Versions

Only the current active release line published on the `main` branch receives actively monitored security patches and dependency vulnerability remediations.

| Module | Active Release Branch | Security Support State |
| :--- | :--- | :--- |
| **crescendo-backend** | `main` | Fully Supported |
| **crescendo-frontend** | `main` | Fully Supported |
| **crescendo-aiml** | `main` | Fully Supported |

## Reporting a Vulnerability

If you discover a potential security vulnerability, cryptographic weakness, OAuth token storage flaw, or unauthorized access vector within any Crescendo component, please report it following these instructions:

1. **Do Not Open a Public Issue**: Do not report security flaws or potential exploit payloads in public GitHub issues, discussions, or pull requests.
2. **Private Communication Channel**: Submit your advisory privately to repository maintainers via GitHub's built-in private vulnerability reporting tool or direct maintainer communication channels.
3. **Required Information**:
   - A descriptive title and detailed summary of the suspected vulnerability.
   - Specific source code file paths, API endpoints, or third-party connector packages involved.
   - Clear reproduction steps or proof-of-concept scripts demonstrating the risk.
   - Potential system impact assessment (e.g., privilege escalation, token disclosure, query injection).

## Maintainer SLA and Remediation Process

- **Acknowledgment**: Maintainers strive to acknowledge receipt of private vulnerability disclosures within 48 business hours.
- **Triage & Assessment**: Maintainers will confirm reproducibility, assign an internal CVSS rating, and formulate a targeted patch plan.
- **Embargo Period**: We request a 90-day responsible disclosure embargo from the notification date to allow adequate time for developing, verifying, and distributing security updates before public disclosure.
- **Credit**: Respected security researchers adhering to responsible disclosure guidelines will be properly credited in published security advisories upon resolution.
