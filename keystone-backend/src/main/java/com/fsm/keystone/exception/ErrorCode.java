package com.fsm.keystone.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable machine-readable error codes for the fsm-keystone API.
 *
 * <p>Each constant declares its default HTTP status so the {@code @RestControllerAdvice}
 * (WO-021) needs no switch statement — it delegates directly to
 * {@link #getDefaultStatus()}.</p>
 *
 * <p>Ratchet rule: add constants, never remove or rename existing ones, so
 * callers can rely on code strings across deployments.</p>
 */
public enum ErrorCode {

    // ── Not Found 404 ──────────────────────────────────────────────────────
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    WORK_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND),
    SITE_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    PART_NOT_FOUND(HttpStatus.NOT_FOUND),

    // ── Business Rule Violations 409 ───────────────────────────────────────
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),
    DUPLICATE_CUSTOMER(HttpStatus.CONFLICT),
    DUPLICATE_SITE(HttpStatus.CONFLICT),

    // ── Validation 400 ─────────────────────────────────────────────────────
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_DATA(HttpStatus.BAD_REQUEST),

    // ── Tenancy 403 ────────────────────────────────────────────────────────
    CROSS_TENANT_ACCESS_DENIED(HttpStatus.FORBIDDEN),

    // ── Authentication 401 ─────────────────────────────────────────────────
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),

    // ── Rate Limiting 429 ──────────────────────────────────────────────────
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS);

    private final HttpStatus defaultStatus;

    ErrorCode(HttpStatus defaultStatus) {
        this.defaultStatus = defaultStatus;
    }

    public HttpStatus getDefaultStatus() {
        return defaultStatus;
    }
}
