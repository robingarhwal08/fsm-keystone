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

## WO-004: User Story: WO-004 - Dependency, Runtime and Container Image Baseline Inventory
- **Status:** completed
- **Commit:** `5ba4c6f`
- **Files:** 11 (+2391/-0)
- **Duration:** 464ss
- **Approach:** Implemented WO-004 as a documentation-and-check-only change (no production manifests modified). Parsed frontend/package.json and package-lock.json with Python to extract declared vs. resolved versions and enumerate all 79 lockfile packages. Generated a valid CycloneDX 1.4 SBOM from the lockfile programmatically (79 components: 46 required, 33 optional platform bindings) since @cyclonedx/cyclonedx-npm is not installed in the sandbox; the runbook documents the exact regeneration command. Constructed a direct-dependency Maven SBOM from pom.xml analysis (16 components) with the same note. The unpinned-dependency check (check-unpinned-deps.js) is a plain Node.js script with no external dependencies, implementing Phase 0 reporting-only semantics (asserts KNOWN_VIOLATION_COUNT=6, exits 0 when count matches, exits 1 on regression). The test suite (14 tests) runs offline using synthetic fixtures and the real package.json. Documentation covers dependency inventory, runtime/image support matrix (Node 20 EOL flagged, Node 24 recommended), Docker image provenance (no digest pins, root user, full JDK), proposed pins, and an SBOM regeneration runbook.

## WO-006: User Story: WO-006 - Establish backend test harness and coverage tooling
- **Status:** completed
- **Commit:** `4e0c013`
- **Files:** 8 (+493/-2)
- **Duration:** 796ss
- **Approach:** Introduced a complete test harness on top of the existing WO-001/002/003 test infrastructure. (1) pom.xml: added Testcontainers BOM import in dependencyManagement (using ${testcontainers.version} managed by spring-boot-starter-parent), archunit-junit5 1.4.0 test dependency, maven-surefire-plugin configured with groups='unit | slice', maven-failsafe-plugin configured with groups='integration' + includes='**/*.java' + skipITs property, and jacoco-maven-plugin with prepare-agent/report/check goals (Phase-0 floor 0.00 with ratchet policy comment). (2) Dockerfile: replaced 'mvn clean package -DskipTests' with 'mvn -B clean package -DskipITs=true' so unit/slice tests gate the image build while integration tests (requiring Docker daemon) are skipped in the builder container. (3) KeystoneApplicationTests: converted from an untagged @SpringBootTest (which would fail without a database) to a proper integration test tagged @Tag('integration') with @Testcontainers and a static PostgreSQLContainer + @DynamicPropertySource wiring. (4) BaseControllerSecurityTest: added @Tag('slice') so all 9 @WebMvcTest security baseline subclasses from WO-001 run under Surefire's slice category. (5) Created SmokeTest (unit category), PlaceholderIT (integration category), and EndpointAuthorizationInventoryTest using ArchUnit ClassFileImporter + reflection to scan com.fsm.keystone.controller, write target/endpoint-authorization-inventory.txt, and assert discovered count == 34. The WO specified 37 but actual codebase has 34 endpoints (verified by grep across all 9 controller files and the WO-001 authorization-matrix.csv which has 34 data rows). (6) docs/testing.md documents all three categories, run commands, coverage ratchet policy, and the inventory report.

## WO-012: User Story: WO-012 - Introduce typed domain exception hierarchy
- **Status:** completed
- **Commit:** `0de809b`
- **Files:** 10 (+695/-0)
- **Duration:** 346ss
- **Approach:** Purely additive change: new com.fsm.keystone.exception package only, no production service/controller/repository classes modified. ErrorCode enum defines 18 UPPER_SNAKE_CASE constants covering all failure modes observed across WorkOrderService, AuthService, CustomerService, SiteService, UserService, PartService and DashboardService, each declaring its default HttpStatus so the WO-021 @RestControllerAdvice needs no switch statement. ApiException abstract base extends RuntimeException (preserving Spring @Transactional rollback semantics), carries ErrorCode, HttpStatus, an immutable Map.copyOf details map, and an optional Throwable cause; a private static guard rejects denylisted detail keys (password, token, secret, authorization, credential, credentials, apikey, api_key) case-insensitively with IllegalArgumentException, and truncates message text to 200 characters. Six concrete subclasses bind their fixed HTTP status: ResourceNotFoundException (404), BusinessRuleException (409), ValidationFailedException (400), TenancyViolationException (403, always CROSS_TENANT_ACCESS_DENIED), AuthenticationFailedException (401), RateLimitExceededException (429). ResourceNotFoundException.of(String resourceType, Object id) reproduces the exact legacy RuntimeException message format ('Customer not found with id: 7') for zero-grep-churn compatibility during the WO-022 service migration; a null id safely produces '... with id: null'. ApiExceptionTest has 30+ @Tag('unit') tests. ExceptionFixtures builds one sample instance of every exception type for WO-021 and WO-025 reuse.

## WO-019: User Story: WO-019 - Externalize Compose Secrets And Delete Committed Credentials
- **Status:** completed
- **Commit:** `e0ae5df`
- **Files:** 11 (+392/-97)
- **Duration:** 534ss
- **Approach:** Infrastructure-only change: no Java behavior modified. (1) docker-compose.yml fully rewritten for Compose V2: version key removed, all credential literals replaced with ${VAR:?} interpolation (POSTGRES_PASSWORD uses :? to fail loudly when absent), postgres port bound to 127.0.0.1:5432:5432, top-level secrets block added with file-based secrets in ./secrets/, backend service mounts spring.datasource.password and app.jwt.secret secrets, APP_JWT_SECRET and APP_JWT_EXPIRATION_MS explicitly supplied, SPRING_CONFIG_IMPORT set to optional:configtree:/run/secrets/. (2) keystone-backend/docker-compose.yml deleted — it was 100% commented-out dead config still containing the robin8181 password literal. (3) application.properties: spring.config.import=optional:configtree:/run/secrets/ added; optional: prefix ensures env-var-only environments (CI, local Java) start without the directory. (4) .env.example expanded with POSTGRES_DB, POSTGRES_USER, VITE_API_BASE_URL and updated SPRING_DATASOURCE_URL to use the postgres service name for Docker Compose; bootstrap script referenced. (5) .gitignore: secrets/ entry added. (6) scripts/dev/bootstrap-secrets.sh: generates .env from .env.example with openssl rand -hex 32 for JWT key and -hex 16 for DB password; creates secrets/ directory with property-key-named files; idempotent with --force flag; Windows PowerShell equivalent documented inline. (7) scripts/ci/compose-smoke.sh: CI smoke test verifying compose fails without .env, stack health, backend response, and absence of committed credentials in docker inspect environment. (8) Fixture configtree directory committed with clearly placeholder (fixture-prefixed) values. (9) ConfigtreeBindingTest: @Tag('unit') tests asserting fixture files exist with expected content, trailing-newline trimming, empty file detection, and property-key file naming convention. (10) README.md: replaced Step 1 database creation with bootstrap script workflow, fixed stale 'edit application.properties' instructions.
