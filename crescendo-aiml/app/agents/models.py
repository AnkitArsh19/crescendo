import os

# ---------------------------------------------------------------------------
# Groq Production Model Registry
# ---------------------------------------------------------------------------
# Fast / Instant Tier (Intent classification, plain-English summaries, clarifications)
# Replaces deprecated llama-3.1-8b-instant with Groq's official 1,000 t/s production model
FAST_MODEL = os.getenv("FAST_MODEL", "openai/gpt-oss-20b")

# High-Reasoning / Resolution Tier (Catalog key resolution, DAG topology, ReAct tool-calling)
# Flagship open-weight 120B reasoning model with tool calling on Groq Developer Plan (500 t/s)
REASONING_MODEL = os.getenv("REASONING_MODEL", "openai/gpt-oss-120b")
