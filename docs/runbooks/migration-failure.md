# Migration Failure Runbook

| Field | Value |
|-------|-------|
| Owner | Backend / Platform engineer |
| Last rehearsed | 2026-08-10 (see [rehearsals.md](rehearsals.md)) |

---

## Trigger

- Spring Boot fails to start with a Flyway error (logged at `ERROR` level).
- `flyway_schema_history` shows a row with `success = false`.
- A checksum mismatch error prevents startup: `Migration checksum mismatch for migration version N`.
- A migration was partially applied (connection drop mid-migration).

---

## Preconditions

- Access to the `postgres-db` container or the database host.
- `POSTGRES_USER` and `POSTGRES_DB` available in `.env`.
- The migration file that caused the failure has been identified.

---

## Production Flyway settings (reference)

| Property | Value | Effect |
|----------|-------|--------|
| `spring.flyway.validate-on-migrate` | `true` | Checksums verified on every startup |
| `spring.flyway.clean-disabled` | `true` | `flyway clean` is **always prohibited** |
| `spring.flyway.out-of-order` | `false` | Versions must be strictly increasing |
| `spring.flyway.baseline-on-migrate` | `true` (dev) / `false` (prod) | See `application-dev.properties` |

---

## Steps

### 1. Read the startup log

```bash
docker compose logs springboot-app | grep -E "ERROR|Flyway|FlywayException|migration"
```

Identify the migration version, description, and error type.

### 2. Inspect flyway_schema_history

```bash
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-fsmdb}" \
  -c "SELECT installed_rank, version, description, type, success, checksum
      FROM flyway_schema_history ORDER BY installed_rank;"
```

| success | type | Meaning |
|---------|------|---------|
| `true`  | `SQL` | Applied cleanly |
| `false` | `SQL` | Failed mid-execution (partially applied) |
| — (row absent) | — | Not yet started |

### 3. Diagnose the failure type

#### 3a. Checksum mismatch

**Symptom**: `Migration checksum mismatch for migration version N`  
**Cause**: An applied migration file was edited after it was run (violates immutability rule).

```bash
# Compare the checksum in the database with the file on disk
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-fsmdb}" \
  -c "SELECT version, checksum FROM flyway_schema_history WHERE version = 'N';"
```

**Fix**: The edited file must be **reverted to its original content** exactly as it was when first applied. The committed checksum is the source of truth. **Never delete the row from `flyway_schema_history`** to bypass the check — that removes the audit trail.

If the original is lost: restore from the Git history of `db/migration/VN__*.sql` at the commit it was first applied.

#### 3b. Partially applied migration (success = false)

**Symptom**: Row exists in `flyway_schema_history` with `success = false`.  
**Cause**: The migration ran but failed mid-script (e.g., connection drop, constraint violation).

```bash
# Inspect the failed row
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-fsmdb}" \
  -c "SELECT version, description, installed_on, execution_time, success
      FROM flyway_schema_history WHERE success = false;"
```

**Fix (fix-forward only)**: Write a new migration `V(N+1)__fix_previous_failure.sql` that repairs the partial state and leaves the schema in the desired end state. **Do not edit VN** — its row remains in the history.

If the partial state must be repaired before the fix-forward migration can run:

```bash
# Manually repair the schema in a transaction
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-fsmdb}"
# Apply targeted DDL to undo the partial change, then exit

# Then repair the Flyway history row:
docker compose run --rm backend \
  java -jar app.jar --spring.flyway.repair=true
```

`flyway repair` removes failed rows from `flyway_schema_history` and recalculates checksums for any rows that are now inconsistent. It does **not** revert schema changes.

#### 3c. Out-of-order migration detected

**Symptom**: `Detected applied migration not resolved locally` or `out-of-order` error.  
**Cause**: A migration with a lower version number exists but was not in the history when the higher version was applied (e.g., two branches both adding migrations).

**Fix**: `out-of-order=false` means the lower-version migration cannot be applied. Renumber the new migration to a higher version than the currently applied maximum.

### 4. Immutability rule

> Applied migrations are **immutable**. Never edit, rename, or delete a migration that has been applied to any non-dev environment. Flyway verifies checksums on every startup with `validate-on-migrate=true`.

Fix forward always. The migration history is the audit trail.

---

## Expand-then-contract sequence

For non-backward-compatible changes (rename column, drop column, add NOT NULL):

| Phase | Migration | App behaviour |
|-------|-----------|---------------|
| **Expand** | Add the new column (nullable, no constraint) | Old revision ignores new column |
| **Backfill** | Populate new column from old values | Both revisions can read either column |
| **Dual-write** | Application writes to both columns | Transition period |
| **Switch reads** | Application reads from new column only | Both revisions deployed |
| **Contract** | Drop the old column in a new migration | Old revision no longer deployed |

Never attempt the Contract phase until the old revision is fully retired from all environments.

---

## Verification

```bash
# All rows show success = true
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-fsmdb}" \
  -c "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false;"
# Expected: 0

# Application starts successfully
docker compose up -d --no-deps backend
docker compose logs springboot-app | grep "Started KeystoneApplication"
```

**Pass criterion**: zero failed rows in `flyway_schema_history`, application starts with no Flyway errors.

---

## Rollback

Schema rollback is **prohibited** in production. Fix forward only.

In a dev/test environment with a disposable database:

```bash
# Drop and recreate the database (dev only — never in production)
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" \
  -c "DROP DATABASE IF EXISTS fsmdb; CREATE DATABASE fsmdb;"
docker compose restart backend
```

---

## Escalation

| Condition | Escalate to | Maximum time |
|-----------|-------------|-------------|
| Partial migration in production with data loss risk | Backend engineer + DevOps + DBA | Immediate |
| Repair requires manual DDL in production | Backend engineer + DevOps | Immediate |
| Audit trail concern (history row deleted) | Security reviewer | Immediate |
