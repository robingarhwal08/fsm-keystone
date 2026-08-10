# Secrets Exposure Inventory

_Phase 0 baseline — generated before any credential rotation or fallback removal._
_All values shown are confirmed **compromised** because this repository is public._

---

## Summary

| Secret | Classification | Status | Owner |
|---|---|---|---|
| JWT signing key (`APP_JWT_SECRET`) | Restricted | **Compromised — committed to public repo** | Platform Engineering |
| Database password (`SPRING_DATASOURCE_PASSWORD`) | Restricted | **Compromised — committed to public repo** | Platform Engineering |
| Database username (`SPRING_DATASOURCE_USERNAME`) | Confidential | Committed (low-sensitivity default) | Platform Engineering |
| PostgreSQL Compose password | Restricted | **Compromised — committed to public repo** | Platform Engineering |
| Frontend API base URL (`VITE_API_BASE_URL`) | Internal | Committed (local dev endpoint, low risk) | Frontend Engineering |

**Git history note:** Even after the fallback literals are deleted from these files, the values remain in git history. Because the repository is public, history rewriting does **not** substitute for rotation — every exposed credential must be rotated regardless of whether the file is updated.

---

## Detailed Findings

### Finding 1 — JWT Signing Key Literal Fallback

| Field | Value |
|---|---|
| **File** | `keystone-backend/src/main/resources/application.properties` |
| **Line** | 18 |
| **Property** | `app.jwt.secret` |
| **Environment variable** | `APP_JWT_SECRET` |
| **Pattern** | `${APP_JWT_SECRET:<64-char-hex>}` |
| **Classification** | **Restricted** |
| **Exposure** | Committed to public repository; value present in git history |
| **Impact** | Anyone can forge valid MANAGER-role JWTs for any email address |
| **Required action** | Rotate per `docs/runbooks/secret-rotation.md`; remove fallback in Phase 2 |

### Finding 2 — Database Password Literal Fallback

| Field | Value |
|---|---|
| **File** | `keystone-backend/src/main/resources/application.properties` |
| **Line** | 8 |
| **Property** | `spring.datasource.password` |
| **Environment variable** | `SPRING_DATASOURCE_PASSWORD` |
| **Pattern** | `${SPRING_DATASOURCE_PASSWORD:robin8181}` |
| **Classification** | **Restricted** |
| **Exposure** | Committed to public repository; value present in git history |
| **Impact** | Anyone can connect to the PostgreSQL instance if the default port is accessible |
| **Required action** | Rotate per `docs/runbooks/secret-rotation.md`; remove fallback in Phase 2 |

### Finding 3 — Docker Compose Hardcoded Password (root)

| Field | Value |
|---|---|
| **File** | `docker-compose.yml` |
| **Lines** | 15, 45 |
| **Variables** | `POSTGRES_PASSWORD`, `SPRING_DATASOURCE_PASSWORD` |
| **Classification** | **Restricted** |
| **Exposure** | Committed to public repository |
| **Impact** | Identical to Finding 2; confirms the password is the same across deployment configs |
| **Required action** | Replace inline values with `${SPRING_DATASOURCE_PASSWORD}` or Docker/Compose secrets reference in Phase 2 |

### Finding 4 — Docker Compose Hardcoded Password (commented-out backend compose)

| Field | Value |
|---|---|
| **File** | `keystone-backend/docker-compose.yml` |
| **Lines** | 12, 41 |
| **Variables** | `POSTGRES_PASSWORD`, `SPRING_DATASOURCE_PASSWORD` |
| **Classification** | **Restricted** |
| **Note** | File is fully commented out (inert) but the values are present in plain text |
| **Exposure** | Committed to public repository |
| **Required action** | Remove values even in commented code; apply same rotation as Findings 2–3 |

### Finding 5 — Frontend API Base URL

| Field | Value |
|---|---|
| **File** | `frontend/.env` |
| **Line** | 1 |
| **Variable** | `VITE_API_BASE_URL` |
| **Value** | `http://localhost:8080/api` (local dev endpoint) |
| **Classification** | Internal |
| **Exposure** | Committed; however this is a local dev URL with no credential |
| **Required action** | Add `frontend/.env` to `.gitignore`; use `frontend/.env.example` as template |

---

## Gitleaks Scan Baseline

**Scan command (run from repository root):**
```bash
gitleaks detect --config=.gitleaks.toml --source=. --report-format=json --report-path=/tmp/gitleaks-report.json
```

**Current known findings** (run against HEAD with `.gitleaks.toml` committed in this story):

| Rule ID | File | Description |
|---|---|---|
| `jwt-hex64-signing-key` | `keystone-backend/src/main/resources/application.properties:18` | 64-char hex JWT signing key as property default |
| `postgres-password-assignment` | `docker-compose.yml:15` | POSTGRES_PASSWORD inline assignment |
| `postgres-password-assignment` | `docker-compose.yml:45` | SPRING_DATASOURCE_PASSWORD inline assignment |
| `postgres-password-assignment` | `keystone-backend/docker-compose.yml:12` | POSTGRES_PASSWORD inline (commented-out compose) |
| `postgres-password-assignment` | `keystone-backend/docker-compose.yml:41` | SPRING_DATASOURCE_PASSWORD inline (commented-out compose) |
| `jdbc-inline-credential` | `keystone-backend/src/main/resources/application.properties:8` | datasource.password with literal fallback |

Future scans must be diffed against this baseline; any new finding is a blocking gate failure in the Forge Shipping pipeline once the scan stage is activated (Phase 2 follow-up).

---

## Follow-Up Actions (Owned by Later Phases)

| Action | Phase | Blocker? |
|---|---|---|
| Rotate JWT signing key (re-generate, inject via env, dual-key window) | Phase 0 operational | Yes before any hardening phase |
| Rotate database password (compose + env injection) | Phase 0 operational | Yes before any hardening phase |
| Delete `${ENV:literal}` fallbacks from `application.properties` | Phase 2 | Yes — dependent on rotation above |
| Promote default-value guard to a blocking assertion (`violations == 0`) | Phase 2 | After fallback removal |
| Enable gitleaks as a blocking gate in Forge Shipping scan stage | Phase 2 | After rotation confirmed |
| Add `frontend/.env` to `.gitignore` | Phase 0 (this story) | Done |
| Remove inline values from `keystone-backend/docker-compose.yml` | Phase 2 | After rotation |
