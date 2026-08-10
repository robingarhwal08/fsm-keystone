# Runtime and Base Image Support Matrix — fsm-keystone

**Date:** 2026-08-10
**Policy reference:** End-of-life dates sourced from endoflife.date, Node.js release schedule, and Eclipse Temurin support calendar.

---

## Runtime Versions

| Runtime | Version in use | Status | End of support | Proposed target | Notes |
|---|---|---|---|---|---|
| **Node.js** (frontend build) | **20** | **End of Life** | **30 Apr 2026** | **Node 24 (Active LTS)** | EOL as of 2026-08-10; receives no security patches. Node 22 is minimum viable upgrade (Maintenance LTS to Apr 2027); Node 24 is recommended (Active LTS to Apr 2028). Constrained by engine floor: `^20.19.0 \|\| >=22.12.0` |
| **Node.js** (runtime in container) | 20-alpine | **End of Life** | **30 Apr 2026** | **node:24-alpine** | Same as build stage; runtime stage serves via Nginx, not Node |
| **Java** | **21** | Active LTS | ~Sep 2028 | Retain Java 21 | Deliberate retention — avoid bundling JDK major change with security cutover |
| **Spring Boot** | **4.0.7** | Current minor | ~12 months OSS support from release | Monitor for 4.1.x minor | No LTS designation; each minor supported ~12 months |
| **PostgreSQL** | **16** (compose image) | Supported | Nov 2028 | Retain PostgreSQL 16 | Currently in active support window |
| **Maven** (build stage) | `3.9.8` (Dockerfile) | Stable | N/A | Retain 3.9.x | Part of `maven:3.9.8-eclipse-temurin-21` build image |

---

## Docker Base Images

| Image | Tag | Used in | Status | EOL / Support end | Proposed target |
|---|---|---|---|---|---|
| `node` | `20-alpine` | `frontend/Dockerfile` (build stage) | **EOL** | **30 Apr 2026** | `node:24-alpine` or `node:24-alpine3.20` with digest pin |
| `nginx` | `alpine` (floating) | `frontend/Dockerfile` (runtime stage) | Supported | Tracks Alpine; no EOL date | `nginx:1.28-alpine` or latest stable with digest pin |
| `maven` | `3.9.8-eclipse-temurin-21` | `keystone-backend/Dockerfile` (build stage) | Supported | Java 21 ~Sep 2028 | Add digest pin; consider switching to `eclipse-temurin:21-jdk-alpine` base |
| `eclipse-temurin` | `21-jdk` | `keystone-backend/Dockerfile` (runtime stage) | Supported | ~Sep 2028 | **Switch to `eclipse-temurin:21-jre`** (full JDK not needed at runtime); add digest pin |
| `postgres` | `16` | `docker-compose.yml` | Supported | Nov 2028 | Add digest pin; consider `postgres:16-alpine` |

> **Finding I-01 (Node EOL):** The frontend build uses `node:20-alpine` which reached end of life
> on 30 Apr 2026 and receives no further CVE patches. All Docker image builds from the current
> Dockerfile inherit this vulnerability surface.
>
> **Finding I-02 (Full JDK at runtime):** `eclipse-temurin:21-jdk` ships the full JDK (~450 MB),
> including `javac`, `jlink` and the debugger. The runtime only needs the JRE (~190 MB).
> Switching to `eclipse-temurin:21-jre` reduces the attack surface by ~260 MB and removes the
> compiler from the running container.
>
> **Finding I-03 (No digest pins):** Neither Dockerfile nor `docker-compose.yml` pins any image
> by digest (`@sha256:...`). A `docker pull` or `docker-compose up` can silently resolve a
> different image if the tag is mutated upstream.
>
> **Finding I-04 (Root user):** Both runtime stages (`nginx:alpine` and `eclipse-temurin:21-jdk`)
> run as root. Neither Dockerfile adds a `USER` directive.

---

## Node.js Engine Floor

The resolved lockfile constrains the Node upgrade:

```
@vitejs/plugin-react 6.0.3 → engines.node: "^20.19.0 || >=22.12.0"
@rolldown/binding-* (15 packages) → engines.node: "^20.19.0 || >=22.12.0"
```

**Implication:** Node 20.0.0–20.18.x will fail on `npm install`. The current `node:20-alpine`
image ships Node 20.19.x (the latest 20.x patch line) which satisfies the constraint, but any
Node 20 image older than 20.19.0 will fail. The minimum safe upgrade target is **Node 22.12.0**;
the recommended target is **Node 24 (any release)**.

---

## Support Window Calendar

```
            2026  2027  2028  2029
Node 20     ████▒             (EOL Apr 2026)
Node 22     ██████████▒       (Maintenance LTS to Apr 2027)
Node 24     ████████████████  (Active LTS to Apr 2028)
Java 21     ████████████████████ (LTS ~Sep 2028)
Spring Boot 4.x  ████████     (~12 mo per minor)
PostgreSQL 16    ████████████ (Nov 2028)

█ = active support  ▒ = maintenance/security-only  (blank) = EOL
```
