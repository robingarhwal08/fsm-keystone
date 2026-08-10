# ADR-0009: JWT access-token TTL reduction to 15 minutes with rotating refresh tokens

| Field  | Value      |
|--------|------------|
| Status | Accepted   |
| Date   | 2026-08-10 |

## Context

`application.properties` sets `app.jwt.expiration-ms=${APP_JWT_EXPIRATION_MS:86400000}`, which resolves to 86,400,000 ms = **24 hours**. A 24-hour access token means a stolen token remains valid for up to a day after theft — a significant blast radius for a mobile field-service workforce where devices may be shared, lost, or stolen.

The JWT is a plain HMAC-SHA signature: `JwtService.getSignInKey()` calls `Keys.hmacShaKeyFor(secret.getBytes())` using a single static key with no `kid` (key ID) claim. There is no key rotation, no token revocation list, and no refresh-token mechanism. The configurable `app.jwt.expiration-ms` property was designed to allow TTL adjustment without code changes, making a TTL reduction a non-breaking configuration change.

Industry guidance (NIST SP 800-63B, OWASP ASVS v4 §3.2) caps short-lived session tokens at one hour for high-assurance applications; 15 minutes is the standard for APIs that carry PII or authorise field-service operations.

## Decision

We will change the default access-token TTL from 86,400,000 ms (24 hours) to **900,000 ms (15 minutes)** by updating the `APP_JWT_EXPIRATION_MS` default in `application.properties`. This is a **deliberate deviation** from the current production default. The configurable-expiration mechanism (`app.jwt.expiration-ms` property and `JwtService.expirationMs` field) is preserved and unchanged.

Alongside the TTL reduction, we will introduce an 8-hour rotating refresh-token mechanism: the auth endpoint issues a short-lived access token and a longer-lived refresh token; the client exchanges the refresh token for a new access/refresh pair before expiry. The refresh token is stored server-side (revocable), capped at 8 hours, and rotated on each use.

## Consequences

- **Positive**: a stolen access token is valid for at most 15 minutes; the blast radius of a credential-theft incident is dramatically reduced.
- **Positive**: the configurable mechanism is preserved; teams with stricter requirements can lower the TTL further via the `APP_JWT_EXPIRATION_MS` environment variable without code changes.
- **Positive**: refresh-token rotation means every token use produces a new refresh token, invalidating the previous one — a stolen refresh token is detected on next legitimate use.
- **Negative**: clients must implement token refresh logic; any client that assumes the 24-hour TTL will see unexpected 401 errors after 15 minutes.
- **Negative**: the refresh-token store requires a new database table or Redis entry; this is new infrastructure.
- **Negative**: there is no current token revocation mechanism for access tokens; a stolen token remains valid until it expires (max 15 minutes). Full revocation requires a token blocklist (deferred).
- **Deviation documentation**: this ADR is the formal record that the 24-hour default was deliberately changed to 15 minutes. Any reversion to a longer TTL must be documented in a superseding ADR with a security rationale.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| Keep 24-hour TTL | Unacceptable risk: a stolen token for a field technician's account is valid for an entire shift plus all remaining hours in the day. Contradicts OWASP ASVS §3.2. |
| 1-hour TTL, no refresh tokens | Reduces blast radius but forces users to re-authenticate every hour, which is disruptive for technicians working in the field without reliable connectivity. 15 min access + 8 h refresh is the industry standard balance. |
| Opaque tokens with server-side session store | Eliminates the stateless architecture (ADR-0001 STATELESS constraint). Adds latency for token validation on every request. |
| Token revocation list (blocklist) | Correct long-term complement to short-lived tokens. Requires a Redis or database store per token, adding infrastructure. Deferred to the token management epic. |

## Evidence

```
keystone-backend/src/main/resources/application.properties
  — app.jwt.expiration-ms=${APP_JWT_EXPIRATION_MS:86400000}
    Current default: 86400000 ms = 24 hours.
    Target default: 900000 ms = 15 minutes.

keystone-backend/src/main/java/com/fsm/keystone/security/JwtService.java:21
  — @Value("${app.jwt.expiration-ms}") private long expirationMs;
    Configurable field; property change takes effect without code modification.

keystone-backend/src/main/java/com/fsm/keystone/security/JwtService.java:36
  — .expiration(new Date(System.currentTimeMillis() + expirationMs))
    TTL applied at token generation time.

keystone-backend/src/main/java/com/fsm/keystone/security/JwtService.java:49
  — getSignInKey(): Keys.hmacShaKeyFor(secret.getBytes())
    Single static HMAC key; no kid claim; no key rotation (known limitation).
```

## Related ADRs

- [ADR-0001](0001-default-deny-security-filter-chain.md) — STATELESS session management that makes the access token the sole session credential.
- [ADR-0004](0004-secrets-compose-secrets-and-rotation-runbook.md) — the HMAC signing key (`app.jwt.secret`) is managed as a Compose secret; key rotation procedure is in `docs/runbooks/secret-rotation.md`.
