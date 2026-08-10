package com.fsm.keystone.exception;

import java.util.Map;

/**
 * Thrown when an operation attempts to access data belonging to a different tenant
 * (HTTP 403 Forbidden).
 *
 * <p>Used in the tenancy-enforcement epic (WO-031+) to distinguish deliberate
 * cross-tenant probing from ordinary permission denials.</p>
 */
public class TenancyViolationException extends ApiException {

    public TenancyViolationException(String message) {
        super(ErrorCode.CROSS_TENANT_ACCESS_DENIED, message, Map.of());
    }

    public TenancyViolationException(String message, Throwable cause) {
        super(ErrorCode.CROSS_TENANT_ACCESS_DENIED, message, Map.of(), cause);
    }

    public TenancyViolationException(String message, Map<String, Object> details) {
        super(ErrorCode.CROSS_TENANT_ACCESS_DENIED, message, details);
    }
}
