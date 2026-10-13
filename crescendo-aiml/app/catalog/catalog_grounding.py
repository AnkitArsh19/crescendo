"""
Dynamic Catalog Grounding & Verification Gate
=============================================
Zero-hardcoding grounding engine for Crescendo's Workflow Builder.

Guarantees:
1. Zero Hardcoding: No static domain dictionaries (crm, lead, database, etc.).
   All brand recognition is dynamically driven by the live 114+ catalog
   (app_state["catalog"] with full_catalog_114.json fallback).
2. Defense in Depth: Combines LLM brand extraction with deterministic
   substring cross-checking against the raw user prompt.
3. Eager-Inference Override: Overrides LLM's RLHF helpfulness bias by enforcing
   that any step lacking a grounded, user-stated application forces
   needs_clarification=True.
4. Multi-Turn Continuity: Respects already-grounded steps from previous turns.
"""

import json
import logging
import re
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple

from app.catalog_sync import app_state
from app.schemas.workflow import IntentResult

logger = logging.getLogger(__name__)

# Fallback catalog path
_CATALOG_FALLBACK_PATH = Path(__file__).resolve().parent / "full_catalog_114.json"

# Common functional/generic English nouns that must never be mistaken for specific brands
_GENERIC_NON_BRAND_TOKENS: Set[str] = {
    "app", "apps", "tool", "tools", "action", "actions", "trigger", "triggers",
    "workflow", "workflows", "event", "events", "data", "file", "files",
    "lead", "leads", "error", "errors", "message", "messages", "notification",
    "notifications", "alert", "alerts", "database", "databases", "db", "table",
    "tables", "row", "rows", "column", "columns", "record", "records",
    "item", "items", "task", "tasks", "form", "forms", "sheet", "sheets",
    "email", "emails", "mail", "inbox", "chat", "user", "users", "team", "teams",
    "customer", "customers", "contact", "contacts", "system", "systems",
    "service", "services", "logic", "code", "http", "webhook", "webhooks",
    "api", "apis", "agent", "agents", "onboarding", "schedule", "time", "date",
    "crm", "cloud", "bucket", "storage", "folder", "document", "doc", "docs",
    "log", "logs", "repo", "repository", "commit", "issue", "ticket", "tickets",
    "something", "anything", "stuff", "info", "information"
}

_cached_catalog_version: Optional[str] = None
_cached_brand_map: Dict[str, str] = {}  # normalized_token -> canonical_app_key


def get_active_catalog() -> List[Dict[str, Any]]:
    """Return live catalog from app_state or fall back to cached 114 catalog."""
    catalog = app_state.get("catalog", [])
    if catalog:
        return catalog

    if _CATALOG_FALLBACK_PATH.exists():
        try:
            with open(_CATALOG_FALLBACK_PATH, "r", encoding="utf-8") as f:
                loaded = json.load(f)
                if isinstance(loaded, list) and loaded:
                    return loaded
        except Exception as exc:
            logger.warning("Could not load fallback catalog: %s", exc)

    return []


def _normalize_token(text: str) -> str:
    """Normalize text to lowercase alphanumeric without symbols."""
    return re.sub(r"[^a-z0-9]", "", text.lower())


def get_catalog_brand_map() -> Dict[str, str]:
    """
    Build a dynamic mapping of normalized brand names/tokens to canonical appKeys.
    Rebuilds only when catalog content or version changes.
    """
    global _cached_catalog_version, _cached_brand_map

    current_version = app_state.get("catalog_version")
    if _cached_brand_map and current_version and current_version == _cached_catalog_version:
        return _cached_brand_map

    catalog = get_active_catalog()
    brand_map: Dict[str, str] = {}

    for app in catalog:
        app_key = app.get("appKey", "")
        name = app.get("name", "")
        if not app_key:
            continue

        # 1. Direct appKey (e.g. "salesforce", "google-sheets")
        brand_map[_normalize_token(app_key)] = app_key

        # 2. Display Name (e.g. "Google Sheets", "Salesforce")
        if name:
            brand_map[_normalize_token(name)] = app_key

        # 3. Hyphen/space variations (e.g. "google sheets" -> "google-sheets")
        key_parts = app_key.replace("-", " ").replace("_", " ").split()
        if len(key_parts) > 1:
            brand_map[_normalize_token(" ".join(key_parts))] = app_key
            # For composite names like "google-sheets", index secondary token if distinctive
            for part in key_parts:
                norm_part = _normalize_token(part)
                if norm_part not in _GENERIC_NON_BRAND_TOKENS and len(norm_part) >= 4:
                    if norm_part not in brand_map:
                        brand_map[norm_part] = app_key

    _cached_brand_map = brand_map
    _cached_catalog_version = current_version
    return _cached_brand_map


