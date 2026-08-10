# Testing Guide — fsm-keystone Backend

This document describes the test harness introduced in WO-006: categories, tags, run commands,
coverage policy, and the endpoint authorization inventory report.

---

## Test Categories

The backend uses three JUnit 5 tag-based categories enforced by Maven Surefire (unit + slice)
and Failsafe (integration).

| Category | JUnit Tag | Annotation style | Docker required? | Maven phase |
|---|---|---|---|---|
| **Unit** | `unit` | Pure Mockito, no Spring context | No | `test` |
| **Web Slice** | `slice` | `@WebMvcTest`, `@DataJpaTest` | No | `test` |
| **Integration** | `integration` | `@SpringBootTest` + Testcontainers | Yes | `verify` |

### Tagging rules

- Tag every new test class with exactly one category tag.
- `unit`: no application context, no I/O, pure logic + mocks.
- `slice`: uses a Spring Boot test slice (`@WebMvcTest`, `@DataJpaTest`); Mockito replaces services/repos.
- `integration`: full application context with a real PostgreSQL via Testcontainers; requires Docker.

---

## Run Commands

### Unit and slice tests (no Docker needed)

```bash
# From repo root:
mvn -f keystone-backend/pom.xml test

# Expected: green for all unit-tagged and slice-tagged tests.
# Coverage report NOT produced here — use verify for that.
```

### Integration tests only (Docker required)

```bash
mvn -f keystone-backend/pom.xml failsafe:integration-test failsafe:verify

# Skip on Docker-less CI runners:
mvn -f keystone-backend/pom.xml verify -DskipITs=true
```

### Full verify (unit + slice + integration + coverage check)

```bash
mvn -f keystone-backend/pom.xml verify

# Skip integration tests (unit + slice only, with coverage):
mvn -f keystone-backend/pom.xml verify -DskipITs=true
```

### Coverage report

JaCoCo produces `keystone-backend/target/site/jacoco/index.html` after `mvn verify`.

Open it locally:
```bash
open keystone-backend/target/site/jacoco/index.html   # macOS
xdg-open keystone-backend/target/site/jacoco/index.html  # Linux
```

---

## Coverage Floor and Ratchet Policy

JaCoCo enforces a **line coverage floor** in the `check` goal bound to the `verify` phase.

| Phase | Floor | Notes |
|---|---|---|
| Phase 0 (WO-006 baseline) | `0.00` | Harness-only suite; slice tests mock all services |
| Phase 1 (WO-011+) | `0.10` | Controller unit tests land; raise to 10% |
| Phase 2 (WO-013+) | `0.30` | Service layer unit tests |
| Phase N | — | Raise by at least 5 pp each sprint until ≥ 80% |

**Ratchet rule:** The `<minimum>` value in `pom.xml`'s jacoco `check` execution must be
raised when new test coverage is added. **Never lower the floor.**  
The current floor is documented in a comment directly next to the `<minimum>` element in
`keystone-backend/pom.xml`.

---

## Endpoint Authorization Inventory

`EndpointAuthorizationInventoryTest` (in `com.fsm.keystone.arch`) uses ArchUnit to scan
`com.fsm.keystone.controller`, then uses Java reflection to read each handler method's
`@PreAuthorize` expression.

### Output

Running `mvn -f keystone-backend/pom.xml test` writes:

```
keystone-backend/target/endpoint-authorization-inventory.txt
```

Example lines:
```
METHOD     PATH                                                    CONTROLLER                               @PreAuthorize
──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
DELETE     /api/customers/{id}                                     CustomerController                       NONE
GET        /api/dashboard/summary                                  DashboardController                      hasAnyRole('MANAGER','DISPATCHER')
POST       /api/auth/login                                         AuthController                           NONE
```

### Assertion

The test asserts `discoveredCount == EXPECTED_ENDPOINT_COUNT` (currently **34**).  
If a controller handler is added or removed, the test fails with a clear message listing the
delta. Update `EXPECTED_ENDPOINT_COUNT` in `EndpointAuthorizationInventoryTest` after
confirming the change is intentional.

### Purpose

This report is the evidence baseline for the authorization epic (WO-011+). It does **not**
fail on missing `@PreAuthorize` annotations yet — that gate is added in WO-011.

---

## Existing Test Files

| Package | Class | Category | Notes |
|---|---|---|---|
| `com.fsm.keystone` | `KeystoneApplicationTests` | `integration` | Full context + Testcontainers PostgreSQL |
| `com.fsm.keystone.arch` | `EndpointAuthorizationInventoryTest` | `unit` | ArchUnit + reflection inventory |
| `com.fsm.keystone.support` | `SmokeTest` | `unit` | Harness smoke test |
| `com.fsm.keystone.support` | `PlaceholderIT` | `integration` | Failsafe wiring proof |
| `com.fsm.keystone.security` | `*SecurityBaselineTest` (×9) | `slice` | WO-001 authorization baseline |
| `com.fsm.keystone.security` | `EndpointDiscoveryGuardTest` | — | WO-001 CSV guard (to be tagged in WO-011) |
| `com.fsm.keystone.config` | `DefaultValueGuardTest` | — | WO-002 secrets guard (to be tagged in WO-011) |
| `com.fsm.keystone.schema` | `SchemaSnapshotTest` | — | WO-003 schema baseline (to be tagged in WO-011) |
| `com.fsm.keystone.schema` | `EntityInventoryCoverageTest` | — | WO-003 entity coverage (to be tagged in WO-011) |

Tests without a tag are excluded from both Surefire and Failsafe until WO-011 tags them.

---

## Dockerfile

`keystone-backend/Dockerfile` builds with `mvn -B clean package` (tests enabled as of WO-006).
A failing test **prevents the Docker image from being built**. Integration tests are skipped
during the Docker build via `-DskipITs=true` (Docker daemon is not available inside the builder
container). Update the Dockerfile if the integration test suite needs a Testcontainers sidecar.
