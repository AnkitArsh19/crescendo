import asyncio
import json
import logging
import urllib.request
from typing import Dict, Any

logger = logging.getLogger(__name__)

import os

# Global state for catalog
app_state: Dict[str, Any] = {
    "catalog": [],
    "catalog_version": None,
}

JAVA_BACKEND_URL = os.getenv("JAVA_BACKEND_URL", "http://localhost:8080/internal/catalog")
POLL_INTERVAL_SECONDS = 30

def _fetch_full_catalog_sync():
    req = urllib.request.Request(
        JAVA_BACKEND_URL,
        headers={"Accept": "application/json", "User-Agent": "Crescendo-AIML/1.0"}
    )
    with urllib.request.urlopen(req, timeout=10) as response:
        return json.loads(response.read().decode())

def _fetch_version_sync():
    req = urllib.request.Request(
        f"{JAVA_BACKEND_URL}/version",
        headers={"Accept": "application/json", "User-Agent": "Crescendo-AIML/1.0"}
    )
    with urllib.request.urlopen(req, timeout=5) as response:
        return json.loads(response.read().decode())

async def fetch_full_catalog():
    try:
        data = await asyncio.to_thread(_fetch_full_catalog_sync)
        app_state["catalog"] = data.get("catalog", [])
        app_state["catalog_version"] = data.get("version")
        logger.info(f"Successfully fetched app catalog ({len(app_state['catalog'])} apps). Version: {app_state['catalog_version']}")
    except Exception as e:
        logger.warning(f"Could not connect to Java backend catalog at {JAVA_BACKEND_URL} ({e}). Will retry in background.")

async def fetch_catalog_version():
    try:
        data = await asyncio.to_thread(_fetch_version_sync)
        return data.get("version")
    except Exception as e:
        logger.debug(f"Catalog version check failed: {e}")
        return None

async def poll_catalog_version_loop():
    while True:
        await asyncio.sleep(POLL_INTERVAL_SECONDS)
        version = await fetch_catalog_version()
        if version and (version != app_state["catalog_version"] or not app_state["catalog"]):
            logger.info(f"Catalog updated or newly available ({app_state['catalog_version']} -> {version}). Fetching catalog...")
            await fetch_full_catalog()

async def start_catalog_sync():
    # Initial fetch
    await fetch_full_catalog()
    # Start background polling
    asyncio.create_task(poll_catalog_version_loop())
