# Documentation Index

This directory contains all architecture, operations, and baseline documentation for the FSM Keystone modernization programme.

## Architecture Decisions

| Resource | Description |
|----------|-------------|
| [adr/README.md](adr/README.md) | Index of all Architecture Decision Records (ADRs) |
| [adr/0001-default-deny-security-filter-chain.md](adr/0001-default-deny-security-filter-chain.md) | Default-deny SecurityFilterChain with STATELESS sessions |
| [adr/0002-method-security-preauthorize-archunit-gate.md](adr/0002-method-security-preauthorize-archunit-gate.md) | @PreAuthorize method security with ArchUnit enforcement |
| [adr/0003-service-layer-tenancy-scoping.md](adr/0003-service-layer-tenancy-scoping.md) | Service-layer tenancy scoping on AppUser.customer |
| [adr/0004-secrets-compose-secrets-and-rotation-runbook.md](adr/0004-secrets-compose-secrets-and-rotation-runbook.md) | Docker Compose secrets and manual rotation runbook |
| [adr/0005-forge-shipping-delivery-platform.md](adr/0005-forge-shipping-delivery-platform.md) | Forge Shipping as the delivery platform |
| [adr/0006-flyway-versioned-migrations.md](adr/0006-flyway-versioned-migrations.md) | Flyway versioned migrations with ddl-auto=validate |
| [adr/0007-request-response-records-and-pagination.md](adr/0007-request-response-records-and-pagination.md) | Request/response records with Bean Validation |
| [adr/0008-frontend-dependency-pinning-and-node-lts.md](adr/0008-frontend-dependency-pinning-and-node-lts.md) | Frontend dependency pinning and Node 24 LTS build image |
| [adr/0009-jwt-token-ttl-and-refresh-strategy.md](adr/0009-jwt-token-ttl-and-refresh-strategy.md) | JWT access-token TTL and refresh strategy |

## Operations Runbooks

| Resource | Description |
|----------|-------------|
| [runbooks/secret-rotation.md](runbooks/secret-rotation.md) | JWT key and database password rotation procedure |
| [runbooks/dependency-baseline.md](runbooks/dependency-baseline.md) | Dependency baseline and SBOM regeneration |
| [runbooks/schema-snapshot.md](runbooks/schema-snapshot.md) | PostgreSQL schema snapshot and drift detection |

## Baseline Inventory

| Resource | Description |
|----------|-------------|
| [baseline/required-properties.md](baseline/required-properties.md) | Required environment variables manifest |
| [baseline/secrets-inventory.md](baseline/secrets-inventory.md) | Secrets exposure inventory and status |
| [baseline/schema-drift-report.md](baseline/schema-drift-report.md) | Schema snapshot baseline and drift report |
| [baseline/dependency-inventory.md](baseline/dependency-inventory.md) | Dependency inventory with versions |
| [baseline/image-baseline.md](baseline/image-baseline.md) | Docker image baseline and digest pins |
| [baseline/proposed-pins.md](baseline/proposed-pins.md) | Proposed frontend dependency pins |
| [baseline/runtime-support-matrix.md](baseline/runtime-support-matrix.md) | Runtime and container support matrix |

## Testing

| Resource | Description |
|----------|-------------|
| [testing.md](testing.md) | Test categories (unit/slice/integration), run commands, coverage policy |

## Component READMEs

| Resource | Description |
|----------|-------------|
| [../keystone-backend/README.md](../keystone-backend/README.md) | Backend prerequisites, environment setup, run and test commands |
| [../frontend/README.md](../frontend/README.md) | Frontend prerequisites, dependency pinning, build commands |

---

> Run `bash scripts/ci/check-docs-links.sh` from the project root to validate all relative links.
