# ADR-0005: Forge Shipping as the delivery platform

| Field  | Value      |
|--------|------------|
| Status | Accepted   |
| Date   | 2026-08-10 |

## Context

The repository has no CI/CD pipeline configuration at all. There is no Jenkinsfile, `.github/workflows/`, `.gitlab-ci.yml`, or equivalent. The modernization programme requires a delivery platform that can gate pull requests on test results, run schema migration dry-runs before deployment, enforce the docs link check, and publish container images. The choice is a greenfield decision — there is no existing pipeline to migrate.

## Decision

We will use Forge Shipping as the delivery platform. Forge Shipping manages the work-order sequencing for this programme and will be extended to run automated gates at each stage boundary. The following gate groups are planned:

| Stage | Gate |
|-------|------|
| Pre-merge | `mvn test` (unit + slice), ArchUnit inventory |
| Pre-deploy | `mvn verify` (integration, Testcontainers), docs link check |
| Post-deploy | Compose smoke test (`scripts/ci/compose-smoke.sh`), schema drift check |
| Release | Container image scan, SBOM snapshot |

## Consequences

- **Positive**: pipeline configuration is co-located with the work-order programme; no separate CI system to maintain.
- **Positive**: Forge Shipping's work-order sequencing prevents dependent stories from shipping before their prerequisites are gated.
- **Positive**: existing scripts (`scripts/ci/check-docs-links.sh`, `scripts/ci/compose-smoke.sh`) are already written to be invoked as shell commands, fitting the Forge Shipping step model.
- **Negative**: Forge Shipping is the sole orchestrator; if it is unavailable, the gating mechanism is unavailable. A fallback CI configuration (e.g. GitHub Actions) should be added in a later work order.
- **Ongoing discipline**: every work order that introduces a new automated check must document the corresponding Forge Shipping gate in the work-order description.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| GitHub Actions | Standard and familiar, but requires the team to own workflow YAML in addition to Forge Shipping work orders — duplication of orchestration. Can be added as a fallback later. |
| Jenkins | Requires self-hosted infrastructure to run; operational overhead not warranted at programme stage. |
| GitLab CI | Not the repository host; would require mirroring or token-based cross-platform access. |
| No CI pipeline | Unacceptable: the programme's SOC 2 compliance objective requires automated evidence of test execution and schema governance. |

## Evidence

```
No pipeline configuration file exists in the repository at the time of this decision.
  — confirmed by: find /path -name "Jenkinsfile" -o -name "*.yml" in .github/workflows; none found.

scripts/ci/compose-smoke.sh — smoke-test script already structured as a Forge Shipping step.
scripts/ci/check-docs-links.sh — docs link-check script (created in this work order).
```

## Related ADRs

- [ADR-0006](0006-flyway-versioned-migrations.md) — the Flyway pre-deploy migration step that Forge Shipping will gate.
- [ADR-0004](0004-secrets-compose-secrets-and-rotation-runbook.md) — secrets bootstrapping that the Forge Shipping compose smoke test exercises.
