# Dependency Inventory — fsm-keystone

**Date:** 2026-08-10
**Sources:** `frontend/package.json`, `frontend/package-lock.json` (lockfileVersion 3), `keystone-backend/pom.xml`

---

## Frontend — npm

### Declared vs. Resolved (direct dependencies)

| Package | Declared specifier | Lockfile resolved | Risk |
|---|---|---|---|
| `@vitejs/plugin-react` | `latest` | `6.0.3` | **HIGH** — unbounded; any rebuild can pull v7+ |
| `axios` | `latest` | `1.18.1` | **HIGH** — unbounded |
| `lucide-react` | `latest` | `1.25.0` | **HIGH** — unbounded |
| `react` | `latest` | `19.2.7` | **HIGH** — unbounded; React 20 would break APIs |
| `react-dom` | `latest` | `19.2.7` | **HIGH** — unbounded |
| `vite` | `latest` | `8.1.5` | **HIGH** — unbounded |
| `react-icons` | `^5.7.0` | `5.7.0` | LOW — semver-bounded to minor |

> **Finding F-01:** Six of seven declared dependencies use `latest` specifier.
> Any `npm install` (without `--prefer-offline`) can resolve a breaking major version.
> The frontend Dockerfile uses `npm install` (not `npm ci`), so a CI image build is
> additionally unprotected by the lockfile.

### Key transitive resolved versions

| Package | Version | Notes |
|---|---|---|
| `rolldown` | `1.1.5` | Vite 8 bundler (optional peer dep) |
| `scheduler` | `0.27.0` | React internal scheduler |
| `lightningcss` | `1.32.0` | Vite CSS processor (optional) |
| `postcss` | `8.5.19` | CSS transform pipeline |
| `axios` → `follow-redirects` | `1.16.0` | HTTP redirect handler |

### Node.js engine floor

Transitive packages declare `engines.node = "^20.19.0 || >=22.12.0"`:
- `@vitejs/plugin-react` 6.0.3
- All `@rolldown/binding-*` platform bindings (optional, 15 packages)

This means Node 20 < 20.19.0 will fail at install time. The Node upgrade target must be **≥ Node 20.19.0** (minimum) or **Node 22.12.0 / Node 24** (recommended). See `runtime-support-matrix.md`.

### SBOM

Full machine-readable CycloneDX SBOM: `docs/baseline/sbom/frontend-sbom.json` (79 components: 46 required, 33 optional platform bindings).

---

## Backend — Maven

### Direct dependencies (from `keystone-backend/pom.xml`)

| Artifact | Group | Declared version | Version managed by | Scope |
|---|---|---|---|---|
| `spring-boot-starter-data-jpa` | `org.springframework.boot` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | compile |
| `spring-boot-starter-security` | `org.springframework.boot` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | compile |
| `spring-boot-starter-validation` | `org.springframework.boot` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | compile |
| `spring-boot-starter-webmvc` | `org.springframework.boot` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | compile |
| `postgresql` | `org.postgresql` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | runtime |
| `lombok` | `org.projectlombok` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | compile (optional) |
| `jjwt-api` | `io.jsonwebtoken` | `0.12.6` | explicit | compile |
| `jjwt-impl` | `io.jsonwebtoken` | `0.12.6` | explicit | runtime |
| `jjwt-jackson` | `io.jsonwebtoken` | `0.12.6` | explicit | runtime |
| `spring-boot-starter-data-jpa-test` | `org.springframework.boot` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | test |
| `spring-boot-starter-security-test` | `org.springframework.boot` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | test |
| `spring-boot-starter-validation-test` | `org.springframework.boot` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | test |
| `spring-boot-starter-webmvc-test` | `org.springframework.boot` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | test |
| `spring-boot-testcontainers` | `org.springframework.boot` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | test |
| `testcontainers:postgresql` | `org.testcontainers` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | test |
| `testcontainers:junit-jupiter` | `org.testcontainers` | — (parent-managed) | `spring-boot-starter-parent:4.0.7` | test |

**Parent:** `org.springframework.boot:spring-boot-starter-parent:4.0.7`
**Java version property:** `java.version = 21`

> **Finding B-01:** Maven parent-managed versions are pinned through the `spring-boot-starter-parent`
> BOM — these update atomically with the Spring Boot version and are safe from individual drift.
> Only the JJWT library (`0.12.6`) is pinned outside the BOM and must be tracked independently.

### Resolved transitive tree

Full resolution requires `mvn dependency:tree` — not available in this environment.
Command to generate (see `docs/runbooks/dependency-baseline.md`):
```bash
cd keystone-backend && mvn dependency:tree -DoutputFile=../docs/baseline/sbom/backend-dependency-tree.txt
```

### SBOM

Direct-dependency CycloneDX SBOM (16 components): `docs/baseline/sbom/backend-sbom.json`
Note: managed-version fields show `managed-by-spring-boot-4.0.7`; regenerate with the
CycloneDX Maven plugin to get exact resolved coordinates (see runbook).
