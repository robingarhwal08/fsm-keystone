package com.fsm.keystone.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ApiExceptionTest {

    // ── ResourceNotFoundException ─────────────────────────────────────────

    @Test
    void resourceNotFoundException_hasCorrectStatusAndCode() {
        ResourceNotFoundException ex = ResourceNotFoundException.of("Work order", 42L);

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(ErrorCode.WORK_ORDER_NOT_FOUND, ex.getCode());
        assertEquals("Work order not found with id: 42", ex.getMessage());
    }

    @Test
    void resourceNotFoundException_customerFactory() {
        ResourceNotFoundException ex = ResourceNotFoundException.of("Customer", 7L);

        assertEquals(ErrorCode.CUSTOMER_NOT_FOUND, ex.getCode());
        assertEquals("Customer not found with id: 7", ex.getMessage());
    }

    @Test
    void resourceNotFoundException_siteFactory() {
        ResourceNotFoundException ex = ResourceNotFoundException.of("Site", 5L);

        assertEquals(ErrorCode.SITE_NOT_FOUND, ex.getCode());
    }

    @Test
    void resourceNotFoundException_userFactory() {
        ResourceNotFoundException ex = ResourceNotFoundException.of("User", 1L);

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getCode());
    }

    @Test
    void resourceNotFoundException_technicianMapsToUserNotFound() {
        ResourceNotFoundException ex = ResourceNotFoundException.of("Technician", 1L);

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getCode());
    }

    @Test
    void resourceNotFoundException_partFactory() {
        ResourceNotFoundException ex = ResourceNotFoundException.of("Part", 3L);

        assertEquals(ErrorCode.PART_NOT_FOUND, ex.getCode());
    }

    @Test
    void resourceNotFoundException_nullId_doesNotThrow() {
        ResourceNotFoundException ex = ResourceNotFoundException.of("Work order", null);

        assertEquals("Work order not found with id: null", ex.getMessage());
    }

    @Test
    void resourceNotFoundException_unknownResourceType_usesGenericCode() {
        ResourceNotFoundException ex = ResourceNotFoundException.of("Widget", 1L);

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getCode());
    }

    // ── BusinessRuleException ─────────────────────────────────────────────

    @Test
    void businessRuleException_insufficientStock_hasConflictStatus() {
        BusinessRuleException ex = new BusinessRuleException(
                ErrorCode.INSUFFICIENT_STOCK, "Not enough stock");

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getCode());
        assertEquals("Not enough stock", ex.getMessage());
    }

    @Test
    void businessRuleException_withDetails_storesDetails() {
        Map<String, Object> details = Map.of("partId", 3L, "available", 2);
        BusinessRuleException ex = new BusinessRuleException(
                ErrorCode.INSUFFICIENT_STOCK, "Not enough stock", details);

        assertEquals(3L, ex.getDetails().get("partId"));
        assertEquals(2, ex.getDetails().get("available"));
    }

    @Test
    void businessRuleException_emailAlreadyExists() {
        BusinessRuleException ex = new BusinessRuleException(
                ErrorCode.EMAIL_ALREADY_EXISTS, "Email already exists");

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getCode());
    }

    // ── ValidationFailedException ─────────────────────────────────────────

    @Test
    void validationFailedException_hasBadRequestStatus() {
        ValidationFailedException ex = new ValidationFailedException(
                ErrorCode.INVALID_TIME_RANGE, "End time before start time");

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(ErrorCode.INVALID_TIME_RANGE, ex.getCode());
    }

    // ── TenancyViolationException ─────────────────────────────────────────

    @Test
    void tenancyViolationException_hasForbiddenStatusAndCorrectCode() {
        TenancyViolationException ex = new TenancyViolationException("Access denied");

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(ErrorCode.CROSS_TENANT_ACCESS_DENIED, ex.getCode());
        assertEquals("Access denied", ex.getMessage());
    }

    // ── AuthenticationFailedException ─────────────────────────────────────

    @Test
    void authenticationFailedException_hasUnauthorizedStatus() {
        AuthenticationFailedException ex = new AuthenticationFailedException("Bad credentials");

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(ErrorCode.AUTHENTICATION_FAILED, ex.getCode());
    }

    @Test
    void authenticationFailedException_tokenExpiredCode() {
        AuthenticationFailedException ex = new AuthenticationFailedException(
                ErrorCode.TOKEN_EXPIRED, "Token expired");

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(ErrorCode.TOKEN_EXPIRED, ex.getCode());
    }

    // ── RateLimitExceededException ────────────────────────────────────────

    @Test
    void rateLimitExceededException_hasTooManyRequestsStatus() {
        RateLimitExceededException ex = new RateLimitExceededException("Too many requests");

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, ex.getCode());
    }

    // ── Details map immutability ──────────────────────────────────────────

    @Test
    void detailsMap_isUnmodifiable() {
        BusinessRuleException ex = new BusinessRuleException(
                ErrorCode.INSUFFICIENT_STOCK, "msg",
                Map.of("key", "value"));

        assertThrows(UnsupportedOperationException.class,
                () -> ex.getDetails().put("newKey", "newValue"));
    }

    @Test
    void detailsMap_nullInput_returnsEmptyMap() {
        BusinessRuleException ex = new BusinessRuleException(
                ErrorCode.INSUFFICIENT_STOCK, "msg", null);

        assertNotNull(ex.getDetails());
        assertTrue(ex.getDetails().isEmpty());
    }

    @Test
    void detailsMap_emptyInput_returnsEmptyMap() {
        BusinessRuleException ex = new BusinessRuleException(
                ErrorCode.INSUFFICIENT_STOCK, "msg", Map.of());

        assertNotNull(ex.getDetails());
        assertTrue(ex.getDetails().isEmpty());
    }

    // ── Denylisted keys ──────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"password", "token", "secret", "authorization",
                             "PASSWORD", "Token", "SECRET", "Authorization"})
    void deniedDetailKey_throwsIllegalArgument(String key) {
        Map<String, Object> badDetails = new HashMap<>();
        badDetails.put(key, "value");

        assertThrows(IllegalArgumentException.class,
                () -> new BusinessRuleException(ErrorCode.INSUFFICIENT_STOCK, "msg", badDetails));
    }

    @Test
    void allowedDetailKey_doesNotThrow() {
        assertDoesNotThrow(() -> new BusinessRuleException(
                ErrorCode.INSUFFICIENT_STOCK, "msg",
                Map.of("partId", 1L, "available", 0)));
    }

    // ── Cause preservation ────────────────────────────────────────────────

    @Test
    void exception_withCause_preservesCause() {
        RuntimeException cause = new RuntimeException("root cause");
        ResourceNotFoundException ex = new ResourceNotFoundException(
                ErrorCode.WORK_ORDER_NOT_FOUND, "not found", cause);

        assertSame(cause, ex.getCause());
    }

    // ── Message truncation ────────────────────────────────────────────────

    @Test
    void veryLongMessage_isTruncatedTo200Chars() {
        String longMessage = "x".repeat(500);
        ResourceNotFoundException ex = ResourceNotFoundException.of("Work order", longMessage);

        assertTrue(ex.getMessage().length() <= 200);
    }

    // ── RuntimeException subtype ─────────────────────────────────────────

    @Test
    void allExceptions_areRuntimeExceptionSubtypes() {
        assertTrue(new ResourceNotFoundException(ErrorCode.WORK_ORDER_NOT_FOUND, "msg")
                instanceof RuntimeException);
        assertTrue(new BusinessRuleException(ErrorCode.INSUFFICIENT_STOCK, "msg")
                instanceof RuntimeException);
        assertTrue(new ValidationFailedException(ErrorCode.INVALID_TIME_RANGE, "msg")
                instanceof RuntimeException);
        assertTrue(new TenancyViolationException("msg") instanceof RuntimeException);
        assertTrue(new AuthenticationFailedException("msg") instanceof RuntimeException);
        assertTrue(new RateLimitExceededException("msg") instanceof RuntimeException);
    }

    // ── ErrorCode enum sweep ──────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyErrorCode_declaresNonNullStatus(ErrorCode code) {
        assertNotNull(code.getDefaultStatus(),
                "ErrorCode." + code.name() + " must declare a non-null HTTP status");
        int statusValue = code.getDefaultStatus().value();
        assertTrue(statusValue >= 400 && statusValue <= 599,
                "ErrorCode." + code.name() + " status must be 4xx or 5xx, got " + statusValue);
    }

    // ── ExceptionFixtures smoke ───────────────────────────────────────────

    @Test
    void exceptionFixtures_allTypesInstantiateWithoutError() {
        assertDoesNotThrow(() -> {
            ExceptionFixtures.resourceNotFound();
            ExceptionFixtures.customerNotFound();
            ExceptionFixtures.userNotFound();
            ExceptionFixtures.partNotFound();
            ExceptionFixtures.siteNotFound();
            ExceptionFixtures.insufficientStock();
            ExceptionFixtures.invalidStatusTransition();
            ExceptionFixtures.emailAlreadyExists();
            ExceptionFixtures.invalidTimeRange();
            ExceptionFixtures.crossTenantAccess();
            ExceptionFixtures.authenticationFailed();
            ExceptionFixtures.tokenExpired();
            ExceptionFixtures.rateLimitExceeded();
        });
    }
}
