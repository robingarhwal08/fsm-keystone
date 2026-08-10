# Architecture Decision Records

This directory contains the Architecture Decision Records (ADRs) for the FSM Keystone modernization programme. Each ADR captures a significant decision, its context, the alternatives that were considered, and the consequences — so future engineers can understand *why* the system is the way it is rather than re-litigating settled questions.

## Index

| Number | Title | Status | Date |
|--------|-------|--------|------|
| [ADR-0000](0000-adr-template.md) | ADR template | Reference | — |
| [ADR-0001](0001-default-deny-security-filter-chain.md) | Default-deny security filter chain | Accepted | 2026-08-10 |
| [ADR-0002](0002-method-security-preauthorize-archunit-gate.md) | Method security via @PreAuthorize with ArchUnit enforcement | Accepted | 2026-08-10 |
| [ADR-0003](0003-service-layer-tenancy-scoping.md) | Service-layer tenancy scoping on AppUser.customer | Accepted | 2026-08-10 |
| [ADR-0004](0004-secrets-compose-secrets-and-rotation-runbook.md) | Secrets via Docker Compose secrets and manual rotation runbook | Accepted | 2026-08-10 |
| [ADR-0005](0005-forge-shipping-delivery-platform.md) | Forge Shipping as the delivery platform | Accepted | 2026-08-10 |
| [ADR-0006](0006-flyway-versioned-migrations.md) | Flyway versioned migrations with ddl-auto=validate | Accepted | 2026-08-10 |
| [ADR-0007](0007-request-response-records-and-pagination.md) | Request/response records with Bean Validation and Page envelopes | Accepted | 2026-08-10 |
| [ADR-0008](0008-frontend-dependency-pinning-and-node-lts.md) | Frontend dependency pinning and Node 24 LTS build image | Accepted | 2026-08-10 |
| [ADR-0009](0009-jwt-token-ttl-and-refresh-strategy.md) | JWT access-token TTL reduction to 15 minutes with rotating refresh tokens | Accepted | 2026-08-10 |

## Process

### Adding a new ADR

1. Copy [0000-adr-template.md](0000-adr-template.md) to `NNNN-kebab-case-title.md` where `NNNN` is the next sequential number.
2. Fill in every section. The **Evidence** section must cite real file paths and symbols that exist at the time of writing — fabricated paths make the record untrustworthy.
3. Set status to `Proposed` until the decision is accepted by the team.
4. Add a row to the index table above.
5. Reference the ADR number in your pull-request description.

### Superseding an ADR

When a decision changes, do **not** edit the accepted ADR. Instead:
1. Write a new ADR with status `Accepted` and link back to the superseded record.
2. Update the superseded ADR's status line to `Superseded by ADR-NNNN` — do not delete the file.
3. Update the index table to reflect both statuses.

### Deprecating an ADR

Mark the status `Deprecated` and add a note in the Consequences section explaining why the decision no longer applies. Keep the file so the history survives.

### Status vocabulary

| Status | Meaning |
|--------|---------|
| `Proposed` | Under discussion; not yet adopted |
| `Accepted` | Adopted and in effect |
| `Superseded by ADR-NNNN` | Replaced by a later decision; file kept for history |
| `Deprecated` | No longer applies; context has changed |

## Link checker

Run `bash scripts/ci/check-docs-links.sh` to validate that all relative links in `docs/` resolve to existing files. This script can be wired into Forge Shipping as a pre-merge gate.
