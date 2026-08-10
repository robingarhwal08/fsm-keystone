# ADR-0004: Secrets via Docker Compose secrets and manual rotation runbook

| Field  | Value      |
|--------|------------|
| Status | Accepted   |
| Date   | 2026-08-10 |

## Context

The original repository committed a cleartext database password (`robin8181`) and a JWT signing key as literal defaults in `keystone-backend/src/main/resources/application.properties`. These values were accessible to anyone with read access to the repository and are now considered compromised. The project required a secrets strategy that:

1. Removes committed credentials from the repository.
2. Works in local development without external services.
3. Works in Docker Compose without operator manual steps beyond an initial bootstrap.
4. Has a documented rotation procedure for production incidents.

Spring Boot 4's `configtree` property import reads flat files from a directory tree and binds them to property keys by filename, making Docker Compose file-based secrets a natural fit: the runtime mounts `/run/secrets/<property-key>` and Spring Boot reads it transparently.

## Decision

We will use Docker Compose file-based secrets (`secrets:` block in `docker-compose.yml`) combined with a git-ignored `.env` file and a `bootstrap-secrets.sh` script for local development. The backend container receives `spring.datasource.password` and `app.jwt.secret` as mounted files under `/run/secrets/`. Spring Boot reads them via `spring.config.import=optional:configtree:/run/secrets/`. A manual rotation runbook documents the steps for secret rotation. No external secrets manager is introduced at this stage.

## Consequences

- **Positive**: no secret value appears in the repository, in `docker inspect` environment output, or in application logs.
- **Positive**: local development requires only `bash scripts/dev/bootstrap-secrets.sh`; no cloud credentials needed.
- **Positive**: the `optional:` prefix on the configtree import allows the application to start in non-Docker environments using environment variables.
- **Negative**: rotation requires manual steps (new file → rolling restart); there is no automatic propagation or zero-downtime rotation without additional tooling.
- **Negative**: file-based secrets are Docker Compose syntax (not Docker Swarm); moving to Swarm or Kubernetes requires migration of the secret-mounting mechanism.
- **Ongoing discipline**: developers must never add default secret values back to `application.properties`; the `DefaultValueGuardTest` catches this automatically.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| HashiCorp Vault | Correct long-term solution for dynamic secrets and automatic rotation. Adds an infrastructure dependency; not viable for a small team in the early programme phase. Deferred to the secrets-management epic. |
| AWS Secrets Manager | Requires AWS credentials at runtime; adds cloud dependency that complicates local development. Deferred alongside Vault. |
| Environment variables only (no files) | Works for CI/CD pipelines but exposes secrets via `docker inspect` environment output and process listings; the `scripts/ci/compose-smoke.sh` smoke test already asserts `robin8181` is absent from container inspect output. |
| `.env` file committed with placeholder values | The file is git-ignored; committing it with real values is a credential leak. `.env.example` is committed as the template. |

## Evidence

```
docker-compose.yml — secrets: block mounts spring.datasource.password and app.jwt.secret
  from ./secrets/<name> files; backend.secrets: lists both secret names.

keystone-backend/src/main/resources/application.properties:8
  — spring.config.import=optional:configtree:/run/secrets/

.gitignore — secrets/ entry prevents secret files from being committed.

scripts/dev/bootstrap-secrets.sh — generates .env and ./secrets/ files with openssl rand.

docs/runbooks/secret-rotation.md — manual rotation procedure for production incidents.

keystone-backend/src/test/java/com/fsm/keystone/config/DefaultValueGuardTest.java
  — asserts that application.properties contains no default secret values.
```

See also: [docs/runbooks/secret-rotation.md](../runbooks/secret-rotation.md)

## Related ADRs

- [ADR-0001](0001-default-deny-security-filter-chain.md) — STATELESS session management that makes the JWT secret the sole authentication credential.
- [ADR-0009](0009-jwt-token-ttl-and-refresh-strategy.md) — the JWT TTL policy that limits the blast radius of a compromised signing key.
