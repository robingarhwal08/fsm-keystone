# ADR-0006: Flyway versioned migrations with ddl-auto=validate

| Field  | Value      |
|--------|------------|
| Status | Accepted   |
| Date   | 2026-08-10 |

## Context

The backend currently uses `spring.jpa.hibernate.ddl-auto=update` (bound to `${SPRING_JPA_HIBERNATE_DDL_AUTO:update}` in `application.properties`). This means Hibernate silently alters or creates tables at every boot, making the schema state unknowable and unreproducible across environments. There is no `flyway_schema_history` table and no record of which migrations have been applied. This violates the SOC 2 audit-logging policy requirement that schema state be observable and auditable per environment.

WO-024 introduced Flyway 10 (`flyway-core` + `flyway-database-postgresql`) to the classpath with strict settings: `validate-on-migrate=true`, `clean-disabled=true`, `out-of-order=false`. An empty `db/migration` directory is tracked via a `README.md` placeholder. The baseline SQL (WO-041) and the `ddl-auto=validate` flip (WO-043) are the remaining steps.

## Decision

We will use Flyway versioned migrations (`V<version>__<description>.sql`) as the sole schema authority. Once the baseline SQL lands (WO-041), `spring.jpa.hibernate.ddl-auto` will be changed to `validate` (WO-043) so Hibernate verifies schema consistency without modifying it. Applied migrations are immutable; corrections are made by writing a new migration.

Dev and test environments use `spring.flyway.baseline-on-migrate=true` (in `application-dev.properties`) to handle the transition from the pre-Flyway state. Production uses `baseline-on-migrate=false` (in `application-prod.properties`).

## Consequences

- **Positive**: `flyway_schema_history` becomes the per-environment source of truth; schema state is observable and auditable.
- **Positive**: `validate-on-migrate=true` means Flyway fails fast at startup if the applied migrations do not match the checksums on disk, catching accidental edits.
- **Positive**: `clean-disabled=true` (in base `application.properties`, never overridden by a profile) prevents accidental schema drops in any environment.
- **Positive**: the migration directory placeholder (`db/migration/README.md`) guarantees the classpath location resolves even when the directory is empty.
- **Negative**: developers must never modify an applied migration; a checksum mismatch causes a startup failure that requires manual intervention.
- **Negative**: the `ddl-auto=update` current state means Hibernate and Flyway co-manage the schema until WO-043; care is needed to avoid Hibernate silently adding columns that Flyway later tries to apply via migration.
- **Expand-then-contract**: additive migrations (add column/table) can be deployed without downtime; destructive migrations (drop column/table) must follow the expand-then-contract pattern to avoid rolling-deployment errors.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| Liquibase | Equivalent capability. Flyway is simpler for SQL-first teams; the BOM already manages the Flyway version via the Spring Boot parent. No existing Liquibase configuration to migrate from. |
| Hibernate `ddl-auto=update` only | Current state; schema is unauditable and unreproducible. Incompatible with SOC 2 evidence requirements. |
| Manual SQL scripts applied by the DBA | No automated history tracking; error-prone; incompatible with Forge Shipping automated pre-deploy gate. |

## Evidence

```
keystone-backend/pom.xml — flyway-core and flyway-database-postgresql dependencies
  (no explicit version; managed by spring-boot-starter-parent 4.0.7).

keystone-backend/src/main/resources/application.properties
  — spring.flyway.enabled=true
  — spring.flyway.locations=classpath:db/migration
  — spring.flyway.validate-on-migrate=true
  — spring.flyway.clean-disabled=true
  — spring.flyway.out-of-order=false
  — spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:update}  (current; flipped in WO-043)

keystone-backend/src/main/resources/application-dev.properties
  — spring.flyway.baseline-on-migrate=true, spring.flyway.baseline-version=1

keystone-backend/src/main/resources/application-prod.properties
  — spring.flyway.baseline-on-migrate=false

keystone-backend/src/main/resources/db/migration/README.md — naming rules and immutability policy.

keystone-backend/src/test/java/com/fsm/keystone/flyway/FlywayMigrationIT.java
  — @Tag("integration") Testcontainers test asserting flyway_schema_history is created
    and clean is disabled.
```

## Related ADRs

- [ADR-0005](0005-forge-shipping-delivery-platform.md) — Forge Shipping pre-deploy gate that runs Flyway migration dry-run.
