import os

AI_PROVIDER = os.getenv("AI_PROVIDER", "gemini").lower()

if AI_PROVIDER == "gemini":
    # Google Gemini Production Models (ultra-fast latency & advanced reasoning)
    FAST_MODEL = os.getenv("FAST_MODEL", "gemini-3.5-flash-lite")
    REASONING_MODEL = os.getenv("REASONING_MODEL", "gemini-3.5-flash")
else:
    # Groq Production Model Registry
    FAST_MODEL = os.getenv("FAST_MODEL", "openai/gpt-oss-20b")
    REASONING_MODEL = os.getenv("REASONING_MODEL", "openai/gpt-oss-120b")

