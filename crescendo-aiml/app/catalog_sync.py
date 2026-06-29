import asyncio
import json
import logging
import urllib.request
from typing import Dict, Any

logger = logging.getLogger(__name__)

# Global state for catalog
app_state: Dict[str, Any] = {
    "catalog": [],
    "catalog_version": None,
}

JAVA_BACKEND_URL = "http://localhost:8080/internal/catalog"
POLL_INTERVAL_SECONDS = 30

def _fetch_full_catalog_sync():
    req = urllib.request.Request(JAVA_BACKEND_URL)
    with urllib.request.urlopen(req, timeout=10) as response:
        return json.loads(response.read().decode())

def _fetch_version_sync():
    req = urllib.request.Request(f"{JAVA_BACKEND_URL}/version")
    with urllib.request.urlopen(req, timeout=5) as response:
        return json.loads(response.read().decode())

async def fetch_full_catalog():
    try:
        data = await asyncio.to_thread(_fetch_full_catalog_sync)
        app_state["catalog"] = data.get("catalog", [])
        app_state["catalog_version"] = data.get("version")
        logger.info(f"Successfully fetched app catalog. Version: {app_state['catalog_version']}")
    except Exception as e:
        logger.error(f"Failed to fetch full catalog from Java backend: {e}")

async def fetch_catalog_version():
    try:
        data = await asyncio.to_thread(_fetch_version_sync)
        return data.get("version")
    except Exception as e:
        logger.error(f"Failed to fetch catalog version: {e}")
        return None

async def poll_catalog_version_loop():
    while True:
        await asyncio.sleep(POLL_INTERVAL_SECONDS)
        version = await fetch_catalog_version()
        if version and version != app_state["catalog_version"]:
            logger.info(f"Catalog version changed ({app_state['catalog_version']} -> {version}). Re-fetching...")
            await fetch_full_catalog()

async def start_catalog_sync():
    # Initial fetch
    await fetch_full_catalog()
    # Start background polling
    asyncio.create_task(poll_catalog_version_loop())
