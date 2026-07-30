# Contacts and suppressions

Contacts are the audience records used by the Email workspace. Suppressions are addresses that must not receive future messages from Crescendo.

## Manage contacts

Open **Email → Contacts** to add, view, edit, or remove a contact. Use a real email address as the contact identity and keep optional fields consistent across your imports and workflows.

When creating contacts through the API, use the Contact endpoints and grant the API key `contact:read` and/or `contact:write` as needed. The live [Audiences and Contacts reference](/docs/api/audiences) shows current request fields instead of relying on a fixed CSV-column convention.

## Use contact data safely

- Store only data you need for the message or workflow.
- Use explicit names for custom properties so a template or workflow can map them consistently.
- Treat contact data as personal data. Limit who can export it and do not include it in public webhook URLs or logs.
- Before importing data, remove duplicate rows and verify that you have the necessary consent.

## Suppressions

Open **Email → Suppressions** to manage addresses that should not receive mail. Suppression can result from an unsubscribe, complaint, bounce, or a manual compliance decision.

Use the public Suppressions API only from a trusted server and with the appropriate `suppression:*` scope. Adding or removing a suppression should be an intentional compliance decision; it is not a retry mechanism for a failed send.
