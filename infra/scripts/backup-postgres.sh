#!/usr/bin/env bash
# Dumps the running aeroops Postgres database (via pg_dump inside the
# infra-postgres-1 container) to a local file. Custom format (-Fc) so it can
# be restored with pg_restore, including into a differently-named database.
#
# Usage: infra/scripts/backup-postgres.sh [output-file]
# Default output: infra/backups/aeroops-<timestamp>.dump

set -euo pipefail

CONTAINER="${AEROOPS_PG_CONTAINER:-infra-postgres-1}"
DB_USER="${AEROOPS_PG_USER:-aeroops}"
DB_NAME="${AEROOPS_PG_DB:-aeroops}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_DIR="$SCRIPT_DIR/../backups"
OUTPUT_FILE="${1:-$DEFAULT_DIR/aeroops-$(date +%Y%m%dT%H%M%SZ).dump}"

mkdir -p "$(dirname "$OUTPUT_FILE")"

docker exec "$CONTAINER" pg_dump -U "$DB_USER" -Fc "$DB_NAME" > "$OUTPUT_FILE"

echo "Backup written to $OUTPUT_FILE ($(wc -c < "$OUTPUT_FILE") bytes)"
