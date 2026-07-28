"""
Workflow Template Library
==========================
Pre-built WorkflowSpec skeletons for the 12 most common automation patterns.

Each template has:
  embedding_text — the canonical natural-language phrase used to build its
                   vector embedding (at startup via Groq embedding API)
  spec           — a WorkflowSpec with correct app_key / action_key / trigger_key
                   and empty configs (configurator fills those in at runtime)

Template fast-path: if the user's prompt is ≥0.92 cosine-similar to any
embedding_text, the intent + resolver LLM calls are skipped entirely.
The configurator, validator, and explainer still run.
"""

from dataclasses import dataclass
from app.schemas.workflow import ActionNode, TriggerNode, WorkflowEdge, WorkflowSpec


@dataclass
class WorkflowTemplate:
    id: str
    embedding_text: str
    spec: WorkflowSpec


TEMPLATES: list[WorkflowTemplate] = [
    WorkflowTemplate(
        id="gmail_to_slack",
        embedding_text="When I receive a new email in Gmail, post it to a Slack channel",
        spec=WorkflowSpec(
            workflow_name="Gmail → Slack notification",
            description="Forward new Gmail emails as Slack messages",
            trigger=TriggerNode(app_key="gmail", trigger_key="gmail:new_email"),
            actions=[ActionNode(app_key="slack", action_key="slack:post_message")],
        ),
    ),
    WorkflowTemplate(
        id="gmail_to_notion",
        embedding_text="Save new Gmail emails to a Notion database",
        spec=WorkflowSpec(
            workflow_name="Gmail → Notion",
            description="Create a Notion page for every new Gmail email",
            trigger=TriggerNode(app_key="gmail", trigger_key="gmail:new_email"),
            actions=[ActionNode(app_key="notion", action_key="notion:create_page")],
        ),
    ),
    WorkflowTemplate(
        id="github_pr_to_slack",
        embedding_text="Notify Slack when a new GitHub pull request is opened",
        spec=WorkflowSpec(
            workflow_name="GitHub PR → Slack",
            description="Post a Slack message when a GitHub pull request is created",
            trigger=TriggerNode(app_key="github", trigger_key="github:new_pull_request"),
            actions=[ActionNode(app_key="slack", action_key="slack:post_message")],
        ),
    ),
    WorkflowTemplate(
        id="stripe_payment_to_slack",
        embedding_text="Send a Slack message when a new Stripe payment is received",
        spec=WorkflowSpec(
            workflow_name="Stripe payment → Slack",
            description="Alert a Slack channel on every new Stripe charge",
            trigger=TriggerNode(app_key="stripe", trigger_key="stripe:new_payment"),
            actions=[ActionNode(app_key="slack", action_key="slack:post_message")],
        ),
    ),
    WorkflowTemplate(
        id="stripe_payment_to_notion",
        embedding_text="Log Stripe payments into a Notion database",
        spec=WorkflowSpec(
            workflow_name="Stripe → Notion log",
            description="Create a Notion page for each new Stripe payment",
            trigger=TriggerNode(app_key="stripe", trigger_key="stripe:new_payment"),
            actions=[ActionNode(app_key="notion", action_key="notion:create_page")],
        ),
    ),
    WorkflowTemplate(
        id="github_issue_to_slack",
        embedding_text="Post to Slack when a new GitHub issue is created",
        spec=WorkflowSpec(
            workflow_name="GitHub Issue → Slack",
            description="Notify a Slack channel when a GitHub issue is opened",
            trigger=TriggerNode(app_key="github", trigger_key="github:new_issue"),
            actions=[ActionNode(app_key="slack", action_key="slack:post_message")],
        ),
    ),
    WorkflowTemplate(
        id="discord_to_slack",
        embedding_text="Mirror Discord messages to a Slack channel",
        spec=WorkflowSpec(
            workflow_name="Discord → Slack mirror",
            description="Post Discord messages to Slack in real time",
            trigger=TriggerNode(app_key="discord", trigger_key="discord:new_message"),
            actions=[ActionNode(app_key="slack", action_key="slack:post_message")],
        ),
    ),
    WorkflowTemplate(
        id="webhook_to_slack",
        embedding_text="Post a Slack message when a webhook is received",
        spec=WorkflowSpec(
            workflow_name="Webhook → Slack",
            description="Send a Slack notification on every incoming webhook event",
            trigger=TriggerNode(app_key="webhook", trigger_key="webhook:received"),
            actions=[ActionNode(app_key="slack", action_key="slack:post_message")],
        ),
    ),
    WorkflowTemplate(
        id="webhook_to_notion",
        embedding_text="Save webhook data to a Notion database",
        spec=WorkflowSpec(
            workflow_name="Webhook → Notion",
            description="Create a Notion page for each incoming webhook event",
            trigger=TriggerNode(app_key="webhook", trigger_key="webhook:received"),
            actions=[ActionNode(app_key="notion", action_key="notion:create_page")],
        ),
    ),
    WorkflowTemplate(
        id="google_sheets_to_slack",
        embedding_text="Notify Slack when a new row is added to Google Sheets",
        spec=WorkflowSpec(
            workflow_name="Google Sheets → Slack",
            description="Post a Slack message when a new Google Sheets row appears",
            trigger=TriggerNode(app_key="googlesheets", trigger_key="googlesheets:new_row"),
            actions=[ActionNode(app_key="slack", action_key="slack:post_message")],
        ),
    ),
    WorkflowTemplate(
        id="gmail_to_discord",
        embedding_text="Send new Gmail emails to a Discord channel",
        spec=WorkflowSpec(
            workflow_name="Gmail → Discord",
            description="Forward Gmail emails as Discord messages",
            trigger=TriggerNode(app_key="gmail", trigger_key="gmail:new_email"),
            actions=[ActionNode(app_key="discord", action_key="discord:send_message")],
        ),
    ),
    WorkflowTemplate(
        id="github_pr_to_notion",
        embedding_text="Log GitHub pull requests in Notion",
        spec=WorkflowSpec(
            workflow_name="GitHub PR → Notion",
            description="Create a Notion page for every new GitHub pull request",
            trigger=TriggerNode(app_key="github", trigger_key="github:new_pull_request"),
            actions=[ActionNode(app_key="notion", action_key="notion:create_page")],
        ),
    ),
]
