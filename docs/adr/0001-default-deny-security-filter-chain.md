# ADR-0001: Default-deny security filter chain

| Field  | Value      |
|--------|------------|
| Status | Accepted   |
| Date   | 2026-08-10 |

## Context

The application exposes 34 REST endpoints across nine controllers under `/api/**`. Without an explicit security policy, Spring Security's default behaviour permits unauthenticated access to everything unless each route is individually locked down — a configuration error that results in open access. The original codebase used `anyRequest().authenticated()` as a catch-all but also added five broad `permitAll()` groups covering paths that include privileged write operations (user management, customer CRUD, time-log submission, part-usage recording). This means the filter chain grants unauthenticated access to `/api/users/**`, `/api/customers/**`, `/api/time-logs/**`, and `/api/part-usage/**` in addition to the intended public auth endpoints.

## Decision

We will keep the default-deny posture (`anyRequest().authenticated()` as the final rule in the filter chain) and rely on Spring Security's `@EnableMethodSecurity` together with per-endpoint `@PreAuthorize` annotations to express fine-grained role requirements. The five `permitAll()` groups in the filter chain are a known gap tracked in the authorization remediation epic (ADR-0002); they are documented here rather than silently accepted.

Session management is stateless (`SessionCreationPolicy.STATELESS`); CSRF protection is disabled because all clients are SPA frontends that communicate exclusively over HTTPS bearer-token headers.

## Consequences

- **Positive**: any endpoint not listed in `authorizeHttpRequests` is automatically denied; there is no fail-open default.
- **Positive**: STATELESS session management eliminates session-fixation and CSRF risks for the API surface.
- **Negative**: the four non-auth `permitAll()` groups (`/api/users/**`, `/api/customers/**`, `/api/time-logs/**`, `/api/part-usage/**`) currently bypass the filter chain. This gap is being closed incrementally in the authorization epic.
- **Negative**: disabling CSRF assumes all clients always send the `Authorization` header; cookie-based integrations would be vulnerable.
- **Ongoing discipline**: every new route group added to `authorizeHttpRequests` must be reviewed; adding `permitAll()` without method-level `@PreAuthorize` is a finding for the ArchUnit gate.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| Permit-all default, lock down sensitive routes individually | Fail-open by default; any missed route is publicly accessible. Default-deny is the safer starting position. |
| Spring Security ACL (domain-object security) | Correct long-term for multi-tenant RBAC, but requires a separate ACL schema and significant setup effort. Deferred to a later architecture phase. |
| API Gateway-level auth (e.g. AWS API Gateway authorizer) | Adds infrastructure dependency; local development becomes harder. Application-level security as a defence-in-depth layer is still required regardless. |

## Evidence

```
keystone-backend/src/main/java/com/fsm/keystone/security/SecurityConfig.java:42-61
  — authorizeHttpRequests block: permitAll on /api/auth/**, /api/users/**, /api/customers/**,
    /api/time-logs/**, /api/part-usage/**; anyRequest().authenticated() as the final rule.

keystone-backend/src/main/java/com/fsm/keystone/security/SecurityConfig.java:23
  — @EnableMethodSecurity annotation enabling @PreAuthorize on handler methods.

keystone-backend/src/main/java/com/fsm/keystone/security/SecurityConfig.java:63-65
  — SessionCreationPolicy.STATELESS; no HTTP session is created or used.

keystone-backend/src/main/java/com/fsm/keystone/security/JwtAuthenticationFilter.java:24
  — doFilterInternal intercepts every request; extracts and validates the Bearer token
    before the downstream handler executes.
```

## Related ADRs

- [ADR-0002](0002-method-security-preauthorize-archunit-gate.md) — method-level `@PreAuthorize` and the ArchUnit gate that enforces it as the second layer of the defence-in-depth model.
