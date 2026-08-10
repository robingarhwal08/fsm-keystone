# Database Backup and Restore Runbook

| Field | Value |
|-------|-------|
| Owner | DevOps engineer |
| Last rehearsed | 2026-08-10 (see [rehearsals.md](rehearsals.md)) |

---

## Trigger

- **Backup**: nightly scheduled job (02:00 UTC); pre-deploy backup before any destructive migration.
- **Restore**: data-corruption incident; disaster recovery drill; restore-verification rehearsal.

---

## Preconditions

- `POSTGRES_USER`, `POSTGRES_DB`, and `POSTGRES_PASSWORD` are set in `.env`.
- Sufficient disk space for the dump file.
- For production restores: anonymisation policy reviewed (see Data-handling policy below).

---

## Topology reference

| Resource | Value |
|----------|-------|
| Container name | `postgres-db` |
| Image | `postgres:16` |
| Data volume | `postgres-data` (Docker named volume at `/var/lib/postgresql/data`) |
| Port | `127.0.0.1:5432:5432` (loopback only) |
| Known exposure | Port 5432 is published to the host. See [Known exposure](#known-exposure). |

---

## Backup

### Schedule

| Frequency | Retention | Storage |
|-----------|-----------|---------|
| Nightly (02:00 UTC) | 30 days rolling | Encrypted remote storage (e.g., S3 with SSE) |
| Pre-deploy (every deploy) | 7 days | Local + remote |

### Backup command

```bash
# Set variables from .env
source .env

# Dump to a timestamped file
BACKUP_FILE="fsmdb_$(date +%Y%m%d_%H%M%S).dump"

docker compose exec -T postgres-db \
  pg_dump \
    --username="${POSTGRES_USER:-postgres}" \
    --dbname="${POSTGRES_DB:-fsmdb}" \
    --format=custom \
    --compress=9 \
  > "/tmp/${BACKUP_FILE}"

echo "Backup written to /tmp/${BACKUP_FILE}"
ls -lh "/tmp/${BACKUP_FILE}"
```

**Note**: `--format=custom` produces a compressed binary dump that supports parallel restore with `pg_restore`.

### Encrypt the backup (required before off-host storage)

```bash
# Example using GPG symmetric encryption — substitute your organisation's key management
gpg --symmetric --cipher-algo AES256 "/tmp/${BACKUP_FILE}"
# Produces /tmp/${BACKUP_FILE}.gpg — transfer this file; delete the plaintext
rm "/tmp/${BACKUP_FILE}"
```

---

## Restore

> **Data-handling policy**: production data must **never** be copied to a non-production environment without anonymisation. Run the anonymisation step (Step 2b) before any restore to a dev or test database. Violating this policy requires immediate escalation to the security reviewer.

### Step 1. Stop the backend (to prevent writes during restore)

```bash
docker compose stop backend
```

### Step 2a. Restore to production (in-place)

```bash
# Drop and recreate the database
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" \
  -c "DROP DATABASE IF EXISTS ${POSTGRES_DB:-fsmdb}; CREATE DATABASE ${POSTGRES_DB:-fsmdb};"

# Restore from dump
docker compose exec -T postgres-db \
  pg_restore \
    --username="${POSTGRES_USER:-postgres}" \
    --dbname="${POSTGRES_DB:-fsmdb}" \
    --no-owner \
    --no-privileges \
  < "/tmp/${BACKUP_FILE}"
```

### Step 2b. Restore to non-production (with anonymisation)

```bash
# Restore to a scratch database
SCRATCH_DB="fsmdb_restore_scratch"
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" \
  -c "CREATE DATABASE ${SCRATCH_DB};"

docker compose exec -T postgres-db \
  pg_restore \
    --username="${POSTGRES_USER:-postgres}" \
    --dbname="${SCRATCH_DB}" \
  < "/tmp/${BACKUP_FILE}"

# Anonymise PII before promoting to non-production
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" -d "${SCRATCH_DB}" \
  -c "UPDATE app_user SET email = 'anon_' || id || '@example.com', full_name = 'Anon User', phone = NULL;"
```

### Step 3. Verify the restore

#### Row-count comparison (compare restored counts against backup manifest)

```bash
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-fsmdb}" \
  -c "\dt"  # List tables

docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-fsmdb}" << 'SQL'
SELECT 'app_user'       AS tbl, COUNT(*) FROM app_user
UNION ALL
SELECT 'customer',               COUNT(*) FROM customer
UNION ALL
SELECT 'site',                   COUNT(*) FROM site
UNION ALL
SELECT 'work_order',             COUNT(*) FROM work_order
UNION ALL
SELECT 'part',                   COUNT(*) FROM part
UNION ALL
SELECT 'part_usage',             COUNT(*) FROM part_usage
UNION ALL
SELECT 'time_log',               COUNT(*) FROM time_log
UNION ALL
SELECT 'status_history',         COUNT(*) FROM status_history
ORDER BY tbl;
SQL
```

#### Schema comparison (flyway_schema_history)

```bash
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-fsmdb}" \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

**Pass criterion**: row counts match the pre-restore baseline; all Flyway rows show `success = true`; no unexpected tables or missing tables.

### Step 4. Restart the backend

```bash
docker compose start backend
docker compose logs -f springboot-app | grep "Started KeystoneApplication"
```

---

## Known exposure

> **⚠ Known risk**: Port 5432 is published to `127.0.0.1:5432` in `docker-compose.yml`. This means the database is reachable from the host machine. While bound to loopback (not `0.0.0.0`), this still exposes the database to any process on the same host.
>
> **Interim mitigation**: firewall rules on the host to restrict port 5432 to maintenance tooling only.  
> **Remediation owner**: DevOps engineer.  
> **Target state**: remove the `ports` entry entirely; the backend reaches `postgres-db` over the Compose bridge network only.

---

## Restore-verification drill schedule

Perform a full restore drill quarterly (or after any significant schema migration):

1. Take a backup using the Backup command above.
2. Restore to a scratch database (Step 2b, with anonymisation).
3. Run the row-count and schema verification (Step 3).
4. Record the outcome in [rehearsals.md](rehearsals.md).

---

## Verification

**Pass criterion**: row counts and schema match the pre-restore state; all Flyway rows `success = true`; backend starts cleanly; no data-loss errors in the application log.

**Failure action**: if row counts do not match, escalate immediately before restarting the backend. Do not discard the dump file.

---

## Escalation

| Condition | Escalate to | Maximum time |
|-----------|-------------|-------------|
| Restore fails or row counts do not match | DevOps engineer + DBA | Immediate |
| PII data copied to non-production without anonymisation | Security reviewer | Immediate |
| Backup file missing or corrupted for > 24 h | DevOps engineer | 1 hour |
