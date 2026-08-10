package com.fsm.keystone.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base for all fsm-keystone domain exceptions.
 *
 * <p>Carries a stable {@link ErrorCode} with its default {@link HttpStatus}, a human
 * message, and an immutable map of context details. The details map guards against
 * sensitive key names (password, token, secret, authorization) and is always
 * non-null — callers may iterate it without null checks.</p>
 *
 * <p>Message text is truncated to {@value #MAX_MESSAGE_LENGTH} characters to prevent
 * unbounded log lines. Exception remains a {@link RuntimeException} subclass so Spring's
 * default rollback-on-unchecked behaviour is preserved inside {@code @Transactional}.</p>
 */
public abstract class ApiException extends RuntimeException {

    private static final int MAX_MESSAGE_LENGTH = 200;

    private static final Set<String> DENIED_DETAIL_KEYS = Set.of(
            "password", "token", "secret", "authorization",
            "credential", "credentials", "apikey", "api_key"
    );

    private final ErrorCode code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    protected ApiException(ErrorCode code, String message, Map<String, Object> details) {
        super(truncate(message));
        this.code = code;
        this.status = code.getDefaultStatus();
        this.details = buildSafeImmutableMap(details);
    }

    protected ApiException(ErrorCode code, String message, Map<String, Object> details,
                           Throwable cause) {
        super(truncate(message), cause);
        this.code = code;
        this.status = code.getDefaultStatus();
        this.details = buildSafeImmutableMap(details);
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    // ── Guard helpers ────────────────────────────────────────────────────────

    private static Map<String, Object> buildSafeImmutableMap(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }
        for (String key : input.keySet()) {
            rejectIfDenied(key);
        }
        return Map.copyOf(new HashMap<>(input));
    }

    private static void rejectIfDenied(String key) {
        if (key != null && DENIED_DETAIL_KEYS.contains(key.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Detail key '" + key + "' is not permitted: sensitive keys are denylisted");
        }
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > MAX_MESSAGE_LENGTH
                ? message.substring(0, MAX_MESSAGE_LENGTH)
                : message;
    }
}
