"""
Stage 1 — Intent Classifier  (async)
======================================
Uses llama-3.1-8b-instant to parse a natural-language prompt into structured
intent: trigger description, action descriptions, branching flag, and whether
clarification is needed.

Phase 3 changes:
  - Fully async (uses AsyncGroq)
  - Detects conditional/branching intent (has_branching)
"""

import json
import logging
from typing import Any, Optional

from groq import AsyncGroq

from app.agents.client import get_groq_client
from app.agents.models import FAST_MODEL
from app.schemas.workflow import IntentResult
from app.audit.logger import audit_log

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# System prompt
# ---------------------------------------------------------------------------

_INTENT_SYSTEM = """\
You are an intent classifier for an automation workflow builder.

Your ONLY job is to extract the user's intent from their request and return
structured JSON. Do NOT design the workflow yet — just understand what they want.

RULES:
1. Respond with ONLY valid JSON — no markdown fences, no extra text.
2. The JSON must conform exactly to this schema:
   {
     "trigger_description":  "string — what event should trigger the workflow",
     "action_descriptions":  ["string — what should happen (one item per action step)"],
     "needs_clarification":  true/false,
     "clarifying_questions": ["string"],  // only if needs_clarification=true, else []
     "has_branching":        true/false,
     "branch_description":   "string | null"
   }
3. CRITICAL RULE for needs_clarification:
   You MUST set needs_clarification=true if ANY of the following apply:
   - The trigger does not explicitly name a specific application (e.g. "when a file is uploaded" -> which app? Dropbox? Google Drive? -> MUST clarify).
   - The action does not explicitly name a specific application (e.g. "alert me", "send a message", "do onboarding" -> which app? Slack? Jira? -> MUST clarify).
   - Missing configurations or settings can be skipped, but if the APP NAME itself is missing from the user's prompt, YOU MUST ASK FOR CLARIFICATION.
4. If needs_clarification=true, populate clarifying_questions with ≤3 specific questions.
5. Set has_branching=true when the user describes conditional logic, for example:
   - "If X, do Y, otherwise do Z"
   - "Only run the next step when condition is met"
   - "Depending on whether … send to A or B"
6. When has_branching=true, set branch_description to a plain-English summary of
   the conditional logic (e.g. "If email subject contains 'urgent', post to #alerts,
   else save to Notion").  Otherwise set it to null.
7. Do NOT duplicate actions that are already present in the existing workflow
   unless the user explicitly asks for a duplicate step. If the user is refining
   or extending an existing workflow, only extract the NEW or CHANGED intent.
"""


async def classify_intent(
    sanitized_prompt: str,
    user_id: str,
    groq_client: Optional[AsyncGroq] = None,
    previous_questions: Optional[list] = None,
    previous_spec: Optional[Any] = None,
    previous_intent: Optional[Any] = None,
) -> IntentResult:
    """
    Stage 1: classify the user intent from the sanitized prompt.

    Parameters
    ----------
    sanitized_prompt:
        User input already wrapped in <user_request>…</user_request>.
    user_id:
        For audit logging.
    groq_client:
        Optional pre-built AsyncGroq client.  Falls back to shared singleton.
    previous_questions:
        Clarifying questions asked on the prior turn (if any), for context.
    previous_spec:
        The WorkflowSpec from the prior turn, to prevent accidental duplication.
    previous_intent:
        The IntentResult from the prior turn, as additional context.
    """
    client = groq_client or get_groq_client()

    # Build context block for multi-turn sessions
    context_lines = []
    if previous_questions:
        context_lines.append(
            "PREVIOUS CLARIFYING QUESTIONS ASKED:\n"
            + "\n".join(f"  - {q}" for q in previous_questions)
        )
    if previous_intent:
        trigger = (
            previous_intent.get("trigger_description", "")
            if isinstance(previous_intent, dict)
            else getattr(previous_intent, "trigger_description", "")
        )
        actions = (
            previous_intent.get("action_descriptions", [])
            if isinstance(previous_intent, dict)
            else getattr(previous_intent, "action_descriptions", [])
        )
        if trigger or actions:
            context_lines.append(
                f"PREVIOUSLY UNDERSTOOD INTENT:\n"
                f"  Trigger: {trigger}\n"
                + "\n".join(f"  Action: {a}" for a in (actions or []))
            )
    multi_turn_context = ("\n\n" + "\n\n".join(context_lines)) if context_lines else ""

    logger.debug(
        "Stage 1 system prompt:\n%s",
        _INTENT_SYSTEM,
    )
    logger.debug(
        "Stage 1 user prompt (user=%s):\n%s",
        user_id,
        sanitized_prompt,
    )

    try:
        response = await client.chat.completions.create(
            model=FAST_MODEL,
            messages=[
                {"role": "system", "content": _INTENT_SYSTEM + multi_turn_context},
                {"role": "user",   "content": sanitized_prompt},
            ],
            temperature=0.1,
            max_tokens=512,
            response_format={"type": "json_object"},
        )
    except Exception as exc:
        logger.exception("Stage 1 (intent) Groq call failed for user %s", user_id)
        raise RuntimeError(f"Intent classifier LLM error: {exc}") from exc

    usage = response.usage
    audit_log(
        user_id=user_id,
        stage="intent",
        model=FAST_MODEL,
        prompt_tokens=usage.prompt_tokens if usage else 0,
        completion_tokens=usage.completion_tokens if usage else 0,
        validation_passed=True,
        error=None,
    )

    raw = response.choices[0].message.content or ""
    logger.debug("Stage 1 raw JSON (user=%s):\n%s", user_id, raw)

    try:
        data = json.loads(raw)
        return IntentResult(**data)
    except (json.JSONDecodeError, ValueError) as exc:
        logger.error("Stage 1 parse error for user %s: %s\nRaw: %s", user_id, exc, raw)
        raise RuntimeError(f"Intent classifier returned unparseable JSON: {exc}") from exc
