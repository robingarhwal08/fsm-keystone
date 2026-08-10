# Container Image and Compose Baseline — fsm-keystone

**Date:** 2026-08-10
**Sources:** `frontend/Dockerfile`, `keystone-backend/Dockerfile`, `docker-compose.yml`, `keystone-backend/docker-compose.yml`

---

## Dockerfile Inventory

### frontend/Dockerfile

| Stage | Base image | Tag | Digest pinned | User | Port exposed | Notable build flags |
|---|---|---|---|---|---|---|
| build | `node` | `20-alpine` | **No** | root | — | `npm install` (not `npm ci`) |
| runtime | `nginx` | `alpine` (floating) | **No** | root | `80` | — |

**Full Dockerfile:**
```dockerfile
# Stage 1 - Build React App
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2 - Serve using Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Findings:**
- `node:20-alpine` is End of Life (30 Apr 2026) — **no security patches**
- `npm install` resolves from registry on each build; `npm ci` would respect the lockfile
- `nginx:alpine` uses a floating `alpine` tag — can resolve a new nginx major silently
- No `USER` directive — Nginx runs as root; a `USER nginx` would reduce blast radius
- No image healthcheck at the application level (HTTP probe)

**Build measurement:** Docker not run in this environment (sandbox constraint).
To measure: `time docker build -t fsm-frontend ./frontend && docker images fsm-frontend`

---

### keystone-backend/Dockerfile

| Stage | Base image | Tag | Digest pinned | User | Port exposed | Notable build flags |
|---|---|---|---|---|---|---|
| build | `maven` | `3.9.8-eclipse-temurin-21` | **No** | root | — | `mvn clean package -DskipTests` |
| runtime | `eclipse-temurin` | `21-jdk` | **No** | root | `8080` | — |

**Full Dockerfile:**
```dockerfile
# Build Stage
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

**Findings:**
- `-DskipTests` skips all unit and integration tests — a production image can be built from untested code
- `eclipse-temurin:21-jdk` ships the full JDK (~450 MB); runtime only needs JRE (~190 MB) — see Finding I-02
- No `USER` directive — Spring Boot runs as root
- No image healthcheck at the application level (HTTP probe to `/actuator/health`)
- No digest pins on either stage

**Build measurement:** Docker not run in this environment (sandbox constraint).
To measure: `time docker build -t fsm-backend ./keystone-backend && docker images fsm-backend`

---

## Docker Compose Inventory

### docker-compose.yml (root — active)

| Service | Image / build context | Base image | Published ports | Healthcheck | User |
|---|---|---|---|---|---|
| `postgres` | `postgres:16` (pull) | `postgres:16` | `5432:5432` (host-published) | YES (`pg_isready`) | postgres (default) |
| `backend` | `./keystone-backend` (build) | `eclipse-temurin:21-jdk` | `8080:8080` | NO application-level | root |
| `frontend` | `./frontend` (build) | `nginx:alpine` | `3000:80` | NO application-level | root |

```yaml
version: "3.9"   # ← obsolete key (removed in Compose Spec)
```

**Findings:**
- **Finding C-01 (Obsolete `version` key):** `version: "3.9"` is deprecated and ignored by
  modern Docker Compose; it should be removed per the current Compose specification.
- **Finding C-02 (PostgreSQL host port published):** `5432:5432` exposes the database port on
  the host network. On a server this is a direct database access vector from any process on
  the host. The port binding should be removed or restricted to `127.0.0.1:5432:5432`.
- **Finding C-03 (Missing application healthchecks):** `backend` and `frontend` services have no
  `healthcheck` directive. `postgres` has one (`pg_isready`), but the application services lack
  HTTP probes, so `depends_on: condition: service_healthy` cannot be applied to them.
- **Finding C-04 (Hardcoded credentials):** `POSTGRES_PASSWORD: robin8181` and
  `SPRING_DATASOURCE_PASSWORD: robin8181` are committed in plaintext.
  Documented as compromised in `docs/baseline/secrets-inventory.md`.

---

### keystone-backend/docker-compose.yml (fully commented out — inert)

This file is fully commented out but still resides in the repository and duplicates service
definitions from the root Compose file.

| Service (commented) | Image | Published ports | Notes |
|---|---|---|---|
| `postgres` | `postgres:16` | `5432:5432` | Duplicate of root file; different service name (`keystone-postgres` vs `postgres-db`) |
| `springboot` | `./keystone-backend` (build) | `8080:8080` | Duplicate of root file; different service name (`keystone-app` vs `springboot-app`) |

**Finding C-05 (Duplicated Compose definitions):** `keystone-backend/docker-compose.yml`
duplicates the PostgreSQL and backend service definitions. Even in commented form, it carries
the same compromised credentials. The file should either be removed or converted to a
developer-only override file (`docker-compose.override.yml`) after the credentials are rotated.

---

## Image Build Measurement

Image builds were **not executed** in this environment (sandbox constraint — builds would take 5–15 minutes and consume significant disk).

To record sizes and durations locally, run:
```bash
# Frontend
time docker build --no-cache -t fsm-frontend:baseline ./frontend 2>&1 | tee /tmp/frontend-build.log
docker images fsm-frontend:baseline --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"

# Backend
time docker build --no-cache -t fsm-backend:baseline ./keystone-backend 2>&1 | tee /tmp/backend-build.log
docker images fsm-backend:baseline --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
```

Expected size ranges based on base image documentation:
- `fsm-frontend` (nginx:alpine + React dist): ~40–80 MB
- `fsm-backend` (eclipse-temurin:21-jdk + fat JAR): ~550–650 MB (full JDK); ~280–380 MB if switched to JRE

Update this document with measured values after running the above commands.
