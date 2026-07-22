"""
Stage 1 — Intent Classifier
============================
Uses llama-3.1-8b-instant (fast / cheap) to parse a natural-language prompt
into structured intent: trigger description, action descriptions, and whether
clarification is needed.

User input is wrapped in <user_request>…</user_request> XML delimiters here
to prevent prompt injection from bleeding into instruction context.
"""

import json
import logging
import os
from typing import Optional

from groq import Groq

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
     "trigger_description":   "string — what event should trigger the workflow",
     "action_descriptions":   ["string — what should happen (one item per action step)"],
     "needs_clarification":   true/false,
     "clarifying_questions":  ["string"] // only if needs_clarification is true
   }
3. Set needs_clarification=true if:
   - The trigger is too vague to identify a specific app or event
   - The desired action is ambiguous (e.g. "notify me" with no channel specified)
   - Multiple conflicting interpretations are equally plausible
4. If needs_clarification=true, populate clarifying_questions with specific,
   concise questions (max 3).
5. If the intent is clear enough, set needs_clarification=false and return
   empty clarifying_questions.
"""


def classify_intent(
    sanitized_prompt: str,
    user_id: str,
    groq_client: Optional[Groq] = None,
) -> IntentResult:
    """
    Stage 1: classify the user intent from the sanitized prompt.

    Parameters
    ----------
    sanitized_prompt:
        User input already wrapped in <user_request>…</user_request> by the sanitizer.
    user_id:
        For audit logging.
    groq_client:
        Optional pre-built Groq client (for testing / reuse). If None, a new
        client is built from GROQ_API_KEY env var.

    Returns
    -------
    IntentResult — always returns a valid object; raises on unrecoverable error.
    """
    if groq_client is None:
        api_key = os.getenv("GROQ_API_KEY", "")
        if not api_key:
            raise RuntimeError("GROQ_API_KEY is not set")
        groq_client = Groq(api_key=api_key)

    try:
        response = groq_client.chat.completions.create(
            model="llama-3.1-8b-instant",
            messages=[
                {"role": "system", "content": _INTENT_SYSTEM},
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
        model="llama-3.1-8b-instant",
        prompt_tokens=usage.prompt_tokens if usage else 0,
        completion_tokens=usage.completion_tokens if usage else 0,
        validation_passed=True,
        error=None,
    )

    raw = response.choices[0].message.content or ""
    logger.debug("Stage 1 raw JSON: %s", raw)

    try:
        data = json.loads(raw)
        return IntentResult(**data)
    except (json.JSONDecodeError, ValueError) as exc:
        logger.error("Stage 1 parse error for user %s: %s\nRaw: %s", user_id, exc, raw)
        raise RuntimeError(f"Intent classifier returned unparseable JSON: {exc}") from exc
