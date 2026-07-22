"""
Audit Logger
=============
Appends one JSON record per LLM call to audit.jsonl in the project root.

Each record contains:
  - timestamp    : ISO-8601 UTC
  - user_id      : str
  - stage        : "intent" | "resolver" | "configurator" | "explainer" | "correction"
  - model        : Groq model name used
  - prompt_tokens: int
  - completion_tokens: int
  - validation_passed: bool (True if catalog validation passed, N/A for non-validator stages)
  - error        : str | null

Phase 1: JSONL on disk (zero extra dependencies).
Phase 2: write to a DB table (swap the _write function below).
"""

import json
import logging
import os
from datetime import datetime, timezone
from typing import Optional

logger = logging.getLogger(__name__)

# Default path — can be overridden via AUDIT_LOG_PATH env var
_DEFAULT_LOG_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(__file__))),
    "audit.jsonl",
)


def _get_log_path() -> str:
    return os.getenv("AUDIT_LOG_PATH", _DEFAULT_LOG_PATH)


def audit_log(
    user_id: str,
    stage: str,
    model: str,
    prompt_tokens: int,
    completion_tokens: int,
    validation_passed: bool,
    error: Optional[str],
) -> None:
    """
    Write one audit record. Never raises — log errors are swallowed so
    a logging failure never breaks the main pipeline.
    """
    record = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "user_id": user_id,
        "stage": stage,
        "model": model,
        "prompt_tokens": prompt_tokens,
        "completion_tokens": completion_tokens,
        "total_tokens": prompt_tokens + completion_tokens,
        "validation_passed": validation_passed,
        "error": error,
    }
    try:
        log_path = _get_log_path()
        with open(log_path, "a", encoding="utf-8") as f:
            f.write(json.dumps(record) + "\n")
    except Exception as exc:
        # Never let audit failure crash the pipeline
        logger.warning("Audit log write failed: %s | record: %s", exc, record)