def is_catalog_member(candidate: Optional[str]) -> bool:
    """Check if candidate name matches an existing app in the dynamic catalog."""
    if not candidate:
        return False
    norm = _normalize_token(candidate)
    if not norm or norm in _GENERIC_NON_BRAND_TOKENS:
        return False
    return norm in get_catalog_brand_map()


def resolve_catalog_app_key(candidate: Optional[str]) -> Optional[str]:
    """Resolve a candidate brand string to its canonical appKey, or None."""
    if not candidate:
        return None
    norm = _normalize_token(candidate)
    if not norm or norm in _GENERIC_NON_BRAND_TOKENS:
        return None
    return get_catalog_brand_map().get(norm)


def scan_text_for_catalog_brands(text: str) -> List[Tuple[str, str]]:
    """
    Deterministically scan raw text for any catalog brands mentioned as word tokens.
    Returns list of (canonical_app_key, matched_brand_str).
    """
    if not text:
        return []

    brand_map = get_catalog_brand_map()
    found: List[Tuple[str, str]] = []
    seen_keys: Set[str] = set()

    # Word-level tokens and bigrams/trigrams
    words = re.findall(r"[A-Za-z0-9\-_]+", text)
    lower_words = [w.lower() for w in words]

    # Check multi-word windows (up to 3 words, e.g. "google sheets", "microsoft teams")
    for window_size in (3, 2, 1):
        for i in range(len(lower_words) - window_size + 1):
            chunk = " ".join(lower_words[i:i + window_size])
            norm = _normalize_token(chunk)
            if norm in _GENERIC_NON_BRAND_TOKENS:
                continue
            if norm in brand_map:
                app_key = brand_map[norm]
                if app_key not in seen_keys:
                    seen_keys.add(app_key)
                    found.append((app_key, chunk))

    return found


def substring_cross_check(extracted_name: Optional[str], raw_text: str) -> Optional[str]:
    """
    Defense-in-depth: verifies that an LLM-extracted brand name actually
    appears as a substring in the user prompt.
    If the LLM hallucinated a vendor out of thin air, this returns None.
    """
    if not extracted_name or not extracted_name.strip():
        return None

    cleaned = extracted_name.strip().lower()
    norm_extracted = _normalize_token(cleaned)

    # Reject generic words
    if norm_extracted in _GENERIC_NON_BRAND_TOKENS:
        return None

    # Check literal presence in raw prompt
    raw_lower = raw_text.lower()
    if cleaned in raw_lower or norm_extracted in _normalize_token(raw_lower):
        return extracted_name.strip()

    # Token-level partial check (e.g. extracted "Google Sheets" when text had "Sheets")
    tokens = [t for t in re.findall(r"[a-z0-9]+", cleaned) if t not in _GENERIC_NON_BRAND_TOKENS and len(t) >= 3]
    if tokens and all(t in raw_lower for t in tokens):
        return extracted_name.strip()

    logger.warning("Deterministic cross-check caught LLM hallucination: '%s' not found in prompt '%s'", extracted_name, raw_text)
    return None


