package com.fsm.keystone.exception;

import java.util.Map;

/**
 * Reusable sample instances of every {@link ApiException} subtype.
 *
 * <p>Consumed by the WO-021 {@code @RestControllerAdvice} handler tests and the
 * WO-025 integration error-contract suite. Each factory method returns a fresh
 * instance with representative field values.</p>
 */
public final class ExceptionFixtures {

    private ExceptionFixtures() {}

    public static ResourceNotFoundException resourceNotFound() {
        return ResourceNotFoundException.of("Work order", 42L);
    }

    public static ResourceNotFoundException customerNotFound() {
        return ResourceNotFoundException.of("Customer", 7L);
    }

    public static ResourceNotFoundException userNotFound() {
        return ResourceNotFoundException.of("User", 99L);
    }

    public static ResourceNotFoundException partNotFound() {
        return ResourceNotFoundException.of("Part", 3L);
    }

    public static ResourceNotFoundException siteNotFound() {
        return ResourceNotFoundException.of("Site", 5L);
    }

    public static BusinessRuleException insufficientStock() {
        return new BusinessRuleException(
                ErrorCode.INSUFFICIENT_STOCK,
                "Insufficient stock for part with id: 3",
                Map.of("partId", 3L, "requested", 10, "available", 2));
    }

    public static BusinessRuleException invalidStatusTransition() {
        return new BusinessRuleException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "Cannot transition from COMPLETED to OPEN",
                Map.of("from", "COMPLETED", "to", "OPEN"));
    }

    public static BusinessRuleException emailAlreadyExists() {
        return new BusinessRuleException(
                ErrorCode.EMAIL_ALREADY_EXISTS,
                "Email already exists");
    }

    public static ValidationFailedException invalidTimeRange() {
        return new ValidationFailedException(
                ErrorCode.INVALID_TIME_RANGE,
                "End time must be after start time",
                Map.of("field", "endTime"));
    }

    public static TenancyViolationException crossTenantAccess() {
        return new TenancyViolationException("Access denied: resource belongs to a different tenant");
    }

    public static AuthenticationFailedException authenticationFailed() {
        return new AuthenticationFailedException("Invalid email or password");
    }

    public static AuthenticationFailedException tokenExpired() {
        return new AuthenticationFailedException(
                ErrorCode.TOKEN_EXPIRED, "Authentication token has expired");
    }

    public static RateLimitExceededException rateLimitExceeded() {
        return new RateLimitExceededException(
                "Rate limit exceeded — retry after 60 seconds",
                Map.of("retryAfterSeconds", 60));
    }
}
