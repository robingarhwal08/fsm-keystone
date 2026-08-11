# Secret Rotation Runbook

**Scope:** JWT signing key and database password for `fsm-keystone`.  
**Cadence:** Every 90 days or immediately upon suspected compromise.  
**Estimated time:** 30 minutes per rotation cycle.  
**Prerequisite:** Read `docs/baseline/required-properties.md` and confirm every required
environment variable is ready before removing the `${ENV:literal}` fallbacks from
`application.properties` (Phase 2).

---

## Prerequisites

- Docker and Docker Compose V2 installed on the target host.
- `openssl` available in the shell.
- A git-ignored `.env` file at the repository root (see `.env.example` for the template).
- The application stack is running and a test user can log in successfully before you begin.
- You have a second terminal open to monitor `docker compose logs -f backend` during rotation.

---

## Part 1 — JWT Signing Key Rotation

### Background

The JWT signing key signs all user session tokens. Any token signed with the old key is
immediately invalid after the key is replaced unless a **dual-key verification window** is used.
Because the current token TTL is 24 hours (target: 15 minutes), the window must cover live
sessions. A forced-relogin notice must be sent if the window is shorter than the active TTL.

### Step 1 — Generate a New Signing Key

```bash
# Generate a 64-byte (512-bit) random hex string.
# NEVER commit the output — inject it via environment only.
openssl rand -hex 32
```

Save the output as `NEW_JWT_KEY`. Do not write it to any file that is tracked by git.

### Step 2 — Configure Dual-Key Verification Window

The dual-key window allows the application to:
- **Sign** all new tokens with `NEW_JWT_KEY`.
- **Verify** tokens still signed with the old key for a configurable window (e.g., 30 minutes).

**Current status:** Dual-key verification window is **live** (implemented in WO-020).
New tokens are always signed with the current key and carry a `kid` header.
Old tokens (signed with the previous key, with or without a `kid` header) remain valid
while `APP_JWT_PREVIOUS_SECRET` is set. Remove it once the fallback counter drains to zero.

### Step 3 — Open the Rotation Window

Set both the new current key **and** the old key as previous-secret, then restart:

```bash
# .env (git-ignored — NEVER commit this file)
APP_JWT_SECRET=<output-of-openssl-rand-hex-32>         # new signing key
APP_JWT_PREVIOUS_SECRET=<old-value-of-APP_JWT_SECRET>  # old key kept for verification
```

```bash
docker compose up -d --no-deps backend
```

All new tokens will be signed with `APP_JWT_SECRET`. Tokens signed with the old key
(still held by active sessions) continue to verify during this window.

### Step 4 — Monitor the Drain

Monitor the `jwt.verification.fallback` counter in Prometheus/Grafana until it flatlines
at zero. This indicates all pre-rotation sessions have either been re-authenticated or
expired.

```promql
# Rate of fallback verifications (old-key tokens still in use)
rate(jwt_verification_fallback_total[5m])
```

A `WARN` log line is emitted for every fallback verification:
```
JWT verified via previous key — subject=<email> keyId=<8-char-hex>
```

Once the counter has been zero for longer than `app.jwt.expiration-ms` (default 24 h):

```bash
# .env — remove previous-secret to close the window
APP_JWT_SECRET=<new-key>
# APP_JWT_PREVIOUS_SECRET  ← remove this line entirely
```

```bash
docker compose up -d --no-deps backend
```

### Step 5 — Post-Rotation Verification

```bash
# 1. Confirm the backend started cleanly
docker compose logs backend | grep "Started KeystoneApplication"

# 2. Verify a new login returns a token signed with the new key
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"<manager-email>","password":"<manager-password>"}' \
  | jq '.token'
# Expected: a non-empty JWT string

# 3. Verify an old-key token is now rejected (after window is closed)
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer <OLD_TOKEN>" \
  http://localhost:8080/api/dashboard/summary
# Expected: 401 (token rejected — no previous-secret configured)
```

If step 2 fails (no token returned), proceed immediately to the **Rollback** section.

### Step 6 — Rollback (if new key is rejected)