def verify_and_ground_intent(
    intent: IntentResult,
    raw_prompt: str,
    previous_questions: Optional[List[str]] = None,
    previous_spec: Optional[Any] = None,
    previous_intent: Optional[Any] = None,
) -> IntentResult:
    """
    Dynamic Verification Gate (Pure Python):
    1. Cross-checks LLM extracted brands against the raw prompt (removes hallucinations).
    2. Recovers missed catalog brands present in the prompt.
    3. Verifies catalog grounding for trigger and each action.
    4. Overrides needs_clarification=True if any step is ungrounded.
    5. Dynamically synthesizes missing clarifying questions.
    """
    # 1. Cross-check extracted brands against raw prompt text
    validated_trigger_app = substring_cross_check(intent.trigger_app_stated, raw_prompt)

    validated_action_apps: List[Optional[str]] = []
    raw_action_apps = intent.action_apps_stated or []
    for i, desc in enumerate(intent.action_descriptions):
        candidate = raw_action_apps[i] if i < len(raw_action_apps) else None
        valid_app = substring_cross_check(candidate, raw_prompt)
        validated_action_apps.append(valid_app)

    # 2. Recovery: If LLM extracted null but user literally typed a catalog brand
    if not validated_trigger_app:
        matches = scan_text_for_catalog_brands(intent.trigger_description)
        if matches:
            validated_trigger_app = matches[0][1]

    for i, desc in enumerate(intent.action_descriptions):
        if not validated_action_apps[i]:
            matches = scan_text_for_catalog_brands(desc)
            if matches:
                validated_action_apps[i] = matches[0][1]

    # Update intent with validated apps
    intent.trigger_app_stated = validated_trigger_app
    intent.action_apps_stated = validated_action_apps

    # 3. Check for previous turn continuity
    prior_has_trigger = False
    if previous_intent:
        t_desc = (
            previous_intent.get("trigger_description")
            if isinstance(previous_intent, dict)
            else getattr(previous_intent, "trigger_description", None)
        )
        if t_desc:
            prior_has_trigger = True
    if previous_spec:
        prior_has_trigger = True

    # 4. Determine Grounding Status for Trigger and Actions
    # Trigger is grounded if an app is stated AND matches catalog or user stated a specific entity
    trigger_grounded = False
    if validated_trigger_app:
        if is_catalog_member(validated_trigger_app):
            trigger_grounded = True
        else:
            # If not in catalog, check if it's an unsupported explicit service (e.g. MarsOrbiterApp, Zoho)
            # If the user literally named a specific non-generic entity in the prompt, it's not ambiguous intent.
            norm = _normalize_token(validated_trigger_app)
            if norm not in _GENERIC_NON_BRAND_TOKENS and len(norm) >= 3:
                trigger_grounded = True
    elif prior_has_trigger:
        trigger_grounded = True

    # Actions grounding check
    ungrounded_action_indices: List[int] = []
    for i, desc in enumerate(intent.action_descriptions):
        app_candidate = validated_action_apps[i] if i < len(validated_action_apps) else None
        action_grounded = False
        if app_candidate:
            if is_catalog_member(app_candidate):
                action_grounded = True
            else:
                norm = _normalize_token(app_candidate)
                if norm not in _GENERIC_NON_BRAND_TOKENS and len(norm) >= 3:
                    action_grounded = True
        if not action_grounded:
            ungrounded_action_indices.append(i)

    # 5. Eager-Inference Override
    needs_clarification = (not trigger_grounded) or bool(ungrounded_action_indices)

    if needs_clarification:
        intent.needs_clarification = True
        questions = list(intent.clarifying_questions or [])

        # Synthesize questions if LLM failed to generate them
        if not trigger_grounded and not any("trigger" in q.lower() or "when" in q.lower() for q in questions):
            t_phrase = intent.trigger_description.strip()
            questions.insert(0, f"Which application or service should trigger the workflow for: '{t_phrase}'?")

        for idx in ungrounded_action_indices:
            action_desc = intent.action_descriptions[idx].strip()
            # Check if this action already has a question
            if not any(action_desc.lower() in q.lower() for q in questions):
                questions.append(f"Which application should be used to: '{action_desc}'?")

        intent.clarifying_questions = questions[:3]
    else:
        # If all steps are grounded, clarification is not needed
        intent.needs_clarification = False
        intent.clarifying_questions = []

    return intent
