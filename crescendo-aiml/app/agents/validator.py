"""
Stage 4 — Catalog Validator  (extended in Phase 3)
====================================================
Pure Python, zero LLM calls.

Phase 3 additions:
  - _build_catalog_index now handles enriched catalog format (ops are objects)
  - validate_required_config: checks that required configSchema fields are present
  - validate_edges: checks that edge source/target IDs exist, no self-loops,
    and logic:if steps have exactly 'true' and 'false' outgoing edges
"""

import logging
from typing import Any, Dict, List, Set

from app.catalog_sync import app_state
from app.schemas.workflow import ResolvedStep, WorkflowEdge, WorkflowSpec

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

def _build_catalog_index(catalog: list) -> Dict[str, Dict[str, Any]]:
    """
    Build a fast-lookup dict that handles the enriched catalog format:
        { appKey: { "triggers": {key: configSchema[]}, "actions": {key: configSchema[]} } }
    """
    index: Dict[str, Dict[str, Any]] = {}
    for app in catalog:
        key = app.get("appKey", "")
        if not key:
            continue
        trigger_map: Dict[str, list] = {}
        action_map: Dict[str, list] = {}
        for op in app.get("triggers", []):
            if isinstance(op, dict):
                trigger_map[op.get("key", "")] = op.get("configSchema", [])
            elif isinstance(op, str):
                trigger_map[op] = []
        for op in app.get("actions", []):
            if isinstance(op, dict):
                action_map[op.get("key", "")] = op.get("configSchema", [])
            elif isinstance(op, str):
                action_map[op] = []
        index[key] = {"triggers": trigger_map, "actions": action_map}
    return index


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def validate_workflow(workflow_spec: WorkflowSpec) -> List[str]:
    """
    Validate every app/action key in the spec against the live catalog.

    Returns empty list on success, or human-readable error messages.
    """
    catalog = app_state.get("catalog", [])
    if not catalog:
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
            available = sorted(index[trigger.app_key]["triggers"].keys())
            errors.append(
                f"trigger_key '{trigger.trigger_key}' does not exist for app "
                f"'{trigger.app_key}'. Available: {available}"
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
                available = sorted(index[action.app_key]["actions"].keys())
                errors.append(
                    f"Action[{i}] action_key '{action.action_key}' does not exist for app "
                    f"'{action.app_key}'. Available: {available}"
                )

    if errors:
        logger.warning("Catalog validation failed: %d error(s): %s", len(errors), errors)
    else:
        logger.info("Catalog validation passed for workflow '%s'", workflow_spec.workflow_name)
    return errors


def validate_required_config(
    trigger_step: ResolvedStep,
    action_steps: List[ResolvedStep],
) -> List[str]:
    """
    Check that fields marked required=true in the configSchema are present and non-null.
    Looks up schemas from the live catalog — skipped gracefully if catalog not loaded.
    """
    catalog = app_state.get("catalog", [])
    if not catalog:
        return []

    index = _build_catalog_index(catalog)
    errors: List[str] = []

    def check_step(label: str, app_key: str, op_key: str, config: dict, op_type: str) -> None:
        app_entry = index.get(app_key, {})
        schema = app_entry.get(op_type, {}).get(op_key, [])
        for field in schema:
            if not isinstance(field, dict):
                continue
            if field.get("required") and field.get("type") not in ("section", "info"):
                field_key = field.get("key", "")
                value = config.get(field_key)
                if value is None or value == "":
                    errors.append(
                        f"{label}: required field '{field_key}' (label: '{field.get('label', '')}') "
                        f"is missing or null in config."
                    )

    check_step("Trigger", trigger_step.app_key, trigger_step.action_key,
               trigger_step.config, "triggers")
    for i, step in enumerate(action_steps):
        check_step(f"Action[{i}]", step.app_key, step.action_key,
                   step.config, "actions")

    if errors:
        logger.warning("Required-config validation: %d missing field(s)", len(errors))
    return errors


def validate_edges(
    trigger_step: ResolvedStep,
    action_steps: List[ResolvedStep],
    edges: List[WorkflowEdge],
) -> List[str]:
    """
    Validate the edge list:
      - All source/target IDs reference real step client_ids
      - No self-loops
      - logic:if steps have exactly 'true' and 'false' outgoing edges
    """
    if not edges:
        return []

    all_ids: Set[str] = {trigger_step.client_id} | {s.client_id for s in action_steps}
    errors: List[str] = []
    outgoing_handles: Dict[str, List[str]] = {}

    for edge in edges:
        src = edge.source_step_id
        tgt = edge.target_step_id
        if src not in all_ids:
            errors.append(f"Edge source '{src}' not found. Valid IDs: {sorted(all_ids)}")
        if tgt not in all_ids:
            errors.append(f"Edge target '{tgt}' not found. Valid IDs: {sorted(all_ids)}")
        if src == tgt:
            errors.append(f"Self-loop detected on step '{src}'")
        outgoing_handles.setdefault(src, []).append(edge.source_handle or "")

    # Validate logic:if steps have both branch handles
    for step in action_steps:
        if step.action_key == "logic:if":
            handles = set(outgoing_handles.get(step.client_id, []))
            if "true" not in handles or "false" not in handles:
                errors.append(
                    f"logic:if step '{step.client_id}' must have both 'true' and 'false' "
                    f"outgoing edges. Found handles: {sorted(handles)}"
                )

    if errors:
        logger.warning("Edge validation: %d error(s)", len(errors))
    return errors


def format_correction_context(errors: List[str]) -> str:
    """Format validation errors into a correction message for the LLM retry prompt."""
    lines = [
        "The previous response contained invalid catalog keys. "
        "You MUST correct the following errors and return only the corrected JSON:",
        "",
    ]
    for i, err in enumerate(errors, 1):
        lines.append(f"  {i}. {err}")
    lines += [
        "",
        "Use ONLY exact appKey / triggerKey / actionKey values from the catalog provided. "
        "Do not invent new keys.",
    ]
    return "\n".join(lines)
