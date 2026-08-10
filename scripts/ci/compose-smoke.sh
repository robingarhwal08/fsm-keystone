#!/usr/bin/env bash
# scripts/ci/compose-smoke.sh
#
# CI smoke test: bring the compose stack up from a generated .env,
# wait for the backend to answer, then tear everything down.
# Exits 1 if the backend never becomes healthy.
#
# Usage:
#   bash scripts/ci/compose-smoke.sh
#
# Requires: docker compose v2, curl, openssl

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE="docker compose"
BACKEND_URL="http://localhost:8080/api/auth/login"
MAX_WAIT_SECONDS=120

cleanup() {
  echo "[smoke] Tearing down compose stack …"
  $COMPOSE -f "$REPO_ROOT/docker-compose.yml" down -v --remove-orphans 2>/dev/null || true
  rm -rf "$REPO_ROOT/.env" "$REPO_ROOT/secrets"
}
trap cleanup EXIT

# ── Generate throwaway credentials ───────────────────────────────────────────

echo "[smoke] Generating throwaway .env and secrets …"
bash "$REPO_ROOT/scripts/dev/bootstrap-secrets.sh" --force

# ── Verify stack fails without .env ──────────────────────────────────────────

echo "[smoke] Verifying stack refuses to start without .env …"
mv "$REPO_ROOT/.env" "$REPO_ROOT/.env.smoke-backup"
if $COMPOSE -f "$REPO_ROOT/docker-compose.yml" config --quiet 2>/dev/null; then
  echo "[smoke] FAIL: compose config succeeded without .env (POSTGRES_PASSWORD not required)" >&2
  mv "$REPO_ROOT/.env.smoke-backup" "$REPO_ROOT/.env"
  exit 1
fi
mv "$REPO_ROOT/.env.smoke-backup" "$REPO_ROOT/.env"
echo "[smoke] PASS: compose config fails without .env"

# ── Bring the stack up ───────────────────────────────────────────────────────

echo "[smoke] Starting compose stack …"
$COMPOSE -f "$REPO_ROOT/docker-compose.yml" up -d --build

# ── Poll the backend ─────────────────────────────────────────────────────────

echo "[smoke] Waiting up to ${MAX_WAIT_SECONDS}s for backend …"
elapsed=0
until curl -sf -o /dev/null -w "%{http_code}" "$BACKEND_URL" \
    -H "Content-Type: application/json" \
    -d '{"email":"smoke@example.com","password":"noop"}' \
    | grep -qE "^(200|400|401|403)$"; do
  if [[ $elapsed -ge $MAX_WAIT_SECONDS ]]; then
    echo "[smoke] FAIL: backend did not become available after ${MAX_WAIT_SECONDS}s" >&2
    $COMPOSE -f "$REPO_ROOT/docker-compose.yml" logs backend >&2
    exit 1
  fi
  sleep 5
  elapsed=$((elapsed + 5))
  echo "[smoke]   still waiting … ${elapsed}s elapsed"
done

echo "[smoke] PASS: backend answered at $BACKEND_URL"

# ── Verify no credential appears in docker inspect environment ───────────────

echo "[smoke] Checking docker inspect for exposed credentials …"
INSPECT_ENV="$(docker inspect springboot-app --format '{{json .Config.Env}}')"
if echo "$INSPECT_ENV" | grep -qiE 'robin8181'; then
  echo "[smoke] FAIL: committed credential 'robin8181' found in container environment" >&2
  exit 1
fi
echo "[smoke] PASS: no committed credentials found in container environment"

echo ""
echo "[smoke] All checks passed."
