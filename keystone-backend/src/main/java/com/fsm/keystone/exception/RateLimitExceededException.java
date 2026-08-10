package com.fsm.keystone.exception;

import java.util.Map;

/**
 * Thrown when a caller exceeds an API rate limit (HTTP 429 Too Many Requests).
 */
public class RateLimitExceededException extends ApiException {

    public RateLimitExceededException(String message) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, message, Map.of());
    }

    public RateLimitExceededException(String message, Map<String, Object> details) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, message, details);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, message, Map.of(), cause);
    }
}
