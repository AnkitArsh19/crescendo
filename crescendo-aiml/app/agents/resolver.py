"""
Stage 2 — App + Action Resolver
=================================
Uses llama-3.3-70b-versatile to map the intent descriptions (from Stage 1)
to exact appKey / triggerKey / actionKey values from the live catalog.

Catalog filtering: before sending to the LLM, we extract keywords from the
intent descriptions and narrow the catalog to the top-N apps by keyword overlap.
This keeps the prompt token budget bounded regardless of how large the catalog grows.
"""

import json
import logging
import os
import re
from typing import List, Optional

from groq import Groq

from app.catalog_sync import app_state
from app.schemas.workflow import IntentResult, ResolvedStep
from app.audit.logger import audit_log

logger = logging.getLogger(__name__)

# How many catalog apps to include in the resolver prompt.
_MAX_CATALOG_APPS = 20

# ---------------------------------------------------------------------------
# Keyword-based catalog filtering
# ---------------------------------------------------------------------------

_STOPWORDS = {
    "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for",
    "of", "with", "by", "from", "as", "is", "it", "its", "that", "this",
    "be", "are", "was", "were", "will", "have", "has", "had", "do", "does",
    "did", "not", "me", "my", "i", "we", "you", "he", "she", "they", "when",
    "new", "get", "set", "send", "message", "any", "all", "up", "so",
}


def _extract_keywords(text: str) -> set[str]:
    """Lowercase words, strip punctuation, remove stopwords."""
    words = re.findall(r"[a-z]+", text.lower())
    return {w for w in words if w not in _STOPWORDS and len(w) > 2}


def _filter_catalog(intent: IntentResult, catalog: list, max_apps: int = _MAX_CATALOG_APPS) -> list:
    """
    Score each catalog app by keyword overlap with the intent descriptions,
    return the top-`max_apps` apps. Falls back to the full catalog (trimmed)
    when no scoring produces a match.
    """
    combined_text = " ".join(
        [intent.trigger_description] + intent.action_descriptions
    )
    keywords = _extract_keywords(combined_text)

    scored: list[tuple[int, dict]] = []
    for app in catalog:
        app_text = " ".join([
            app.get("appKey", ""),
            app.get("name", ""),
        ] + app.get("triggers", []) + app.get("actions", [])).lower()
        score = sum(1 for kw in keywords if kw in app_text)
        scored.append((score, app))

    # Sort by score descending; break ties by appKey alphabetically for determinism
    scored.sort(key=lambda x: (-x[0], x[1].get("appKey", "")))
    top = [app for _, app in scored[:max_apps]]

    logger.debug(
        "Catalog filtered: %d apps → %d selected. Top 5: %s",
        len(catalog),
        len(top),
        [a.get("appKey") for a in top[:5]],
    )
    return top


# ---------------------------------------------------------------------------
# System prompt builder
# ---------------------------------------------------------------------------

def _build_resolver_prompt(intent: IntentResult, filtered_catalog: list) -> str:
    lines = [
        "You are an app and action resolver for an automation platform.",
        "",
        "Given a trigger description and a list of action descriptions, resolve each",
        "to an EXACT appKey and triggerKey/actionKey from the catalog below.",
        "",
        "RULES:",
        "1. Respond with ONLY valid JSON — no markdown fences, no extra text.",
        "2. NEVER invent keys — only use appKey/triggerKey/actionKey values from the catalog.",
        "3. The JSON must conform exactly to this schema:",
        "   {",
        '     "trigger": { "app_key": "string", "action_key": "string", "display_name": "string", "config": {} },',
        '     "actions": [ { "app_key": "string", "action_key": "string", "display_name": "string", "config": {} } ]',
        "   }",
        "4. For the trigger object, action_key must be a triggerKey for that app.",
        "5. For each action object, action_key must be an actionKey for that app.",
        "6. Set config to a reasonable dict based on the description, or {} if uncertain.",
        "",
        "AVAILABLE APPS (use ONLY these appKey / triggerKey / actionKey values):",
    ]

    for app in filtered_catalog:
        lines.append(f"  appKey: \"{app.get('appKey')}\"  ({app.get('name', '')})")
        triggers = app.get("triggers", [])
        actions = app.get("actions", [])
        if triggers:
            lines.append(f"    triggers: {json.dumps(triggers)}")
        if actions:
            lines.append(f"    actions:  {json.dumps(actions)}")

    lines += [
        "",
        "INTENT TO RESOLVE:",
        f"  Trigger: {intent.trigger_description}",
    ]
    for i, ad in enumerate(intent.action_descriptions, 1):
        lines.append(f"  Action {i}: {ad}")

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def resolve_steps(
    intent: IntentResult,
    user_id: str,
    groq_client: Optional[Groq] = None,
) -> tuple[ResolvedStep, list[ResolvedStep]]:
    """
    Stage 2: resolve intent descriptions to exact catalog keys.

    Returns
    -------
    (trigger_step, action_steps) — a tuple of ResolvedStep objects.
    Raises RuntimeError on failure.
    """
    if groq_client is None:
        api_key = os.getenv("GROQ_API_KEY", "")
        if not api_key:
            raise RuntimeError("GROQ_API_KEY is not set")
        groq_client = Groq(api_key=api_key)

    catalog = app_state.get("catalog", [])
    filtered = _filter_catalog(intent, catalog)

    system_prompt = _build_resolver_prompt(intent, filtered)

    try:
        response = groq_client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user",   "content": "Resolve the intent to catalog keys now."},
            ],
            temperature=0.1,
            max_tokens=1024,
            response_format={"type": "json_object"},
        )
    except Exception as exc:
        logger.exception("Stage 2 (resolver) Groq call failed for user %s", user_id)
        raise RuntimeError(f"Resolver LLM error: {exc}") from exc

    usage = response.usage
    audit_log(
        user_id=user_id,
        stage="resolver",
        model="llama-3.3-70b-versatile",
        prompt_tokens=usage.prompt_tokens if usage else 0,
        completion_tokens=usage.completion_tokens if usage else 0,
        validation_passed=True,
        error=None,
    )

    raw = response.choices[0].message.content or ""
    logger.debug("Stage 2 raw JSON: %s", raw)

    try:
        data = json.loads(raw)
        trigger_step = ResolvedStep(**data["trigger"])
        action_steps = [ResolvedStep(**a) for a in data.get("actions", [])]
        return trigger_step, action_steps
    except (json.JSONDecodeError, KeyError, ValueError) as exc:
        logger.error("Stage 2 parse error for user %s: %s\nRaw: %s", user_id, exc, raw)
        raise RuntimeError(f"Resolver returned unparseable JSON: {exc}") from exc
