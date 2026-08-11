package com.fsm.keystone.dto;

import java.time.Instant;
import java.util.List;

/**
 * Canonical error response body returned by every error branch in
 * {@code GlobalExceptionHandler} and by the security entry-point/denied-handler beans.
 *
 * <p>All fields are always present. {@code fieldErrors} is an empty list when the error
 * is not a validation failure so clients can iterate it unconditionally.</p>
 *
 * <p>Field contract (API Design Conventions policy):
 * <ul>
 *   <li>{@code timestamp} — ISO-8601 UTC instant, e.g. {@code 2026-01-05T10:15:30.000Z}</li>
 *   <li>{@code status} — HTTP status code integer mirror</li>
 *   <li>{@code code} — stable UPPER_SNAKE_CASE machine-readable code</li>
 *   <li>{@code message} — human-readable, safe (no stack traces, no SQL)</li>
 *   <li>{@code correlationId} — echoed from MDC key {@code correlationId} or generated UUID</li>
 *   <li>{@code path} — request URI, e.g. {@code /api/work-orders/42}</li>
 *   <li>{@code fieldErrors} — populated only for 400 validation failures</li>
 * </ul>
 * </p>
 */
public record ApiErrorResponse(
        String timestamp,
        int status,
        String code,
        String message,
        String correlationId,
        String path,
        List<FieldErrorDetail> fieldErrors
) {

    /**
     * Static factory that stamps the current UTC instant as the {@code timestamp}.
     */
    public static ApiErrorResponse of(int status, String code, String message,
                                      String correlationId, String path,
                                      List<FieldErrorDetail> fieldErrors) {
        return new ApiErrorResponse(
                Instant.now().toString(),
                status,
                code,
                message,
                correlationId,
                path,
                fieldErrors != null ? fieldErrors : List.of()
        );
    }
}
