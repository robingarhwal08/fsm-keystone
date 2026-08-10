#!/usr/bin/env bash
# scripts/dev/bootstrap-secrets.sh
#
# Generates a local .env file and the ./secrets/ directory required by
# docker compose up.  Safe to re-run — existing files are NOT overwritten
# unless -f (--force) is passed.
#
# Usage:
#   bash scripts/dev/bootstrap-secrets.sh          # first-time setup
#   bash scripts/dev/bootstrap-secrets.sh --force  # regenerate all secrets
#
# Windows developers: run this in Git Bash, WSL, or execute the equivalent
# PowerShell commands documented at the bottom of this file.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FORCE=false

for arg in "$@"; do
  case $arg in
    -f|--force) FORCE=true ;;
    *) echo "Unknown argument: $arg" >&2; exit 1 ;;
  esac
done

# ── Helper ───────────────────────────────────────────────────────────────────

write_if_missing() {
  local path="$1" content="$2"
  if [[ -f "$path" && "$FORCE" == false ]]; then
    echo "  SKIP  $path (already exists — use --force to regenerate)"
  else
    printf '%s' "$content" > "$path"
    echo "  WRITE $path"
  fi
}

# ── .env ─────────────────────────────────────────────────────────────────────

ENV_FILE="$REPO_ROOT/.env"

if [[ ! -f "$ENV_FILE" || "$FORCE" == true ]]; then
  echo "Generating $ENV_FILE from .env.example …"
  cp "$REPO_ROOT/.env.example" "$ENV_FILE"

  # Replace placeholder values with generated secrets
  DB_PASS="$(openssl rand -hex 16)"
  JWT_SECRET="$(openssl rand -hex 32)"

  sed -i.bak \
    -e "s|POSTGRES_PASSWORD=<your-database-password>|POSTGRES_PASSWORD=${DB_PASS}|g" \
    -e "s|SPRING_DATASOURCE_PASSWORD=<your-database-password>|SPRING_DATASOURCE_PASSWORD=${DB_PASS}|g" \
    -e "s|APP_JWT_SECRET=<generate-with-openssl-rand-hex-32>|APP_JWT_SECRET=${JWT_SECRET}|g" \
    "$ENV_FILE"
  rm -f "$ENV_FILE.bak"
  echo "  WRITE $ENV_FILE"
else
  echo "  SKIP  $ENV_FILE (already exists — use --force to regenerate)"
fi

# ── ./secrets/ ───────────────────────────────────────────────────────────────

SECRETS_DIR="$REPO_ROOT/secrets"
mkdir -p "$SECRETS_DIR"
echo "Generating secrets in $SECRETS_DIR …"

# Read generated values from .env
source <(grep -E '^(SPRING_DATASOURCE_PASSWORD|APP_JWT_SECRET)=' "$ENV_FILE")

write_if_missing "$SECRETS_DIR/spring.datasource.password" "${SPRING_DATASOURCE_PASSWORD}"
write_if_missing "$SECRETS_DIR/app.jwt.secret" "${APP_JWT_SECRET}"

echo ""
echo "Done. Run 'docker compose up' to start the stack."
echo ""
echo "NOTE: If you already have a postgres-data volume created with a different"
echo "password, drop it first: docker compose down -v"

# ── Windows PowerShell equivalent (manual) ───────────────────────────────────
#
# PowerShell:
#   Copy-Item .env.example .env
#   $dbPass   = -join ((1..16) | ForEach-Object { '{0:x2}' -f (Get-Random -Maximum 256) })
#   $jwtSecret = -join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Maximum 256) })
#   (Get-Content .env) `
#     -replace 'POSTGRES_PASSWORD=<your-database-password>', "POSTGRES_PASSWORD=$dbPass" `
#     -replace 'SPRING_DATASOURCE_PASSWORD=<your-database-password>', "SPRING_DATASOURCE_PASSWORD=$dbPass" `
#     -replace 'APP_JWT_SECRET=<generate-with-openssl-rand-hex-32>', "APP_JWT_SECRET=$jwtSecret" |
#     Set-Content .env
#   New-Item -ItemType Directory -Force secrets | Out-Null
#   $dbPass    | Set-Content secrets\spring.datasource.password -NoNewline
#   $jwtSecret | Set-Content secrets\app.jwt.secret -NoNewline
