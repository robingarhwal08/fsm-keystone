package com.fsm.keystone.exception;

import java.util.Map;

/**
 * Thrown when an operation violates a domain business rule (HTTP 409 Conflict).
 *
 * <p>Examples: insufficient part stock, invalid work-order status transition,
 * duplicate email on sign-up.</p>
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(ErrorCode code, String message) {
        super(code, message, Map.of());
    }

    public BusinessRuleException(ErrorCode code, String message, Throwable cause) {
        super(code, message, Map.of(), cause);
    }

    public BusinessRuleException(ErrorCode code, String message,
                                 Map<String, Object> details) {
        super(code, message, details);
    }

    public BusinessRuleException(ErrorCode code, String message,
                                 Map<String, Object> details, Throwable cause) {
        super(code, message, details, cause);
    }
}
