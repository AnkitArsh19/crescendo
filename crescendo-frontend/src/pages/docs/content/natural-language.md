# AI Builder: Natural Language Workflows

Building complex logic by dragging and dropping nodes can sometimes be tedious. Crescendo features a powerful **AI Builder** that translates natural language prompts directly into executable workflow graphs.

## How It Works

Powered by the platform's intelligent catalog registry, the AI Builder knows exactly what apps, triggers, and actions are available. It parses your intent and automatically scaffolds the workflow.

### Example Prompts
- *"When a GitHub issue is labeled 'bug', send a message to the #engineering Slack channel and create a Jira ticket."*
- *"Every day at 8 AM, fetch the weather from the OpenWeather API and send me an SMS via Twilio."*
- *"Listen for a webhook payload. If the customer_tier is 'enterprise', send a priority email using Gmail, otherwise send them a standard welcome email."*

## Using the AI Builder

1. On the **Workflows** dashboard, click **Generate with AI**.
2. A prompt modal will appear. Describe your automation clearly. 
3. **Review the Graph:** The AI will generate a visual graph. It automatically selects the correct triggers and actions, and even maps basic data fields between steps.
4. **Authorize Connections:** If the AI selected apps that require authentication (like Slack or GitHub), you will see an "Action Required" badge on those nodes. Click them to connect your account.

> [!WARNING]
> The AI Builder generates structural graphs, but complex data transformations (like parsing arrays into specific database schemas) may require manual fine-tuning in the step configuration panel.
