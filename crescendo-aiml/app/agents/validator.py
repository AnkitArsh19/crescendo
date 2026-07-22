"""
Stage 4 — Catalog Validator
=============================
Pure Python, zero LLM calls.

Checks every app_key, trigger_key, and action_key in the generated WorkflowSpec
against the live catalog stored in app_state.  Returns a list of specific,
human-readable error messages so they can be fed into a correction prompt if needed.
"""

import logging
from typing import List

from app.catalog_sync import app_state
from app.schemas.workflow import WorkflowSpec

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

def _build_catalog_index(catalog: list) -> dict[str, dict]:
    """
    Build a fast-lookup dict:
        { appKey: { "triggers": set(...), "actions": set(...) } }
    """
    index: dict[str, dict] = {}
    for app in catalog:
        key = app.get("appKey", "")
        if key:
            index[key] = {
                "triggers": set(app.get("triggers", [])),
                "actions":  set(app.get("actions", [])),
            }
    return index


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def validate_workflow(workflow_spec: WorkflowSpec) -> List[str]:
    """
    Validate every app/action key in the spec against the live catalog.

    Returns
    -------
    List[str]
        Empty list  → all keys valid (pass).
        Non-empty   → human-readable error messages describing each problem.
    """
    catalog = app_state.get("catalog", [])
    if not catalog:
        # Catalog hasn't loaded yet — skip validation to avoid false positives
        logger.warning("Catalog is empty; skipping key validation")
        return []

    index = _build_catalog_index(catalog)
    errors: List[str] = []

    # --- Validate trigger ---
    trigger = workflow_spec.trigger
    if trigger.app_key not in index:
        errors.append(
            f"Trigger app_key '{trigger.app_key}' is not in the catalog. "
            f"Available keys: {sorted(index.keys())[:10]} (showing first 10)"
        )
    else:
        if trigger.trigger_key not in index[trigger.app_key]["triggers"]:
            available = sorted(index[trigger.app_key]["triggers"])
            errors.append(
                f"trigger_key '{trigger.trigger_key}' does not exist for app "
                f"'{trigger.app_key}'. Available trigger keys: {available}"
            )

    # --- Validate each action ---
    for i, action in enumerate(workflow_spec.actions):
        if action.app_key not in index:
            errors.append(
                f"Action[{i}] app_key '{action.app_key}' is not in the catalog. "
                f"Available keys: {sorted(index.keys())[:10]} (showing first 10)"
            )
        else:
            if action.action_key not in index[action.app_key]["actions"]:
                available = sorted(index[action.app_key]["actions"])
                errors.append(
                    f"Action[{i}] action_key '{action.action_key}' does not exist for app "
                    f"'{action.app_key}'. Available action keys: {available}"
                )

    if errors:
        logger.warning(
            "Catalog validation failed: %d error(s): %s", len(errors), errors
        )
    else:
        logger.info("Catalog validation passed for workflow '%s'", workflow_spec.workflow_name)

    return errors


def format_correction_context(errors: List[str]) -> str:
    """
    Format validation errors into a correction message for the LLM retry prompt.
    """
    lines = [
        "The previous response contained invalid catalog keys. "
        "You MUST correct the following errors and return only the corrected JSON:",
        "",
    ]
    for i, err in enumerate(errors, 1):
        lines.append(f"  {i}. {err}")
    lines.append("")
    lines.append(
        "Use ONLY exact appKey / triggerKey / actionKey values from the catalog provided. "
        "Do not invent new keys."
    )
    return "\n".join(lines)
