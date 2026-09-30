#!/usr/bin/env bash
# Restores a pg_dump custom-format file (as produced by backup-postgres.sh)
# into a Postgres container. Defaults to a *throwaway* target so this is safe
# to run against a rehearsal container without risking the live dev database
# — pass AEROOPS_PG_CONTAINER=infra-postgres-1 explicitly to target the real
# one, and only do that if you actually mean to overwrite it.
#
# Usage: infra/scripts/restore-postgres.sh <dump-file> [target-container]

set -euo pipefail

DUMP_FILE="${1:?Usage: restore-postgres.sh <dump-file> [target-container]}"
CONTAINER="${2:-${AEROOPS_PG_CONTAINER:-aeroops-restore-rehearsal}}"
DB_USER="${AEROOPS_PG_USER:-aeroops}"
DB_NAME="${AEROOPS_PG_DB:-aeroops}"

if [ ! -f "$DUMP_FILE" ]; then
  echo "Dump file not found: $DUMP_FILE" >&2
  exit 1
fi

echo "Restoring $DUMP_FILE into database '$DB_NAME' on container '$CONTAINER'..."

# --clean drops existing objects first so this script is safe to re-run
# against the same target (used by the rollback rehearsal to restore twice).
docker exec -i "$CONTAINER" pg_restore -U "$DB_USER" -d "$DB_NAME" --clean --if-exists < "$DUMP_FILE"

echo "Restore complete."
