# Required Properties Manifest

_Every environment variable consumed by `keystone-backend/src/main/resources/application.properties`._
_This manifest is the pre-condition check before removing `${ENV:literal}` fallbacks in Phase 2._

---

## How to Use This Manifest

Before removing any `${ENV:literal}` fallback from `application.properties`, verify that **every
environment marked "Required" in the target column for that environment is provisioned**.
Run the default-value guard test (see `DefaultValueGuardTest`) against a dry-run config to confirm.

A value of **REQUIRED-NO-DEFAULT** means the application must not start without this variable. After
Phase 2, Spring Boot will throw `IllegalStateException` on startup if it is absent — which is the
desired fail-fast behaviour.

---

## Manifest Table

| Property Key | Env Variable | Current Fallback | Classification | Required? | Dev | Staging | Prod | Notes |
|---|---|---|---|---|---|---|---|---|
| `spring.application.name` | `SPRING_APPLICATION_NAME` | `keystone` | Internal | Optional | ✓ optional | ✓ optional | ✓ optional | Safe default; override for log correlation |
| `server.port` | `PORT` | `8080` | Internal | Optional | ✓ optional | ✓ required | ✓ required | Port 80/443 in prod via reverse proxy |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/fsmdb` | Confidential | Required | ✓ required | ✓ required | ✓ required | Localhost default unusable in containers |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `postgres` | Confidential | Required | ✓ required | ✓ required | ✓ required | Default value is low-risk but must be explicit |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `robin8181` ⚠️ | **Restricted** | **REQUIRED-NO-DEFAULT** | ✓ required | ✓ required | ✓ required | **Exposed in public repo — rotate immediately** |
| `spring.jpa.hibernate.ddl-auto` | `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Internal | Optional | ✓ optional | ✓ required | ✓ required | Prod must be `validate`; Flyway manages schema |
| `spring.jpa.show-sql` | `SPRING_JPA_SHOW_SQL` | `true` | Internal | Optional | ✓ optional | ✓ required | ✓ required | Must be `false` in staging/prod |
| `spring.jpa.properties.hibernate.format_sql` | `SPRING_JPA_FORMAT_SQL` | `true` | Internal | Optional | ✓ optional | ✓ optional | ✓ optional | Dev convenience only |
| `app.jwt.secret` | `APP_JWT_SECRET` | `8b7a0c7e...` ⚠️ | **Restricted** | **REQUIRED-NO-DEFAULT** | ✓ required | ✓ required | ✓ required | **Exposed in public repo — rotate immediately; min 64-char hex** |
| `app.jwt.expiration-ms` | `APP_JWT_EXPIRATION_MS` | `86400000` (24 h) | Internal | Optional | ✓ optional | ✓ required | ✓ required | Prod should be ≤ 900000 (15 min) per architecture target |
| `app.cors.allowed-origin` | `APP_CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | Internal | Required | ✓ required | ✓ required | ✓ required | Must match frontend origin; localhost is wrong in non-dev |

---

## Variables Flagged REQUIRED-NO-DEFAULT

These two variables must have their literal fallbacks removed in Phase 2. Until then, the
`DefaultValueGuardTest` will report them as known violations (count = 2).

```
APP_JWT_SECRET          → app.jwt.secret
SPRING_DATASOURCE_PASSWORD → spring.datasource.password
```

**After Phase 2 removal**, the test assertion must be updated from `knownViolationCount == 2`
to `knownViolationCount == 0` to promote the guard to a blocking gate.

---

## Frontend Variables

| Variable | File | Default | Classification | Notes |
|---|---|---|---|---|
| `VITE_API_BASE_URL` | `frontend/.env` / `frontend/src/api/axiosConfig.js` | `http://localhost:8080/api` | Internal | Set to the actual backend URL per environment; not a secret |

---

## Validation Pre-Condition (before fallback removal)

Run the following against each target environment and confirm each "Required" entry is set:

```bash
# Verify environment has all required variables
for var in SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD \
           APP_JWT_SECRET APP_CORS_ALLOWED_ORIGIN; do
  if [ -z "${!var:-}" ]; then
    echo "MISSING: $var"
  else
    echo "OK:      $var"
  fi
done
```

All required variables must print `OK` before removing fallbacks. Any `MISSING` line is a
blocker — the application will fail to start after fallback removal.
