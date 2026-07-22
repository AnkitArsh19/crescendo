"""
Prompt Injection Defense — Sanitizer
======================================
All user input passes through here BEFORE reaching any LLM call.

Two responsibilities:
1. Scan for known prompt-injection patterns (regex). If matched → raise HTTP 400.
2. Wrap surviving input in <user_request>…</user_request> XML delimiters so LLM
   instruction context cannot be overridden by user-supplied text.
"""

import re
import logging
from fastapi import HTTPException, status

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Injection pattern library
# ---------------------------------------------------------------------------
# Patterns that reliably indicate an attempt to override the system prompt or
# exfiltrate instructions.  Case-insensitive matching.

_INJECTION_PATTERNS: list[re.Pattern] = [
    re.compile(r"ignore\s+(all\s+)?(previous|above|prior)\s+instructions?", re.IGNORECASE),
    re.compile(r"disregard\s+(all\s+)?(previous|above|prior)\s+instructions?", re.IGNORECASE),
    re.compile(r"forget\s+(all\s+)?(previous|above|prior)\s+instructions?", re.IGNORECASE),
    re.compile(r"you\s+are\s+now\s+(a|an|the)\b", re.IGNORECASE),
    re.compile(r"act\s+as\s+(a|an|the)\b", re.IGNORECASE),
    re.compile(r"reveal\s+(your\s+)?(system\s+prompt|instructions?|prompt)", re.IGNORECASE),
    re.compile(r"print\s+(your\s+)?(system\s+prompt|instructions?|prompt)", re.IGNORECASE),
    re.compile(r"show\s+(me\s+)?(your\s+)?(system\s+prompt|instructions?)", re.IGNORECASE),
    re.compile(r"output\s+(your\s+)?(system\s+prompt|instructions?|full\s+prompt)", re.IGNORECASE),
    re.compile(r"</?(system|instructions?|prompt)\s*>", re.IGNORECASE),   # XML tag injection
    re.compile(r"\[\[.*?override.*?\]\]", re.IGNORECASE),
    re.compile(r"DAN\s+mode", re.IGNORECASE),
    re.compile(r"jailbreak", re.IGNORECASE),
]


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def sanitize_prompt(raw_prompt: str) -> str:
    """
    Validate and sanitize user input.

    Parameters
    ----------
    raw_prompt : str
        Raw user-supplied text (already length-validated by Pydantic schema).

    Returns
    -------
    str
        The sanitized prompt wrapped in XML delimiters, safe to include in
        any LLM system or user message.

    Raises
    ------
    HTTPException(400)
        If any injection pattern is detected.  The error detail deliberately
        does not reveal which pattern matched.
    """
    for pattern in _INJECTION_PATTERNS:
        if pattern.search(raw_prompt):
            logger.warning(
                "Prompt injection attempt detected. Pattern=%s | Prompt preview=%.80r",
                pattern.pattern,
                raw_prompt,
            )
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=(
                    "Your request contains content that cannot be processed. "
                    "Please describe your workflow using plain language."
                ),
            )

    # Wrap in XML delimiters — prevents user content from being interpreted
    # as additional instructions even if the model is susceptible to such attacks.
    wrapped = f"<user_request>\n{raw_prompt}\n</user_request>"
    logger.debug("Prompt sanitized successfully (length=%d)", len(raw_prompt))
    return wrapped
