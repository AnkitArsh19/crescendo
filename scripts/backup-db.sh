#!/bin/bash
# ==============================================================================
# Crescendo Production PostgreSQL Backup Script
# ==============================================================================
# Performs consistent binary pg_dump of command and query databases from
# the running Docker container, compresses, and synchronizes to AWS S3 / Cloudflare R2.
#
# Recommended Cron (daily at 03:00 UTC):
#   0 3 * * * /path/to/crescendo/scripts/backup-db.sh >> /var/log/crescendo-backup.log 2>&1
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# Source environment variables if .env exists
if [ -f "$ROOT_DIR/.env" ]; then
    # shellcheck disable=SC1091
    source "$ROOT_DIR/.env"
fi

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-crescendo-postgres}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
COMMAND_DB="${POSTGRES_COMMAND_DB:-crescendo_command}"
QUERY_DB="${POSTGRES_QUERY_DB:-crescendo_query}"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"
TIMESTAMP="$(date -u +"%Y%m%d_%H%M%SZ")"
TARGET_DIR="$BACKUP_DIR/$TIMESTAMP"

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Starting Crescendo database backup..."

mkdir -p "$TARGET_DIR"

# 1. Dump Command DB (Postgres custom format with compression)
echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Dumping $COMMAND_DB..."
docker exec "$POSTGRES_CONTAINER" pg_dump -U "$POSTGRES_USER" -Fc -b -v "$COMMAND_DB" > "$TARGET_DIR/${COMMAND_DB}_${TIMESTAMP}.dump"

# 2. Dump Query DB
echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Dumping $QUERY_DB..."
docker exec "$POSTGRES_CONTAINER" pg_dump -U "$POSTGRES_USER" -Fc -b -v "$QUERY_DB" > "$TARGET_DIR/${QUERY_DB}_${TIMESTAMP}.dump"

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Local dumps created successfully at $TARGET_DIR."

# 3. Upload to S3 if configured
if [ -n "${S3_BUCKET_NAME:-}" ] && command -v aws >/dev/null 2>&1; then
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Uploading backups to s3://$S3_BUCKET_NAME/backups/$TIMESTAMP/..."
    
    EXTRA_ARGS=""
    if [ -n "${S3_ENDPOINT_OVERRIDE:-}" ]; then
        EXTRA_ARGS="--endpoint-url $S3_ENDPOINT_OVERRIDE"
    fi
    
    # shellcheck disable=SC2086
    aws s3 cp "$TARGET_DIR" "s3://$S3_BUCKET_NAME/backups/$TIMESTAMP/" --recursive $EXTRA_ARGS
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] S3 sync completed."
else
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] AWS CLI not found or S3_BUCKET_NAME not set. Retaining local backup only."
fi

# 4. Retention: Delete local backups older than 7 days
echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Pruning local backups older than 7 days..."
find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -mtime +7 -exec rm -rf {} +

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Backup process completed successfully."