```bash
# Re-inject the previous key value (retrieved from your secret store or prior .env backup)
# Re-open the window:
APP_JWT_SECRET=<previous-key-from-secret-store>
APP_JWT_PREVIOUS_SECRET=<new-key-that-failed>  # optional — allows tokens from failed attempt

# Restart
docker compose up -d --no-deps backend

# Verify login succeeds again
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"<manager-email>","password":"<manager-password>"}' \
  | jq '.token'
```

Record the failure in the incident log and open a bug before re-attempting rotation.

---

## Part 2 — Database Password Rotation

### Step 1 — Generate a New Password

```bash
# Generate a 24-character random alphanumeric password.
openssl rand -base64 18 | tr -dc 'a-zA-Z0-9' | head -c24
```

Save the output as `NEW_DB_PASSWORD`. Do not write it to any tracked file.

### Step 2 — Update the Database Password

```bash
# Connect to the running PostgreSQL instance and change the password
docker compose exec postgres psql -U postgres -c \
  "ALTER USER postgres PASSWORD '<new-password-placeholder>';"
```

Replace `<new-password-placeholder>` with `NEW_DB_PASSWORD`. Do not paste the value into any
terminal that logs to a file — use shell variable substitution:

```bash
NEW_DB_PASSWORD=$(openssl rand -base64 18 | tr -dc 'a-zA-Z0-9' | head -c24)
docker compose exec postgres psql -U postgres -c \
  "ALTER USER postgres PASSWORD '$NEW_DB_PASSWORD';"
echo "Password updated. Store $NEW_DB_PASSWORD in your secret store now."
```

### Step 3 — Inject the New Password

Update your `.env` file:

```bash
SPRING_DATASOURCE_PASSWORD=<new-password>
POSTGRES_PASSWORD=<new-password>
```

Both variables must be updated together — they must match.

Restart the stack:

```bash
docker compose up -d --no-deps backend
```

### Step 4 — Post-Rotation Verification

```bash
# 1. Backend starts cleanly
docker compose logs backend | grep "HikariPool-1 - Start completed"

# 2. A test login succeeds (verifies DB connectivity via JPA)
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"<test-user-email>","password":"<test-user-password>"}' \
  | jq '.token'
# Expected: a non-empty JWT string
```

### Step 5 — Rollback (if backend fails to connect)

```bash
# Re-inject the previous password from your secret store
# Restore both variables in .env, then:
PREV_DB_PASSWORD=<previous-password>
docker compose exec postgres psql -U postgres -c \
  "ALTER USER postgres PASSWORD '$PREV_DB_PASSWORD';"
docker compose up -d --no-deps backend
```

---

## Part 3 — Compose Secrets Injection (Target Pattern)

Once the fallback literals are removed in Phase 2, secrets must be supplied entirely via
environment injection. The recommended pattern is:

```bash
# .env (git-ignored)
APP_JWT_SECRET=<generate-with-openssl-rand-hex-32>
SPRING_DATASOURCE_PASSWORD=<generate-with-openssl-rand-alphanumeric-24>
POSTGRES_PASSWORD=<same-as-SPRING_DATASOURCE_PASSWORD>
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/fsmdb
SPRING_DATASOURCE_USERNAME=postgres
APP_CORS_ALLOWED_ORIGIN=http://localhost:3000
```

```yaml
# docker-compose.yml (target shape — Phase 2)
services:
  backend:
    environment:
      APP_JWT_SECRET: ${APP_JWT_SECRET}           # no fallback
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}  # no fallback
```

Docker Compose automatically reads `.env` when it exists. Alternatively, pass secrets via
Docker secrets (`docker secret create`) for production deployments on Docker Swarm or ECS.

---

## Rotation Calendar

| Secret | Last Rotated | Next Due | Owner |
|---|---|---|---|
| JWT signing key | _NEVER — must rotate immediately_ | Within 7 days | Platform Engineering |
| Database password | _NEVER — must rotate immediately_ | Within 7 days | Platform Engineering |

**90-day cadence reminder:** Schedule a recurring calendar event on the day of each rotation
for +90 days. Include a link to this runbook. If a rotation is missed, treat it as an incident.

---

## Incident — Current Exposure

Both the JWT signing key and database password were committed to this public repository as
`${ENV:literal}` fallbacks. They must be treated as **compromised** regardless of whether
any unauthorized access has been confirmed. Rotate both before any hardening phase deploys
to a non-local environment.

See `docs/baseline/secrets-inventory.md` for the full exposure classification.
