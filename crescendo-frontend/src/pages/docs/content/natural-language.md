# AI Builder & Natural Language Automation

Building complex integration logic by manually placing nodes and tracing connecting wires across extensive canvases can sometimes be time-consuming. Crescendo addresses this by incorporating a powerful **AI Builder** that synthesizes conversational natural language text descriptions directly into fully wired, executable workflow graphs.

## How the AI Builder Operates

Powered by the Crescendo intelligent integration registry and deep LLM inference engines, the AI Builder possesses exhaustive structural awareness of every native trigger, action, configuration parameter, and schema across our 114+ supported SaaS applications. When you submit an instruction text prompt, the engine performs three synchronous automation steps:
1. **Intent Parsing & Module Matching:** Identifies the primary triggering event and maps secondary subsequent tasks to appropriate third-party app integration modules.
2. **Graph Scaffold Topology Creation:** Connects sequential node handles and constructs conditional routing branches (such as If/Else or Switch statements) where logical decisions occur.
3. **Dynamic Parameter Mapping:** Automatically generates variable binding expressions (such as `{{steps.trigger.email}}` or `{{steps.1.issue_id}}`), connecting data output variables from preceding steps directly into corresponding input field boxes of downstream action nodes.

## Example Natural Language Prompts

You can input instructions ranging from elementary notifications to complex multi-branch routing logic:
* *"When a GitHub pull request is labeled 'ready-for-review', post an alert in the #engineering Slack channel and create a tracking task in Jira."*
* *"Every morning at 8:00 AM, fetch daily sales aggregates from Stripe and append the summary totals into our Accounting Google Sheet."*
* *"Listen for incoming customer support webhooks. If the customer_tier parameter equals 'enterprise', dispatch an urgent escalation SMS via Twilio; otherwise, deliver a standard welcome notification using Gmail."*

## Step-by-Step AI Graph Generation

1. Open the **Workflows** studio console from your dashboard interface and select **Generate with AI**.
2. A modal dialog will appear. Enter a clear, conversational description of your intended automation sequence into the text prompt field and click **Generate Graph**.
3. **Review the Synthesized Studio Canvas:** Within seconds, the AI engine constructs a complete visual graph on your studio canvas, showing all configured node triggers, action sequence blocks, and interconnected routing wires.
4. **Authorize Connected Credentials:** If the synthesized graph contains third-party application modules requiring account authentication (such as Slack, GitHub, or Google Workspace), an orange **Action Required** indicator badge will display on the affected nodes. Click each marked node to open its configuration panel and select your authenticated enterprise account connection from the dropdown menu.
5. **Test & Activate:** Execute an automated simulation run using sample test payloads. Once verified, toggle the workflow status from **Draft** to **Active** to begin handling production executions.

> [!WARNING]
> While the AI Builder creates structurally sound and accurately wired workflow graphs, specialized custom database formatting or non-standard custom variable transformations may benefit from manual verification and fine-tuning inside individual node configuration side panels before active deployment.
