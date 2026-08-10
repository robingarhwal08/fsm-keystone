package com.fsm.keystone.exception;

import java.util.Map;

/**
 * Thrown when an authentication attempt fails (HTTP 401 Unauthorized).
 *
 * <p>Use generic messages only — never embed credentials, tokens or raw request
 * bodies in the message or details map.</p>
 */
public class AuthenticationFailedException extends ApiException {

    public AuthenticationFailedException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, message, Map.of());
    }

    public AuthenticationFailedException(ErrorCode code, String message) {
        super(code, message, Map.of());
    }

    public AuthenticationFailedException(ErrorCode code, String message, Throwable cause) {
        super(code, message, Map.of(), cause);
    }

    public AuthenticationFailedException(ErrorCode code, String message,
                                         Map<String, Object> details) {
        super(code, message, details);
    }
}
