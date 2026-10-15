"""
Unit and Regression Tests for Dynamic Catalog Grounding Verification Gate
Tests 29 test cases across 8 categories:
- Fully ambiguous prompts (0 apps stated)
- Partially ambiguous prompts (ambiguous triggers, clear actions)
- Partially ambiguous prompts (clear triggers, ambiguous actions)
- Mixed multi-action prompts (one clear action, one ambiguous action)
- Fully clear single-action prompts
- Fully clear multi-action prompts
- Catalog validation traps (named third-party service not in catalog)
- Hallucination defense (LLM falsely extracting unmentioned vendors)
"""
import pytest
from app.schemas.workflow import IntentResult, ResolvedStep, WorkflowEdge
from app.agents.validator import validate_edges
from app.catalog.catalog_grounding import (
    verify_and_ground_intent,
    is_catalog_member,
    scan_text_for_catalog_brands,
    substring_cross_check,
    get_catalog_brand_map,
)

BENCHMARK_CASES = [
    # 1. Fully Ambiguous (0 apps stated in prompt)
    {"id": "ambig_1", "category": "Fully Ambiguous", "prompt": "Send a message when something happens", "trigger": "something happens", "actions": ["Send a message"], "trigger_app": None, "action_apps": [None], "expected_clarify": True},
    {"id": "ambig_2", "category": "Fully Ambiguous", "prompt": "Notify team on new lead", "trigger": "new lead", "actions": ["Notify team"], "trigger_app": None, "action_apps": [None], "expected_clarify": True},
    {"id": "ambig_3", "category": "Fully Ambiguous", "prompt": "Update database on file upload", "trigger": "file upload", "actions": ["Update database"], "trigger_app": None, "action_apps": [None], "expected_clarify": True},
    {"id": "ambig_4", "category": "Fully Ambiguous", "prompt": "Alert me if error", "trigger": "error", "actions": ["Alert me"], "trigger_app": None, "action_apps": [None], "expected_clarify": True},
    {"id": "ambig_5", "category": "Fully Ambiguous", "prompt": "Do onboarding stuff", "trigger": "new user", "actions": ["Do onboarding stuff"], "trigger_app": None, "action_apps": [None], "expected_clarify": True},
    {"id": "ambig_6", "category": "Fully Ambiguous", "prompt": "Sync new records when created", "trigger": "records created", "actions": ["Sync records"], "trigger_app": None, "action_apps": [None], "expected_clarify": True},

    # 2. Ambiguous Trigger, Clear Action
    {"id": "ambig_trig_1", "category": "Ambiguous Trigger", "prompt": "Send a message to Slack when a lead comes in", "trigger": "when a lead comes in", "actions": ["Send a message to Slack"], "trigger_app": None, "action_apps": ["Slack"], "expected_clarify": True},
    {"id": "ambig_trig_2", "category": "Ambiguous Trigger", "prompt": "Post on Discord when a file is uploaded", "trigger": "when a file is uploaded", "actions": ["Post on Discord"], "trigger_app": None, "action_apps": ["Discord"], "expected_clarify": True},
    {"id": "ambig_trig_3", "category": "Ambiguous Trigger", "prompt": "Create a task in Notion when an event starts", "trigger": "when an event starts", "actions": ["Create a task in Notion"], "trigger_app": None, "action_apps": ["Notion"], "expected_clarify": True},
    {"id": "ambig_trig_4", "category": "Ambiguous Trigger", "prompt": "Send an email via Gmail when an error occurs", "trigger": "when an error occurs", "actions": ["Send an email via Gmail"], "trigger_app": None, "action_apps": ["Gmail"], "expected_clarify": True},
    {"id": "ambig_trig_5", "category": "Ambiguous Trigger", "prompt": "Add a row in Google Sheets when a purchase happens", "trigger": "when a purchase happens", "actions": ["Add a row in Google Sheets"], "trigger_app": None, "action_apps": ["Google Sheets"], "expected_clarify": True},

    # 3. Clear Trigger, Ambiguous Action
    {"id": "ambig_act_1", "category": "Ambiguous Action", "prompt": "When a GitHub commit happens, notify the team", "trigger": "When a GitHub commit happens", "actions": ["notify the team"], "trigger_app": "GitHub", "action_apps": [None], "expected_clarify": True},
    {"id": "ambig_act_2", "category": "Ambiguous Action", "prompt": "When a charge succeeds in Stripe, update the database", "trigger": "When a charge succeeds in Stripe", "actions": ["update the database"], "trigger_app": "Stripe", "action_apps": [None], "expected_clarify": True},
    {"id": "ambig_act_3", "category": "Ambiguous Action", "prompt": "When a Jira ticket is resolved, alert the channel", "trigger": "When a Jira ticket is resolved", "actions": ["alert the channel"], "trigger_app": "Jira", "action_apps": [None], "expected_clarify": True},
    {"id": "ambig_act_4", "category": "Ambiguous Action", "prompt": "When a new row is added in Google Sheets, send a notification", "trigger": "When a new row is added in Google Sheets", "actions": ["send a notification"], "trigger_app": "Google Sheets", "action_apps": [None], "expected_clarify": True},
    {"id": "ambig_act_5", "category": "Ambiguous Action", "prompt": "When a Zendesk ticket is opened, send a message", "trigger": "When a Zendesk ticket is opened", "actions": ["send a message"], "trigger_app": "Zendesk", "action_apps": [None], "expected_clarify": True},

    # 4. Multi-Action Mixed (One Action Clear, One Action Ambiguous)
    {"id": "mixed_act_1", "category": "Mixed Multi-Action", "prompt": "When an issue is opened in GitHub, post to Slack and update the database", "trigger": "When an issue is opened in GitHub", "actions": ["post to Slack", "update the database"], "trigger_app": "GitHub", "action_apps": ["Slack", None], "expected_clarify": True},
    {"id": "mixed_act_2", "category": "Mixed Multi-Action", "prompt": "When a form is submitted in Typeform, send an email via Gmail and log to CRM", "trigger": "When a form is submitted in Typeform", "actions": ["send an email via Gmail", "log to CRM"], "trigger_app": "Typeform", "action_apps": ["Gmail", None], "expected_clarify": True},
    {"id": "mixed_act_3", "category": "Mixed Multi-Action", "prompt": "When a payment succeeds in Stripe, alert on Discord and update spreadsheet", "trigger": "When a payment succeeds in Stripe", "actions": ["alert on Discord", "update spreadsheet"], "trigger_app": "Stripe", "action_apps": ["Discord", None], "expected_clarify": True},
    {"id": "mixed_act_4", "category": "Mixed Multi-Action", "prompt": "When a deal closes in Salesforce, notify team on Slack and update the CRM", "trigger": "When a deal closes in Salesforce", "actions": ["notify team on Slack", "update the CRM"], "trigger_app": "Salesforce", "action_apps": ["Slack", None], "expected_clarify": True},

    # 5. Fully Clear Single Action (All Stated and Grounded)
    {"id": "clear_1", "category": "Fully Clear Single", "prompt": "When an issue is opened in GitHub, send a message in Slack", "trigger": "When an issue is opened in GitHub", "actions": ["send a message in Slack"], "trigger_app": "GitHub", "action_apps": ["Slack"], "expected_clarify": False},
    {"id": "clear_2", "category": "Fully Clear Single", "prompt": "When a new lead is added in Salesforce, create a contact in HubSpot", "trigger": "When a new lead is added in Salesforce", "actions": ["create a contact in HubSpot"], "trigger_app": "Salesforce", "action_apps": ["HubSpot"], "expected_clarify": False},
    {"id": "clear_3", "category": "Fully Clear Single", "prompt": "When a row is added to Google Sheets, send an email via Gmail", "trigger": "When a row is added to Google Sheets", "actions": ["send an email via Gmail"], "trigger_app": "Google Sheets", "action_apps": ["Gmail"], "expected_clarify": False},
    {"id": "clear_4", "category": "Fully Clear Single", "prompt": "When a Jira ticket status changes to Done, post a message in Discord", "trigger": "When a Jira ticket status changes to Done", "actions": ["post a message in Discord"], "trigger_app": "Jira", "action_apps": ["Discord"], "expected_clarify": False},
    {"id": "clear_5", "category": "Fully Clear Single", "prompt": "When a charge succeeds in Stripe, post payment details to Slack", "trigger": "When a charge succeeds in Stripe", "actions": ["post payment details to Slack"], "trigger_app": "Stripe", "action_apps": ["Slack"], "expected_clarify": False},

    # 6. Fully Clear Multi-Action (All Stated and Grounded)
    {"id": "clear_multi_1", "category": "Fully Clear Multi", "prompt": "When a user signs up in Supabase, send a welcome email via Gmail and notify team on Slack", "trigger": "When a user signs up in Supabase", "actions": ["send a welcome email via Gmail", "notify team on Slack"], "trigger_app": "Supabase", "action_apps": ["Gmail", "Slack"], "expected_clarify": False},
    {"id": "clear_multi_2", "category": "Fully Clear Multi", "prompt": "When a commit is pushed to GitHub, post to Discord and add a row to Google Sheets", "trigger": "When a commit is pushed to GitHub", "actions": ["post to Discord", "add a row to Google Sheets"], "trigger_app": "GitHub", "action_apps": ["Discord", "Google Sheets"], "expected_clarify": False},

    # 7. Catalog Validation Trap (Explicit Named App, not in catalog)
    {"id": "trap_1", "category": "Validation Trap", "prompt": "When an alien signal is detected in MarsOrbiterApp, upload coordinate logs to JupiterVault", "trigger": "When an alien signal is detected in MarsOrbiterApp", "actions": ["upload coordinate logs to JupiterVault"], "trigger_app": "MarsOrbiterApp", "action_apps": ["JupiterVault"], "expected_clarify": False},

    # 8. Hallucination Trap (LLM claims Slack/Salesforce when user only said notify/lead)
    {"id": "hallucination_1", "category": "Hallucination Defense", "prompt": "Notify team on new lead", "trigger": "new lead", "actions": ["Notify team"], "trigger_app": "Salesforce", "action_apps": ["Slack"], "expected_clarify": True},
]


