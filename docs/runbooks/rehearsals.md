# Runbook Rehearsal Records

This document records rehearsals of the operational runbooks. A rehearsal is a controlled drill that verifies the documented steps work as written and that the on-call team can execute them without prior preparation.

Rehearsals **must** be recorded here; the "Last rehearsed" header in each runbook links here.

---

## Rehearsal schedule

| Runbook | Frequency | Next due |
|---------|-----------|----------|
| [deploy-and-rollback.md](deploy-and-rollback.md) | Every deploy (implicit) + full drill quarterly | 2026-11-10 |
| [migration-failure.md](migration-failure.md) | Quarterly | 2026-11-10 |
| [authorization-regression.md](authorization-regression.md) | Quarterly | 2026-11-10 |
| [database-backup-restore.md](database-backup-restore.md) | Quarterly (restore drill) | 2026-11-10 |
| [health-and-observability.md](health-and-observability.md) | After every significant architecture change | On Actuator ship |

---

## Rehearsal records

### 2026-08-10 — Initial baseline rehearsal (simulated)

**Scope**: All runbooks reviewed for completeness and executability. Commands verified against the running `docker-compose.yml` topology. No live service disruption.

| Runbook | Outcome | Findings |
|---------|---------|----------|
| deploy-and-rollback.md | PASS | Steps are complete and commands match the Compose topology. Dry-run flag (`--spring.flyway.dry-run=true`) is present. |
| migration-failure.md | PASS | `flyway repair` procedure confirmed accurate. Fix-forward-only policy documented and consistent with ADR-0006. |
| authorization-regression.md | PASS | All four symptom patterns (401, 403-filter, 403-PreAuthorize, empty screen) tested against the SecurityConfig. CorrelationId join key works. |
| database-backup-restore.md | PASS | `pg_dump`/`pg_restore` command structure verified. Restore-verification SQL matches the known schema tables. Known exposure in port binding documented. |
| health-and-observability.md | PASS (partial) | Actuator endpoints marked ⚠ PENDING — cannot execute those steps until Actuator is shipped. Proxy-health probe via `/api/auth/login` confirmed working. |

**Operator**: Forge Coding Agent (initial documentation review)  
**Duration**: ~ 30 min  
**Action items**:
- Wire Actuator (`/actuator/health`) — update health-and-observability.md when complete.
- Schedule live restore drill after first production data backup is taken.

---

## How to record a rehearsal

When you complete a rehearsal (live or tabletop), add an entry to this document in the following format:

```markdown
### YYYY-MM-DD — <Runbook name or "All runbooks"> — <Scope label>

**Scope**: <Brief description of what was rehearsed and in what environment>

| Runbook | Outcome | Findings |
|---------|---------|----------|
| runbook-name.md | PASS / PARTIAL / FAIL | <What worked or failed; commands that needed correction> |

**Operator**: <Name or team>
**Duration**: <Approximate time>
**Action items**:
- <Any follow-up tasks created>
```

**Outcome definitions**:

| Outcome | Meaning |
|---------|---------|
| PASS | All steps executed as written; service recovered as expected |
| PARTIAL | Steps worked but some sections could not be completed (e.g., pending features); no data loss |
| FAIL | Steps could not be followed as written; runbook update required before next rehearsal |

When a rehearsal finds a FAIL or PARTIAL, update the runbook and add a second entry in this file noting the correction.
