# Forge Implementation Log

| Field | Value |
|-------|-------|
| Project | 76a65eff-144c-4497-8913-0b961b369a3d |
| Branch | forge/fsm-keystone-362eb0e5-run10-77wo |
| Started | 2026-08-10T21:52:24Z |

---

## WO-001: User Story: WO-001 - Baseline Route-by-Role Authorization Evidence Matrix
- **Status:** completed
- **Commit:** `aa8fc5d`
- **Files:** 18 (+1387/-1)
- **Duration:** 1308ss
- **Approach:** N/A

## WO-002: User Story: WO-002 - Secrets Exposure Inventory and Rotation Runbook Baseline
- **Status:** completed
- **Commit:** `d29efe2`
- **Files:** 12 (+808/-7)
- **Duration:** 788ss
- **Approach:** Implemented the secrets exposure inventory and rotation runbook baseline as a documentation-and-guard-only change (no production code modified). Grepped the full tree for secret-bearing patterns and recorded every finding in docs/baseline/secrets-inventory.md with file path, line reference, classification, and exposure status. Wrote a required-properties manifest flagging APP_JWT_SECRET and SPRING_DATASOURCE_PASSWORD as REQUIRED-NO-DEFAULT. Authored a rotation runbook covering JWT dual-key window, forced-relogin notice, compose secrets injection, and 90-day cadence. Added gitleaks configuration with targeted rules and narrow allowlists for test fixtures and lock files. Implemented a reporting-only DefaultValueGuard (Phase 0: asserts KNOWN_VIOLATION_COUNT=2, never echoes literal values) with synthetic fixture tests for isolation. Updated .gitignore to cover .env/.env.* while tracking .env.example, created the placeholder template, and sanitized README files to remove any credential guidance pointing to hardcoded values.

## WO-003: User Story: WO-003 - Capture PostgreSQL Schema Baseline and Column Inventory
- **Status:** completed
- **Commit:** `ef7ced7`
- **Files:** 6 (+754/-0)
- **Duration:** 1062ss
- **Approach:** Implemented the schema baseline as a documentation-and-guard-only change (no production code or ddl-auto changes). SchemaSnapshotTest uses @DataJpaTest + Testcontainers PostgreSQL 16 with JPA standard schema-generation properties to export DDL to target/test-schema-export.sql on context startup, then normalizes it (lowercase, CONSTRAINT names stripped, if-exists removed, whitespace collapsed, statements sorted) and compares against docs/baseline/schema-snapshot.sql. An UPDATE_SNAPSHOT=true system property mode lets developers regenerate the committed file. EntityInventoryCoverageTest uses Spring's ClassPathScanningCandidateComponentProvider to discover @Entity classes, reflects on their persistent fields (excluding inverse collection sides), and fails with an aggregated list of any class or field absent from the inventory document. Documentation covers all 8 entity classes with full per-field type/nullability/constraint/fetch-strategy/classification, a drift report (no production dump available), four anomaly findings with source references, and a step-by-step snapshot runbook.
