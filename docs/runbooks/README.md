# Operational Runbooks

This directory contains the operational runbooks for the FSM Keystone platform. Every runbook follows a consistent template:

> **Trigger → Preconditions → Steps → Verification → Rollback → Escalation → Owner**

An on-call responder can go from alert to verified resolution using only the committed documentation. Runbooks name concrete commands, container names, and endpoints. No real credentials appear — all examples use placeholder values.

## Index

| Runbook | Trigger condition | Owner |
|---------|-------------------|-------|
| [deploy-and-rollback.md](deploy-and-rollback.md) | New image ready to promote; or rollback decision after a failed deploy | DevOps engineer |
| [migration-failure.md](migration-failure.md) | Flyway startup failure; checksum mismatch; partially applied migration | Backend / Platform engineer |
| [authorization-regression.md](authorization-regression.md) | 401/403 spike; empty screens after default-deny cutover or tenancy-scoping deploy | Security reviewer + Backend engineer |
| [database-backup-restore.md](database-backup-restore.md) | Nightly backup job; data-corruption incident; restore-verification drill | DevOps engineer |
| [health-and-observability.md](health-and-observability.md) | Alert fires; unknown service state; request tracing needed | On-call responder (any role) |
| [rehearsals.md](rehearsals.md) | Scheduled rehearsal or post-incident drill | DevOps engineer |

## Related documents

- [../operations/slo.md](../operations/slo.md) — SLIs, SLOs, error budgets and alert conditions
- [../adr/README.md](../adr/README.md) — Architecture Decision Records referenced by these runbooks
- `secret-rotation.md` — JWT key and database password rotation procedure (⚠ PENDING — see ADR-0004)

## Runbook conventions

- **Placeholder credentials**: `${POSTGRES_PASSWORD}`, `${APP_JWT_SECRET}`. Never substitute real values.
- **Docker Compose V2 syntax**: `docker compose` (not `docker-compose`).
- **Container names**: `postgres-db`, `springboot-app`, `react-app` (as declared in `docker-compose.yml`).
- **Pending capabilities**: sections marked `⚠ PENDING` depend on features not yet shipped. Follow the forward reference before treating those steps as executable.
- **Prohibited shortcuts**: no runbook may instruct an operator to disable a security control (e.g., re-enabling `permitAll`, reverting `ddl-auto=update`, disabling `clean-disabled`). The sanctioned alternative is always given instead.
