package com.fsm.keystone.exception;

import com.fsm.keystone.dto.SignupRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

/**
 * Test-only controller used exclusively by {@link GlobalExceptionHandlerWebMvcTest}.
 *
 * <p>Each endpoint deliberately throws one specific exception type so the WebMvc slice
 * can assert that every {@code GlobalExceptionHandler} branch produces the correct
 * HTTP status, JSON shape and {@code X-Correlation-Id} response header.</p>
 *
 * <p>This class must NOT appear in any production component scan — it is located
 * in the test source tree and is loaded only by the explicit {@code @WebMvcTest}
 * controller reference.</p>
 */
@RestController
@RequestMapping("/test/errors")
public class ErrorTestController {

    /** Triggers {@code ResourceNotFoundException} → 404. */
    @GetMapping("/not-found")
    public ResponseEntity<Void> throwNotFound() {
        throw ResourceNotFoundException.of("Work order", 42L);
    }

    /** Triggers {@code BusinessRuleException} → 409. */
    @GetMapping("/conflict")
    public ResponseEntity<Void> throwConflict() {
        throw new BusinessRuleException(ErrorCode.INSUFFICIENT_STOCK,
                "Insufficient stock for part with id: 3");
    }

    /** Triggers {@code TenancyViolationException} → 403. */
    @GetMapping("/forbidden")
    public ResponseEntity<Void> throwForbidden() {
        throw new TenancyViolationException("Access denied: resource belongs to a different tenant");
    }

    /** Triggers {@code AuthenticationFailedException} → 401. */
    @GetMapping("/unauthorized")
    public ResponseEntity<Void> throwUnauthorized() {
        throw new AuthenticationFailedException("Invalid email or password");
    }

    /** Triggers {@code RateLimitExceededException} → 429. */
    @GetMapping("/rate-limited")
    public ResponseEntity<Void> throwRateLimit() {
        throw new RateLimitExceededException("Rate limit exceeded — retry after 60 seconds");
    }

    /**
     * Accepts a {@code @Valid}-annotated body so that submitting an invalid payload
     * triggers {@code MethodArgumentNotValidException} → 400 with fieldErrors.
     */
    @PostMapping("/validate")
    public ResponseEntity<Void> validateBody(@Valid @RequestBody SignupRequest req) {
        return ResponseEntity.ok().build();
    }

    /** Triggers {@code DataIntegrityViolationException} → 409. */
    @PostMapping("/db-conflict")
    public ResponseEntity<Void> throwDbConflict() {
        throw new DataIntegrityViolationException("unique constraint violation on users.email");
    }

    /**
     * Triggers a Spring Security {@code AccessDeniedException} at the controller layer.
     * This exercises the {@code GlobalExceptionHandler} path for controller-level access denial.
     */
    @GetMapping("/access-denied")
    public ResponseEntity<Void> throwAccessDenied() {
        throw new AccessDeniedException("Simulated method-level access denial");
    }

    /** Triggers a Spring Security {@code AuthenticationException} at the controller layer. */
    @GetMapping("/auth-exception")
    public ResponseEntity<Void> throwAuthException() {
        throw new BadCredentialsException("Simulated bad credentials");
    }

    /** Triggers the catch-all {@code Exception} → 500. */
    @GetMapping("/unexpected")
    public ResponseEntity<Void> throwUnexpected() {
        throw new RuntimeException("com.fsm.keystone.SomeInternalClass: database connection failed at line 42");
    }
}
