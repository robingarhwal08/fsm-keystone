package com.fsm.keystone.exception;

import com.fsm.keystone.dto.ApiErrorResponse;
import com.fsm.keystone.dto.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests that directly invoke each {@link GlobalExceptionHandler} method and assert
 * the returned {@link ResponseEntity} status, error code, message and correlationId.
 *
 * <p>Uses {@link ExceptionFixtures} from WO-012 for consistent exception instances.
 * No Spring context is loaded — the handler is instantiated as a plain Java object.</p>
 */
@Tag("unit")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test/path");
        when(request.getMethod()).thenReturn("GET");
        // Clear security context so isAnonymous checks work predictably.
        SecurityContextHolder.clearContext();
    }

    // ── ApiException branch ───────────────────────────────────────────────────

    @Test
    void handleApiException_resourceNotFound_returns404() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleApiException(
                ExceptionFixtures.resourceNotFound(), request);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(404, resp.getBody().status());
        assertEquals("WORK_ORDER_NOT_FOUND", resp.getBody().code());
        assertEquals("Work order not found with id: 42", resp.getBody().message());
        assertTrue(resp.getBody().fieldErrors().isEmpty());
        assertCorrelationIdHeader(resp);
    }

    @Test
    void handleApiException_businessRule_returns409() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleApiException(
                ExceptionFixtures.insufficientStock(), request);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("INSUFFICIENT_STOCK", resp.getBody().code());
        assertEquals(409, resp.getBody().status());
    }

    @Test
    void handleApiException_authenticationFailed_returns401() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleApiException(
                ExceptionFixtures.authenticationFailed(), request);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("AUTHENTICATION_FAILED", resp.getBody().code());
    }

    @Test
    void handleApiException_tokenExpired_returns401WithTokenExpiredCode() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleApiException(
                ExceptionFixtures.tokenExpired(), request);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("TOKEN_EXPIRED", resp.getBody().code());
    }

    @Test
    void handleApiException_tenancyViolation_returns403() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleApiException(
                ExceptionFixtures.crossTenantAccess(), request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("CROSS_TENANT_ACCESS_DENIED", resp.getBody().code());
    }

    @Test
    void handleApiException_rateLimitExceeded_returns429() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleApiException(
                ExceptionFixtures.rateLimitExceeded(), request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, resp.getStatusCode());
        assertEquals("RATE_LIMIT_EXCEEDED", resp.getBody().code());
    }

    // ── Response structure ────────────────────────────────────────────────────

    @Test
    void handleApiException_responseBodyHasAllRequiredFields() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleApiException(
                ExceptionFixtures.resourceNotFound(), request);

        ApiErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertNotNull(body.timestamp(), "timestamp must not be null");
        assertTrue(body.status() > 0, "status must be set");
        assertNotNull(body.code(), "code must not be null");
        assertNotNull(body.message(), "message must not be null");
        assertNotNull(body.correlationId(), "correlationId must not be null");
        assertNotNull(body.path(), "path must not be null");
        assertNotNull(body.fieldErrors(), "fieldErrors must not be null");
    }

    @Test
    void handleApiException_correlationIdMatchesHeader() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleApiException(
                ExceptionFixtures.resourceNotFound(), request);

        String headerValue = resp.getHeaders().getFirst("X-Correlation-Id");
        assertNotNull(headerValue, "X-Correlation-Id header must be present");
        assertEquals(headerValue, resp.getBody().correlationId(),
                "X-Correlation-Id header must match body correlationId");
    }

    @Test
    void handleApiException_pathMatchesRequestUri() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleApiException(
                ExceptionFixtures.resourceNotFound(), request);

        assertEquals("/api/test/path", resp.getBody().path());
    }

    // ── AccessDeniedException branch ──────────────────────────────────────────

    @Test
    void handleAccessDenied_anonymousContext_returns401() {
        // No authentication in context → anonymous
        SecurityContextHolder.clearContext();

        ResponseEntity<ApiErrorResponse> resp = handler.handleAccessDenied(
                new AccessDeniedException("test"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("AUTHENTICATION_FAILED", resp.getBody().code());
    }

    @Test
    void handleAccessDenied_anonymousToken_returns401() {
        AnonymousAuthenticationToken anon = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anon);

        ResponseEntity<ApiErrorResponse> resp = handler.handleAccessDenied(
                new AccessDeniedException("test"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    // ── Data Integrity Violation 409 ──────────────────────────────────────────

    @Test
    void handleDataIntegrityViolation_returns409WithGenericMessage() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("unique constraint violation on users.email"),
                request);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("DATA_INTEGRITY_VIOLATION", resp.getBody().code());
        // SQL detail must NOT appear in the response body.
        assertFalse(resp.getBody().message().contains("unique constraint"),
                "SQL constraint detail must not leak into response message");
        assertFalse(resp.getBody().message().contains("users.email"),
                "Table name must not leak into response message");
    }

    // ── Spring Security AuthenticationException 401 ───────────────────────────

    @Test
    void handleAuthenticationException_returns401() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleAuthenticationException(
                new BadCredentialsException("bad credentials"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("AUTHENTICATION_FAILED", resp.getBody().code());
        assertEquals("Authentication failed", resp.getBody().message());
    }

    // ── Catch-all 500 ─────────────────────────────────────────────────────────

    @Test
    void handleUnexpected_returns500WithGenericMessage() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleUnexpected(
                new RuntimeException("com.fsm.keystone.SomeClass: NPE at line 42"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("INTERNAL_ERROR", resp.getBody().code());
        assertEquals("An unexpected error occurred", resp.getBody().message());
    }

    @Test
    void handleUnexpected_bodyDoesNotLeakInternalDetails() {
        String internalMessage = "com.fsm.keystone.service.WorkOrderService: NullPointerException at java.lang.Object:42";
        ResponseEntity<ApiErrorResponse> resp = handler.handleUnexpected(
                new RuntimeException(internalMessage), request);

        String bodyJson = resp.getBody().message();
        assertFalse(bodyJson.contains("com.fsm"), "Class name must not appear in response");
        assertFalse(bodyJson.contains("Exception"), "Exception keyword must not appear in response");
        assertFalse(bodyJson.contains("at java"), "Stack trace must not appear in response");
    }

    @Test
    void handleUnexpected_correlationIdAlwaysPresent() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleUnexpected(
                new RuntimeException("boom"), request);

        assertNotNull(resp.getBody().correlationId());
        assertFalse(resp.getBody().correlationId().isBlank());
    }

    // ── Sensitive field masking ───────────────────────────────────────────────

    @Test
    void handleMethodArgumentNotValid_passwordField_isMasked() throws Exception {
        // Build a MethodArgumentNotValidException with a "password" field error.
        // We test the masking logic via the buildFieldErrors helper indirectly
        // through a reflective invocation to keep this as a pure unit test.
        // The WebMvcTest below covers the HTTP path for this scenario.
        // Here we verify the sensitive field detection directly.
        GlobalExceptionHandler h = new GlobalExceptionHandler();
        var method = GlobalExceptionHandler.class.getDeclaredMethod("isSensitiveField", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(h, "password"), "password is sensitive");
        assertTrue((Boolean) method.invoke(h, "oldPassword"), "oldPassword contains 'password' → sensitive");
        assertTrue((Boolean) method.invoke(h, "confirmPassword"), "confirmPassword is sensitive");
        assertTrue((Boolean) method.invoke(h, "token"), "token is sensitive");
        assertTrue((Boolean) method.invoke(h, "refreshToken"), "refreshToken is sensitive");
        assertTrue((Boolean) method.invoke(h, "apiSecret"), "apiSecret is sensitive");
        assertFalse((Boolean) method.invoke(h, "email"), "email is not sensitive");
        assertFalse((Boolean) method.invoke(h, "fullName"), "fullName is not sensitive");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void assertCorrelationIdHeader(ResponseEntity<?> resp) {
        String header = resp.getHeaders().getFirst("X-Correlation-Id");
        assertNotNull(header, "X-Correlation-Id header must be present");
        assertFalse(header.isBlank(), "X-Correlation-Id must not be blank");
    }
}
