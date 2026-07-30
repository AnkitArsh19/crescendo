# Crescendo documentation

Crescendo combines workflow automation with an email delivery workspace. Use this guide to choose the right starting point, build a safe first workflow, and find the public API when you need to automate Crescendo itself.

## Choose your path

- **Build an automation:** start in **Workflows**, connect an app, add a trigger and one or more actions, then test the configuration before activating it.
- **Send and manage email:** open **Email** for API keys, domains, templates, logs, contacts, broadcasts, analytics, and suppressions.
- **Integrate from your server:** create an API key in **Email → API Keys**, choose only the scopes it needs, and use the [Public API](/docs/public-api) or one of the SDKs.
- **Connect an external account:** open **Connections**, choose an app, and complete its OAuth or API-key setup. The configuration form shows the credentials that specific app supports.

## Build your first workflow

1. Go to **Workflows** and select **Create workflow**.
2. Add one trigger. A webhook is the quickest option for testing because you can send it a sample JSON payload.
3. Add an action, select a connected account when required, and complete every required field in the configuration panel.
4. Use values from earlier steps with the variable picker rather than copying data by hand.
5. Save the workflow as a draft. Review the graph and any validation messages.
6. Activate it only after the trigger, connected account, and action configuration are ready. Inspect **History** after the first real run.

> [!TIP]
> Begin with one action and a non-production destination such as a test Slack channel or a test email address. Add branches and side effects only after the first path is successful.

## What the platform validates

The canvas validates graph structure and required configuration fields before saving. Third-party services still enforce their own permissions, resource IDs, rate limits, and data rules at run time. A connection that has expired or a resource selected from another account can therefore still make a run fail. The run detail is the source of truth for the affected step.

## Next steps

- [Workflow Studio](/docs/workflow-canvas) — graph structure, variables, branches, and testing.
- [Connections and apps](/docs/apps-integrations) — OAuth, API keys, and the app catalog.
- [Workflow runs](/docs/workflow-runs) — read a run and recover from a failed step.
- [Public API](/docs/public-api) — server-side API keys, scopes, and the live reference.
