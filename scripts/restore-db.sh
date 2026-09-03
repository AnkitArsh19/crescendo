#!/bin/bash
# ==============================================================================
# Crescendo Production PostgreSQL Restore Script
# ==============================================================================
# Restores binary pg_dump archives into the running PostgreSQL container.
#
# Usage:
#   ./scripts/restore-db.sh /path/to/backups/YYYYMMDD_HHMMSSZ [--force]
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

if [ -f "$ROOT_DIR/.env" ]; then
    # shellcheck disable=SC1091
    source "$ROOT_DIR/.env"
fi

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-crescendo-postgres}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
COMMAND_DB="${POSTGRES_COMMAND_DB:-crescendo_command}"
QUERY_DB="${POSTGRES_QUERY_DB:-crescendo_query}"

if [ $# -lt 1 ]; then
    echo "Usage: $0 <path-to-backup-directory> [--force]"
    exit 1
fi

TARGET_DIR="$1"
FORCE="${2:-}"

if [ ! -d "$TARGET_DIR" ]; then
    echo "Error: Directory '$TARGET_DIR' does not exist."
    exit 1
fi

COMMAND_DUMP=$(find "$TARGET_DIR" -name "${COMMAND_DB}_*.dump" | head -n 1)
QUERY_DUMP=$(find "$TARGET_DIR" -name "${QUERY_DB}_*.dump" | head -n 1)

if [ -z "$COMMAND_DUMP" ] || [ -z "$QUERY_DUMP" ]; then
    echo "Error: Could not find required dump files (${COMMAND_DB}_*.dump and ${QUERY_DB}_*.dump) in $TARGET_DIR."
    exit 1
fi

if [ "$FORCE" != "--force" ]; then
    echo "WARNING: Restoring will overwrite existing data in '$COMMAND_DB' and '$QUERY_DB'!"
    read -r -p "Are you sure you want to proceed with database restore? [y/N] " confirmation
    if [[ ! "$confirmation" =~ ^[Yy]$ ]]; then
        echo "Restore aborted by user."
        exit 0
    fi
fi

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Terminating active client connections..."
docker exec "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d postgres -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname IN ('$COMMAND_DB', '$QUERY_DB') AND pid <> pg_backend_pid();" || true

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Restoring $COMMAND_DB from $COMMAND_DUMP..."
docker exec -i "$POSTGRES_CONTAINER" pg_restore -U "$POSTGRES_USER" -d "$COMMAND_DB" --clean --if-exists --no-owner --no-privileges < "$COMMAND_DUMP" || true

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Restoring $QUERY_DB from $QUERY_DUMP..."
docker exec -i "$POSTGRES_CONTAINER" pg_restore -U "$POSTGRES_USER" -d "$QUERY_DB" --clean --if-exists --no-owner --no-privileges < "$QUERY_DUMP" || true

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Database restore finished successfully."