@pytest.mark.parametrize("case", BENCHMARK_CASES, ids=[c["id"] for c in BENCHMARK_CASES])
def test_grounding_verification_gate(case):
    """Verify that verify_and_ground_intent correctly flags or clears clarification."""
    intent = IntentResult(
        trigger_description=case["trigger"],
        action_descriptions=case["actions"],
        needs_clarification=False,  # Deliberately False to simulate eager LLM completion
        clarifying_questions=[],
        trigger_app_stated=case["trigger_app"],
        action_apps_stated=case["action_apps"],
    )

    result = verify_and_ground_intent(intent=intent, raw_prompt=case["prompt"])
    assert result.needs_clarification == case["expected_clarify"], (
        f"Failed on case {case['id']}: expected needs_clarification={case['expected_clarify']}, "
        f"got {result.needs_clarification}. Questions: {result.clarifying_questions}"
    )


def test_catalog_membership():
    """Verify dynamic catalog membership lookups."""
    assert is_catalog_member("slack") is True
    assert is_catalog_member("Slack") is True
    assert is_catalog_member("Google Sheets") is True
    assert is_catalog_member("google-sheets") is True
    assert is_catalog_member("salesforce") is True
    # Non-brand generic tokens must NOT be catalog members
    assert is_catalog_member("crm") is False
    assert is_catalog_member("lead") is False
    assert is_catalog_member("database") is False
    assert is_catalog_member("file") is False


