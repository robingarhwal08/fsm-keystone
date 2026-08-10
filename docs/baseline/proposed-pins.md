# Proposed Dependency and Image Pins — fsm-keystone

**Date:** 2026-08-10
**Status:** Proposal only — no production manifest has been modified in this document.
**Diff verification:** `git diff -- frontend/package.json frontend/Dockerfile keystone-backend/Dockerfile docker-compose.yml keystone-backend/docker-compose.yml` must show zero changes.

---

## Frontend npm Specifiers

### Current → Proposed

| Package | Current specifier | Proposed specifier | Rationale | Risk |
|---|---|---|---|---|
| `react` | `latest` | `19.2.7` | Pin to lockfile-resolved version | React 20 is pre-release; `latest` could pull it on next install |
| `react-dom` | `latest` | `19.2.7` | Keep in sync with `react` | Same risk as above; must match `react` exactly |
| `vite` | `latest` | `8.1.5` | Pin to lockfile-resolved version | Vite 9 may change config API; CI would silently break on the next build |
| `@vitejs/plugin-react` | `latest` | `6.0.3` | Pin to lockfile-resolved version | Major version bump could break Vite config |
| `axios` | `latest` | `1.18.1` | Pin to lockfile-resolved version | Breaking API changes in axios v2 range |
| `lucide-react` | `latest` | `1.25.0` | Pin to lockfile-resolved version | Icon set breaking changes (renamed icons) |
| `react-icons` | `^5.7.0` | `^5.7.0` | No change — semver-bounded within minor | Low risk; upgrade within 5.x patch |

After applying the pins:
```bash
# Verify lockfile unchanged (should show no diff):
cd frontend && npm ci --prefer-offline
git diff frontend/package-lock.json
```

---

## Dockerfile Base Images

### frontend/Dockerfile — Build Stage

| Stage | Current image | Proposed image | Digest (example — verify at pinning time) | Rationale | Risk |
|---|---|---|---|---|---|
| build | `node:20-alpine` | `node:24-alpine` | `node:24-alpine@sha256:<run: docker pull node:24-alpine && docker inspect --format '{{index .RepoDigests 0}}' node:24-alpine>` | Node 20 EOL; Node 24 Active LTS to Apr 2028 | Engine floor `>=22.12.0` satisfied by Node 24; `@rolldown/binding-*` install passes |
| runtime | `nginx:alpine` | `nginx:1.28-alpine` | `nginx:1.28-alpine@sha256:<run: docker pull nginx:1.28-alpine && docker inspect --format '{{index .RepoDigests 0}}' nginx:1.28-alpine>` | Stabilize on a specific Nginx minor | Low — Nginx config is static |

Additional recommendation for build stage: switch from `npm install` to `npm ci` to honour the lockfile:
```dockerfile
# PROPOSED change (not applied in this PR):
RUN npm ci --prefer-offline
```

### keystone-backend/Dockerfile — Runtime Stage

| Stage | Current image | Proposed image | Digest (example) | Rationale | Risk |
|---|---|---|---|---|---|
| build | `maven:3.9.8-eclipse-temurin-21` | `maven:3.9.9-eclipse-temurin-21` | `<pin after pull>` | Bring Maven to latest 3.9.x patch | Low — Maven 3.9.x is backward compatible |
| runtime | `eclipse-temurin:21-jdk` | `eclipse-temurin:21-jre` | `<pin after pull>` | JRE sufficient at runtime; removes ~260 MB and compiler from the running container | Low — Spring Boot fat JAR needs only JRE; verify with `java -jar app.jar` smoke test |

### Digest pinning procedure

For each image:
```bash
# Pull and record the current digest:
docker pull <image>:<tag>
docker inspect --format '{{index .RepoDigests 0}}' <image>:<tag>
# Returns: <image>@sha256:<hash>
# Use this in FROM statements:
# FROM node:24-alpine@sha256:<hash> AS build
```

---

## docker-compose.yml Proposed Changes

| Finding | Current state | Proposed change | Risk |
|---|---|---|---|
| C-01 | `version: "3.9"` present | Remove the `version:` key entirely | None — modern Compose ignores it |
| C-02 | `postgres.ports: "5432:5432"` | Change to `127.0.0.1:5432:5432` or remove for production | Low — dev environments may rely on host access |
| C-03 | No application healthchecks | Add `healthcheck: test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]` to `backend` | Medium — requires Spring Actuator to be on classpath |
| C-04 | Hardcoded passwords | Move to `.env` file (see `docs/baseline/required-properties.md`) | Low — config-only change |
| C-05 | Duplicate definitions in `keystone-backend/docker-compose.yml` | Remove the file (or convert to a dev-only override) | Low — file is already commented out |

---

## Notes

1. This document must not be applied until `docs/baseline/secrets-inventory.md` rotation runbook has been executed (credentials change on the same PR as the compose update).
2. The Node upgrade (`node:20-alpine` → `node:24-alpine`) should be verified by building the frontend Docker image and running `npm test` before merging.
3. The JDK→JRE switch should be verified by running `docker run --rm fsm-backend:jre-test java -jar app.jar` and confirming the application starts.
4. Digest values shown as `<pin after pull>` — fill these in at the time of the pinning commit. Do not commit digests that you have not personally verified.
