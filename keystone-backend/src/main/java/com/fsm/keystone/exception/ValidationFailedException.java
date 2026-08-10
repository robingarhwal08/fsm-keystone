package com.fsm.keystone.exception;

import java.util.Map;

/**
 * Thrown when request data fails domain-level validation (HTTP 400 Bad Request).
 *
 * <p>Examples: end time before start time in a time log, negative stock quantity.</p>
 */
public class ValidationFailedException extends ApiException {

    public ValidationFailedException(ErrorCode code, String message) {
        super(code, message, Map.of());
    }

    public ValidationFailedException(ErrorCode code, String message, Throwable cause) {
        super(code, message, Map.of(), cause);
    }

    public ValidationFailedException(ErrorCode code, String message,
                                     Map<String, Object> details) {
        super(code, message, details);
    }

    public ValidationFailedException(ErrorCode code, String message,
                                     Map<String, Object> details, Throwable cause) {
        super(code, message, details, cause);
    }
}
