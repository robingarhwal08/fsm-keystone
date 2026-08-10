# ADR-0008: Frontend dependency pinning and Node 24 LTS build image

| Field  | Value      |
|--------|------------|
| Status | Accepted   |
| Date   | 2026-08-10 |

## Context

`frontend/package.json` currently specifies `"latest"` version ranges for all major dependencies: `@vitejs/plugin-react`, `axios`, `lucide-react`, `react`, `react-dom`, and `vite`. Running `npm install` at any point could silently install a breaking major version. One package uses a caret range (`"react-icons": "^5.7.0"`) which at least pins the major version.

`frontend/Dockerfile` uses `FROM node:20-alpine AS build`. Node 20 enters Maintenance LTS in April 2026 and End-of-Life in April 2027. Node 24 is the current Active LTS (released April 2025, Active LTS from October 2025). Using an outdated build image produces different behaviour between CI and developer laptops running a newer Node version.

## Decision

We will pin all frontend dependencies to explicit semantic versions (`"18.3.1"` not `"latest"` or `"^18"`) and replace `FROM node:20-alpine` with `FROM node:24-alpine` in `frontend/Dockerfile`. The `docs/baseline/proposed-pins.md` document records the pinned versions and the rationale for each. Node 22 (Maintenance LTS) is the fallback if a specific dependency requires it; Node 24 is the target.

## Consequences

- **Positive**: reproducible builds — `npm ci` with a locked `package-lock.json` and an explicit Node image produces identical output on every build.
- **Positive**: Node 24 Active LTS receives security patches; Node 20 Maintenance LTS does not receive new features and will be EOL.
- **Positive**: explicit version pins surface dependency update decisions as deliberate pull requests rather than silent `npm install` side-effects.
- **Negative**: pinned versions require active maintenance: each dependency upgrade is a conscious decision and a pull request.
- **Negative**: moving to Node 24 may require updates to native addon dependencies (rare in a pure-React app, but possible for Vite and its Rollup native binary).
- **Ongoing discipline**: Dependabot or Renovate should be configured to open automated upgrade PRs so pins do not drift into obsolescence.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| Keep `"latest"` ranges | Non-reproducible builds; any `npm install` can silently introduce a breaking change. Incompatible with a reproducible-build policy. |
| Caret ranges (`^18.x`) | Pins the major version but allows minor/patch updates that can still include breaking changes in pre-1.0 packages. Explicit pins are unambiguous. |
| Node 22 (Maintenance LTS) | Acceptable fallback. Node 24 Active LTS is preferred because it receives both security and bug-fix updates for longer. |
| Node Alpine vs Debian slim | Alpine is smaller and already in use. Debian slim would be needed only if a native addon required glibc. No such addon exists in this project. |

## Evidence

```
frontend/package.json
  — "react": "latest", "react-dom": "latest", "vite": "latest", "@vitejs/plugin-react": "latest",
    "axios": "latest", "lucide-react": "latest" — all unpinned "latest" ranges.
  — "react-icons": "^5.7.0" — only dependency with a caret-pinned version.

frontend/Dockerfile:2
  — FROM node:20-alpine AS build — current build image; Node 20 is Maintenance LTS.

docs/baseline/proposed-pins.md — records the specific pinned versions for review.
```

## Related ADRs

- [ADR-0005](0005-forge-shipping-delivery-platform.md) — Forge Shipping pre-merge gate that runs the frontend build and fails if pinned dependencies produce errors.
