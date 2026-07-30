# Build workflows with natural language

The AI builder turns a written request into a workflow draft. It uses the Crescendo app catalog, the connections already available in your workspace, and selected live resource lists where they are available. It is a drafting tool, not a replacement for review.

## Write a useful prompt

Include the trigger, the source data, the destination, and any routing rule. Name the connected accounts and resources when that matters.

```text
When a GitHub pull request is opened in the engineering repository,
post its title and URL to the #reviews Slack channel. If the title starts
with "urgent:", also create a high-priority Linear issue.
```

For scheduled work, include a timezone and schedule. For a webhook, describe the expected fields. For a branch, say what happens on both the matching and non-matching paths.

## Review every draft

1. Confirm the selected trigger and actions match your intent.
2. Select an authenticated connection for every provider node.
3. Check resource fields such as Slack channels, Notion databases, or Google Sheets. If a suggestion does not exist in the selected account, choose the correct option from the dropdown.
4. Inspect variable references and condition rules in the configuration panel.
5. Save the draft and test with a safe destination before activation.

The builder can create if and switch nodes, but it cannot know business-specific data formats, provider permissions, or whether a side effect is appropriate. Those decisions remain yours.

## When to use the canvas directly

Use the canvas for a small edit, a custom HTTP request, precise JSON configuration, or a workflow where a provider’s API details matter. You can also generate a draft and then finish the exact configuration manually.