def test_hallucination_substring_cross_check():
    """Verify that hallucinated vendor names are rejected if absent from raw prompt."""
    prompt = "Notify team on new lead"
    assert substring_cross_check("Salesforce", prompt) is None
    assert substring_cross_check("Slack", prompt) is None

    prompt_with_slack = "Post a message to Slack on new lead"
    assert substring_cross_check("Slack", prompt_with_slack) == "Slack"


def test_kahns_cycle_detection_valid_dag():
    """Verify that Kahn's algorithm passes valid acyclic DAGs."""
    trigger = ResolvedStep(app_key="github", action_key="new_issue", display_name="GitHub New Issue", client_id="step_0")
    step_1 = ResolvedStep(app_key="logic", action_key="logic:if", display_name="Logic If", client_id="step_1")
    step_2 = ResolvedStep(app_key="slack", action_key="post_message", display_name="Slack Message", client_id="step_2")
    step_3 = ResolvedStep(app_key="gmail", action_key="send_email", display_name="Gmail Send", client_id="step_3")

    edges = [
        WorkflowEdge(source_step_id="step_0", target_step_id="step_1"),
        WorkflowEdge(source_step_id="step_1", target_step_id="step_2", source_handle="true"),
        WorkflowEdge(source_step_id="step_1", target_step_id="step_3", source_handle="false"),
    ]

    errors = validate_edges(trigger, [step_1, step_2, step_3], edges)
    assert errors == [], f"Expected 0 errors on valid DAG, got: {errors}"


def test_kahns_cycle_detection_multi_step_cycle():
    """Verify that Kahn's algorithm catches multi-step circular dependencies (A -> B -> C -> A)."""
    trigger = ResolvedStep(app_key="github", action_key="new_issue", display_name="GitHub New Issue", client_id="step_0")
    step_1 = ResolvedStep(app_key="slack", action_key="post_message", display_name="Slack Message", client_id="step_1")
    step_2 = ResolvedStep(app_key="airtable", action_key="create_record", display_name="Airtable Record", client_id="step_2")
    step_3 = ResolvedStep(app_key="notion", action_key="create_page", display_name="Notion Page", client_id="step_3")

    # Multi-step cycle: step_1 -> step_2 -> step_3 -> step_1
    cyclic_edges = [
        WorkflowEdge(source_step_id="step_0", target_step_id="step_1"),
        WorkflowEdge(source_step_id="step_1", target_step_id="step_2"),
        WorkflowEdge(source_step_id="step_2", target_step_id="step_3"),
        WorkflowEdge(source_step_id="step_3", target_step_id="step_1"),
    ]

    errors = validate_edges(trigger, [step_1, step_2, step_3], cyclic_edges)
    assert any("Cycle detected" in err for err in errors), f"Expected cycle detection error, got: {errors}"

